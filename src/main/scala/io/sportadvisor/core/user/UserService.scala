package io.sportadvisor.core.user

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import com.roundeights.hasher.Implicits._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.sportadvisor.core.user.UserService._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.slf4s.Logging
import io.sportadvisor.util.MonadTransformers._
import io.sportadvisor.util.db.DatabaseConnector
import io.sportadvisor.util.i18n.I18n
import io.sportadvisor.util.mail._
import io.sportadvisor.exception._
import io.sportadvisor.util._

import scala.util.Success

/**
  * @author sss3 (Vladimir Alekseev)
  */
abstract class UserService(
    userRepository: UserRepository,
    tokenRepository: TokenRepository,
    secretKey: String,
    mailService: MailService[I18n, Throwable, _],
    mailTokenRepository: MailChangesTokenRepository)(implicit executionContext: ExecutionContext)
    extends Logging
    with I18nService {

  private[this] val expPeriod = 2.hour
  private[this] val mailChangeExpPeriod = 1.day

  def secret: String = secretKey

  def signUp(email: String, password: String, name: String): Future[Either[ApiError, AuthToken]] =
    userRepository
      .save(CreateUser(email, password.sha256.hex, name))
      .flatMap {
        case Left(e)  => Future.successful(Left(e))
        case Right(u) => createAndSaveToken(u, Boolean.box(true)).map(t => Right(t))
      }

  def signIn(email: String, password: String, remember: Boolean): Future[Option[AuthToken]] =
    userRepository
      .find(email)
      .filterT(u => password.sha256.hex == u.password)
      .flatMapTOuter(u => createAndSaveToken(u, remember))

  def changeEmail(userID: UserID,
                  email: String,
                  redirectUrl: String): Future[Either[ApiError, Unit]] = {
    userRepository.find(email).flatMap {
      case Some(_) => Future.successful(Left(DuplicateException()))
      case None =>
        userRepository
          .get(userID)
          .flatMap {
            case Some(u) => sendRequestOfChangeEmail(u, email, redirectUrl)
            case None    => Future.successful(Left(UserNotFound(userID)))
          }
    }
  }

  def confirmEmail(token: String): Future[Boolean] = {
    mailTokenRepository
      .get(token)
      .flatMapTInner(t => encodeChangeEmailToken(t.token, secret))
      .flatMapT { t =>
        userRepository
          .find(t.from)
          .mapT(u => u.copy(email = t.to))
          .flatMapTOuter(u => userRepository.save(u))
          .flatMapTInner(e => e.toOption)
          .flatMapTOuter(u => sendChangeEmailConfirmation(u, t.from).map(_ => u))
          .flatMapTOuter(u => tokenRepository.removeByUser(u.id).transform(_ => Success(u)))
      }
      .map {
        case Some(_) => true
        case None    => false
      }
  }

  def getById(id: UserID): Future[Option[UserData]] = userRepository.get(id)

  def changeAccount(userID: UserID,
                    name: String,
                    language: Option[String]): Future[Option[UserData]] = {
    userRepository
      .get(userID)
      .mapT(u => u.copy(name = name, language = language))
      .flatMapT(u => userRepository.save(u).map(e => e.toOption))
  }

  def changePassword(userID: UserID,
                     oldPass: String,
                     newPass: String): Future[Either[ApiError, Unit]] = {
    userRepository
      .get(userID)
      .flatMapTOuter(u => updatePass(u, oldPass, newPass)) map {
      case None    => Left(UserNotFound(userID))
      case Some(e) => e
    }
  }

  private def updatePass(u: UserData,
                         oldPass: String,
                         newPass: String): Future[Either[ApiError, Unit]] = {
    if (u.password == oldPass.sha256.hex) {
      val updatedUser = u.copy(password = newPass.sha256.hex)
      userRepository
        .save(updatedUser)
        .flatMapRight(user => tokenRepository.removeByUser(user.id))
    } else {
      Future.successful(Left(PasswordMismatch()))
    }
  }

  def deleteTokenById(id: Long): Future[Unit] = {
    tokenRepository.removeById(id)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def sendRequestOfChangeEmail(user: UserData,
                                       email: String,
                                       redirectUrl: String): Future[Either[ApiError, Unit]] = {
    val time = LocalDateTime.now().plusMinutes(mailChangeExpPeriod.toMinutes)
    val token = generateChangeEmailToken(user.email, email, secret, time)
    val args = Map[String, Any]("redirect" -> buildUrl(redirectUrl, token),
                                "user" -> user,
                                "expAt" -> dateToString(time),
                                "email" -> email)
    val body = mailService.mailRender.renderI18n("mails/mail-change.ssp", args, mails(user.lang))
    val subject = mails(user.lang).t("Change email on SportAdvisor")
    val msg = MailMessage(List(email), List(), List(), subject, HtmlContent(body))
    mailService.mailSender.send(msg).flatMap {
      case Left(t)  => Future.successful(Left(UnhandledException(t)))
      case Right(_) => mailTokenRepository.save(ChangeMailToken(token, time)).map(_ => Right())
    }
  }

  private def buildUrl(redirectUrl: String, token: String): String = {
    if (redirectUrl.contains("?")) {
      s"$redirectUrl&token=$token"
    } else {
      s"$redirectUrl?token=$token"
    }
  }

  private def createAndSaveToken(user: UserData, remember: Boolean): Future[AuthToken] = {
    val refreshToken =
      JwtUtil.encode(RefreshTokenContent(user.id, new Date().getTime), secret, None)
    val time = LocalDateTime.now()
    saveRefreshToken(user, refreshToken, remember, time).map(_.id) map { refreshTokenId =>
      val expTime = time.plusMinutes(expPeriod.toMinutes)
      val token = JwtUtil.encode(AuthTokenContent(refreshTokenId, user.id), secret, Option(expTime))
      AuthToken(token, refreshToken, expTime.atZone(ZoneId.systemDefault()))
    }
  }

  private def saveRefreshToken(user: UserData,
                               refreshToken: Token,
                               remember: Boolean,
                               creation: LocalDateTime): Future[RefreshTokenData] = {
    tokenRepository.save(CreateRefreshToken(user.id, refreshToken, remember, creation))
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

}

object UserService {

  private[user] final case class ChangeMailTokenContent(from: String, to: String)
  private implicit val changeMailTokenContentEncoder: Encoder[ChangeMailTokenContent] =
    deriveEncoder
  private implicit val changeMailTokenContentDecoder: Decoder[ChangeMailTokenContent] =
    deriveDecoder

  def apply(config: Config,
            databaseConnector: DatabaseConnector,
            mailService: MailService[I18n, Throwable, Unit])(
      implicit executionContext: ExecutionContext): UserService = {
    val userRepository = new UserRepositorySQL(databaseConnector)
    val tokenRepository = new TokenRepositorySQL(databaseConnector)
    val mailTokenRepository = new MailChangesTokenRepositorySQL(databaseConnector)
    new UserService(userRepository,
                    tokenRepository,
                    config.secretKey,
                    mailService,
                    mailTokenRepository) with I18nServiceImpl
  }

  private[user] def generateChangeEmailToken(from: String,
                                             to: String,
                                             secret: String,
                                             exp: LocalDateTime): String =
    JwtUtil.encode(ChangeMailTokenContent(from, to), secret, Option(exp))

  private[user] def encodeChangeEmailToken(token: String,
                                           secret: String): Option[ChangeMailTokenContent] =
    JwtUtil.decode[ChangeMailTokenContent](token, secret)
}
