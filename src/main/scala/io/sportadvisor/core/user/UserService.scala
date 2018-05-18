package io.sportadvisor.core.user

import java.time.LocalDateTime
import java.util.Date

import com.roundeights.hasher.Implicits._
import io.circe.Encoder
import io.circe.generic.semiauto._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.slf4s.Logging

import io.sportadvisor.util.MonadTransformers._
import io.sportadvisor.util.db.DatabaseConnector
import io.sportadvisor.util.i18n.I18n
import io.sportadvisor.util.mail.{MailService, _}
import io.sportadvisor.exception._
import io.sportadvisor.util._
import io.sportadvisor.core.user._

/**
  * @author sss3 (Vladimir Alekseev)
  */
abstract class UserService(
    userRepository: UserRepository,
    tokenRepository: TokenRepository,
    secretKey: String,
    mailService: MailService[I18n, Throwable, _],
    mailTokenService: MailChangesTokenRepository)(implicit executionContext: ExecutionContext)
    extends Logging
    with I18nService {

  private[this] val expPeriod = 2.hour
  private[this] val mailChangeSecret = this.secret.toUpperCase.reverse
  private[this] val mailChangeExpPeriod = 1.day

  def secret: String = secretKey

  def signUp(email: String, password: String, name: String): Future[Either[ApiError, AuthToken]] =
    userRepository
      .save(CreateUser(email, password.sha256.hex, name))
      .flatMap {
        case Left(e)  => Future.successful(Left(ApiError(Option(e))))
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
      case Some(_) => Future.successful(Left(ApiError(Option(new DuplicateException))))
      case None    => sendChangeMailToken(userID, email, redirectUrl)
    }
  }

  private def sendChangeMailToken(userID: UserID,
                                  email: String,
                                  redirectUrl: String): Future[Either[ApiError, Unit]] = {
    userRepository
      .get(userID)
      .flatMap {
        case Some(u) => sendChangeMailTokenToUser(u, email, redirectUrl)
        case None    => Future.successful(Left(ApiError(Option(UserNotFound()))))
      }

  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def sendChangeMailTokenToUser(user: UserData,
                                  email: String,
                                  redirectUrl: String): Future[Either[ApiError, Unit]] = {
    val time = LocalDateTime.now().plusMinutes(mailChangeExpPeriod.toMinutes)
    val token =
      JwtUtil.encode(ChangeMailTokenContent(user.email, email), mailChangeSecret, Option(time))
    val args = Map[String, Any]("redirect" -> buildUrl(redirectUrl, token),
                   "user" -> user,
                   "expAt" -> dateToString(time),
                   "email" -> email)
    val body = mailService.mailRender.renderI18n("mails/mail-change.ssp", args, messages(user.lang))
    val subject = messages(user.lang).t("Change email on SportAdvisor")
    val msg = MailMessage(List(email), List(), List(), subject, HtmlContent(body))
    mailService.mailSender.send(msg).flatMap {
      case Left(t)  => Future.successful(Left(ApiError(Option(UnhandledException(t)))))
      case Right(_) => mailTokenService.save(ChangeMailToken(token, time)).map(_ => Right())
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
    val token = createToken(user, remember)
    saveToken(user, token.refreshToken, remember, LocalDateTime.now())
      .map(_ => token)
  }

  private def createToken(user: UserData, remember: Boolean): AuthToken = {
    val time = LocalDateTime.now()
    val expTime = time.plusMinutes(expPeriod.toMinutes)
    val token = JwtUtil.encode(AuthTokenContent(user.id), secret, Option(expTime))
    val refreshToken =
      JwtUtil.encode(RefreshTokenContent(user.id, new Date().getTime), secret, None)
    AuthToken(token, refreshToken, expTime)
  }

  private def saveToken(user: UserData,
                        refreshToken: Token,
                        remember: Boolean,
                        creation: LocalDateTime): Future[RefreshToken] = {
    tokenRepository.save(RefreshToken(user.id, refreshToken, remember, creation))
  }

  private final case class ChangeMailTokenContent(from: String, to: String)
  private implicit val changeMailTokenContentEncoder: Encoder[ChangeMailTokenContent] =
    deriveEncoder
}

object UserService {
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
}
