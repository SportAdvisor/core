package io.sportadvisor.core.user

import java.time.LocalDateTime

import cats.data.{EitherT, OptionT}
import com.roundeights.hasher.Implicits._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.sportadvisor.core.user.UserModels._
import io.sportadvisor.core.user.UserService._
import io.sportadvisor.exception.Exceptions._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.slf4s.Logging
import io.sportadvisor.util.i18n.I18n
import io.sportadvisor.util.mail._
import io.sportadvisor.util.future._
import io.sportadvisor.exception._
import io.sportadvisor.util._
import io.sportadvisor.util.mail.MailModel.{HtmlContent, MailMessage}
import cats.instances.string._
import cats.instances.future._
import cats.syntax.eq._
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.user.token._

/**
  * @author sss3 (Vladimir Alekseev)
  */
abstract class UserService(userRepository: UserRepository,
                           authService: AuthService,
                           secretKey: String,
                           mailService: MailService[I18n, Throwable, _],
                           mailTokenRepository: TokenRepository[ChangeMailToken],
                           resetPasswordTokenRepository: TokenRepository[ResetPasswordToken])(
    implicit executionContext: ExecutionContext)
    extends Logging
    with I18nService {

  private[this] val mailChangeExpPeriod = 1.day
  private[this] val passwordRessetExpPerion = 1.day

  def secret: String = secretKey

  def signUp(email: String, password: String, name: String): Future[Either[ApiError, AuthToken]] =
    EitherT(userRepository.save(CreateUser(email, password.sha256.hex, name)))
      .semiflatMap(authService.createToken(_, remember = true))
      .value

  def signIn(email: String, password: String, remember: Boolean): Future[Option[AuthToken]] =
    OptionT(userRepository.find(email))
      .filter(_.password === password.sha256.hex)
      .semiflatMap(authService.createToken(_, remember))
      .value

  def changeEmail(userID: UserID, email: String, redirectUrl: String): Future[Either[ApiError, Unit]] = {
    userRepository.find(email).flatMap {
      case Some(_) => Future.successful(Left(DuplicateException()))
      case None =>
        userRepository
          .get(userID)
          .flatMap {
            case Some(u) => sendRequestOfChangeEmail(u, email, redirectUrl)
            case None    => Future.successful(Left(ResourceNotFound(userID)))
          }
    }
  }

  def confirmEmail(token: String): Future[Boolean] =
    OptionT(mailTokenRepository.get(token))
      .subflatMap(t => decodeChangeEmailToken(t.token, secret))
      .flatMap(updateEmail)
      .value
      .map(_.isDefined)

  def resetPassword(email: String, redirectUrl: String): Future[Either[ApiError, Unit]] =
    userRepository
      .find(email)
      .flatMap {
        case None    => Future.successful(Right(()))
        case Some(u) => sendResetPasswordToken(u, redirectUrl)
      }

  def setNewPassword(token: String, password: String): Future[Either[ApiError, Unit]] = {
    val eitherT = for {
      tokenFromDb <- EitherT
        .fromOptionF(resetPasswordTokenRepository.get(token),
                     TokenDoesntExist("reset password"): ApiError)
      decodedToken <- EitherT.fromOption[Future](
        decodeResetPasswordToken(tokenFromDb.token, secret),
        TokenExpired("reset password"): ApiError)
      user <- EitherT.fromOptionF(userRepository.find(decodedToken.email),
                                  ResourceNotFound(decodedToken.email): ApiError)
      result <- updatePassword(user, password)
    } yield result
    eitherT
      .semiflatMap(u => resetPasswordTokenRepository.removeByUser(u.id).toSuccess(()))
      .value
  }

  def getById(id: UserID): Future[Option[UserData]] = userRepository.get(id)

  def changeAccount(userID: UserID,
                    name: String,
                    language: Option[String]): Future[Option[UserData]] =
    OptionT(userRepository.get(userID))
      .map(_.copy(name = name, language = language))
      .flatMapF(u => userRepository.save(u).map(_.toOption))
      .value

  def changePassword(userID: UserID,
                     oldPass: String,
                     newPass: String): Future[Either[ApiError, Unit]] = {
    EitherT
      .fromOptionF(userRepository
                     .get(userID),
                   ResourceNotFound(userID))
      .flatMap(u => updatePass(u, oldPass, newPass))
      .map(_ => ())
      .value
  }

  def logout(token: String): Future[Either[ApiError, Unit]] = authService.revokeToken(token)

  private def updatePass(u: UserData,
                         oldPass: String,
                         newPass: String): EitherT[Future, ApiError, UserData] = {
    if (u.password === oldPass.sha256.hex) {
      updatePassword(u, newPass)
    } else {
      EitherT.leftT[Future, UserData](PasswordMismatch())
    }
  }

  private def updatePassword(u: UserData, newPass: String): EitherT[Future, ApiError, UserData] = {
    val updatedUser = u.copy(password = newPass.sha256.hex)
    EitherT(
      userRepository
        .save(updatedUser))
      .semiflatMap(user => authService.revokeAllTokens(user.id).toSuccess(user))
  }

  private def updateEmail(t: ChangeMailTokenContent) = {
    OptionT(userRepository.find(t.from))
      .map(_.copy(email = t.to))
      .flatMap(u => EitherT(userRepository.save(u)).toOption)
      .semiflatMap(u => sendChangeEmailConfirmation(u, t.from).map(_ => u))
      .semiflatMap(u => mailTokenRepository.removeByUser(u.id).toSuccess(u))
      .semiflatMap(u => authService.revokeAllTokens(u.id).toSuccess(u))
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def sendResetPasswordToken(user: UserData,
                                     redirectUrl: String): Future[Either[ApiError, Unit]] = {
    val time = LocalDateTime.now().plusMinutes(passwordRessetExpPerion.toMinutes)
    val token = generateResetPasswordToken(user.email, secret, time)
    EitherT(resetPasswordTokenRepository.save(ResetPasswordToken(user.id, token, time))).flatMapF {
      _ =>
        val args = Map[String, Any]("redirect" -> buildUrl(redirectUrl, token),
                                    "user" -> user,
                                    "expAt" -> dateToString(time))
        val body =
          mailService.mailRender.renderI18n("mails/reset-password.ssp", args, mails(user.lang))
        val subject = mails(user.lang).t("Reset password on SportAdvisor")
        val msg = MailMessage(List(user.email), List(), List(), subject, HtmlContent(body))
        sendMessage(msg, Future.successful(Right(())))
    }.value
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def sendRequestOfChangeEmail(user: UserData,
                                       email: String,
                                       redirectUrl: String): Future[Either[ApiError, Unit]] = {
    val time = LocalDateTime.now().plusMinutes(mailChangeExpPeriod.toMinutes)
    val token = generateChangeEmailToken(user.email, email, secret, time)

    EitherT(mailTokenRepository.save(ChangeMailToken(user.id, token, time))).flatMapF { _ =>
      val args = Map[String, Any]("redirect" -> buildUrl(redirectUrl, token),
                                  "user" -> user,
                                  "expAt" -> dateToString(time),
                                  "email" -> email)
      val body =
        mailService.mailRender.renderI18n("mails/mail-change.ssp", args, mails(user.lang))
      val subject = mails(user.lang).t("Change email on SportAdvisor")
      val msg = MailMessage(List(email), List(), List(), subject, HtmlContent(body))
      sendMessage(msg, Future.successful(Right(())))
    }.value
  }

  private def buildUrl(redirectUrl: String, token: String): String = {
    if (redirectUrl.contains("?")) {
      s"$redirectUrl&token=$token"
    } else {
      s"$redirectUrl?token=$token"
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def sendChangeEmailConfirmation(user: UserData,
                                          oldEmail: String): Future[Either[ApiError, Unit]] = {
    val args = Map[String, Any]("user" -> user, "oldEmail" -> oldEmail)
    val body =
      mailService.mailRender.renderI18n("mails/mail-change-confirm.ssp", args, mails(user.lang))
    val subject = mails(user.lang).t("Change email on SportAdvisor")
    val msg = MailMessage(List(oldEmail), List(), List(user.email), subject, HtmlContent(body))
    mailService.mailSender.send(msg).map {
      case Left(t)  => Left(UnhandledException(t))
      case Right(_) => Right(())
    }
  }

  private def sendMessage[T](msg: MailMessage,
                             successHandle: => Future[Either[ApiError, T]]): Future[Either[ApiError, T]] = {
    mailService.mailSender.send(msg).flatMap {
      case Left(t)  => Future.successful(Left(UnhandledException(t)))
      case Right(_) => successHandle
    }
  }

}

object UserService {

  private[user] final case class ChangeMailTokenContent(from: String, to: String)
  private[user] final case class ResetPasswordTokenContent(email: String)
  private implicit val changeMailTokenContentEncoder: Encoder[ChangeMailTokenContent] =
    deriveEncoder
  private implicit val changeMailTokenContentDecoder: Decoder[ChangeMailTokenContent] =
    deriveDecoder
  private implicit val resetPasswordTokenContentEncoder: Encoder[ResetPasswordTokenContent] =
    deriveEncoder
  private implicit val resetPasswordTokenContentDencoder: Decoder[ResetPasswordTokenContent] =
    deriveDecoder

  private[user] def generateChangeEmailToken(from: String,
                                             to: String,
                                             secret: String,
                                             exp: LocalDateTime): String =
    JwtUtil.encode(ChangeMailTokenContent(from, to), secret, Option(exp))

  private[user] def decodeChangeEmailToken(token: String, secret: String): Option[ChangeMailTokenContent] =
    JwtUtil.decode[ChangeMailTokenContent](token, secret)

  private[user] def generateResetPasswordToken(email: String, secret: String, exp: LocalDateTime): String =
    JwtUtil.encode(ResetPasswordTokenContent(email), secret, Option(exp))

  private[user] def decodeResetPasswordToken(token: String,
                                             secret: String): Option[ResetPasswordTokenContent] =
    JwtUtil.decode[ResetPasswordTokenContent](token, secret)
}
