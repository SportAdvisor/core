package io.sportadvisor.core.user

import java.time.{LocalDateTime, ZonedDateTime}

import com.roundeights.hasher.Implicits._
import io.sportadvisor.BaseTest
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.user.UserModels._
import io.sportadvisor.exception.Exceptions.{DuplicateException, ResourceNotFound, UnhandledException}
import io.sportadvisor.exception._
import io.sportadvisor.http.I18nStub
import io.sportadvisor.util.i18n.I18n
import io.sportadvisor.util.mail.MailModel.MailMessage
import io.sportadvisor.util.mail.{MailRenderService, MailSenderService, MailService}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.concurrent.Future
import scala.util.Random

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserServiceTest extends BaseTest {

  "UserService" when {
    "signUp" should {
      "return valid auth token" in new Context {
        when(userRepository.save(CreateUser(testEmail, testPassword.sha256.hex, testName)))
          .thenReturn(Future.successful(Right(testUser)))
        when(authService.createToken(any[UserData](), anyBoolean()))
          .thenReturn(Future.successful(AuthToken("", "", ZonedDateTime.now())))
        val value: Either[ApiError, AuthToken] = awaitForResult(userService.signUp(testEmail, testPassword, testName))
        value.isRight shouldBe true
      }

      "return user already registered" in new Context {
        when(userRepository.save(CreateUser(testEmail, testPassword.sha256.hex, testName)))
          .thenReturn(Future.successful(Left(new DuplicateException)))
        val value: Either[ApiError, AuthToken] = awaitForResult(userService.signUp(testEmail, testPassword, testName))
        value.isLeft shouldBe true
      }
    }

    "signIn" should {
      "return valid auth token" in new Context {
        when(userRepository.find(testEmail)).thenReturn(Future.successful(Some(testUser)))
        when(authService.createToken(any[UserData](), anyBoolean()))
          .thenReturn(Future.successful(AuthToken("", "", ZonedDateTime.now())))
        val token: Option[AuthToken] = awaitForResult(userService.signIn(testEmail, testPassword, remember = false))
        token.isDefined shouldBe true
      }

      "return empty if user not found" in new Context {
        when(userRepository.find(testEmail)).thenReturn(Future.successful(None))
        val token: Option[AuthToken] = awaitForResult(userService.signIn(testEmail, testPassword, remember = false))
        token.isEmpty shouldBe true
      }

      "return empty if password invalid" in new Context {
        when(userRepository.find(testEmail)).thenReturn(Future.successful(Some(testUser)))
        val token: Option[AuthToken] = awaitForResult(userService.signIn(testEmail, testPassword + Random.nextString(1), remember = false))
        token.isEmpty shouldBe true
      }
    }

    "changeMail" should {
      "return dublication error if email already registered" in new Context {
        when(userRepository.find(testEmail)).thenReturn(Future.successful(Some(testUser)))
        val future: Future[Either[ApiError, Unit]] = userService.changeEmail(2L, testEmail, "https://sportadvisor.io/t")
        val either: Either[ApiError, Unit] = awaitForResult(future)
        either.isLeft shouldBe true
        either match {
          case Left(error) =>
            error match {
              case DuplicateException() => ()
              case _ => throw new IllegalStateException()
            }
          case Right(_) => throw new IllegalStateException()
        }
      }

      "return error if user with id not foud" in new Context {
        when(userRepository.find(testEmail)).thenReturn(Future.successful(None))
        when(userRepository.get(2L)).thenReturn(Future.successful(None))
        val future: Future[Either[ApiError, Unit]] = userService.changeEmail(2L, testEmail, "https://sportadvisor.io/t")
        val either: Either[ApiError, Unit] = awaitForResult(future)
        either.isLeft shouldBe true
        either match {
          case Left(error) =>
            error match {
              case ResourceNotFound(_) => ()
              case _               => throw new IllegalStateException()
            }
          case Right(_) => throw new IllegalStateException()
        }
      }

      "return Right(Unit) if all success" in new Context {
        when(userRepository.find(newEmail)).thenReturn(Future.successful(None))
        when(userRepository.get(testId)).thenReturn(Future.successful(Some(testUser)))
        when(render.renderI18n(Matchers.eq("mails/mail-change.ssp"), any[Map[String, Any]](), any[I18n]()))
          .thenReturn(Random.nextString(20))
        when(sender.send(any[MailMessage]())).thenReturn(Future.successful(Right(())))
        when(mailChangesTokenRepository.save(any[ChangeMailToken]))
          .thenReturn(Future.successful(ChangeMailToken("", LocalDateTime.now())))
        val result: Either[ApiError, Unit] = awaitForResult(userService.changeEmail(testId, newEmail, "https://sportadvisor.io/t"))
        result.isRight shouldBe true
      }
    }

    "approvalChangeEmail" should {
      "return false if token not found" in new Context {
        when(mailChangesTokenRepository.get("test")).thenReturn(Future.successful(None))
        awaitForResult(userService.confirmEmail("test")) shouldBe false
      }

      "return false if token is invalid" in new Context {
        when(mailChangesTokenRepository.get("test"))
          .thenReturn(Future.successful(Some(ChangeMailToken("test", LocalDateTime.now()))))
        awaitForResult(userService.confirmEmail("test")) shouldBe false
      }

      "return false if token expired" in new Context {
        val time: LocalDateTime = LocalDateTime.now().minusHours(1)
        val token: String = UserService.generateChangeEmailToken("test", "test", testSecretKey, time)
        when(mailChangesTokenRepository.get(token))
          .thenReturn(Future.successful(Some(ChangeMailToken(token, time))))
        awaitForResult(userService.confirmEmail(token)) shouldBe false
      }

      "return false is user not found" in new Context {
        val time: LocalDateTime = LocalDateTime.now().plusHours(1)
        val token: String = UserService.generateChangeEmailToken("test", "test", testSecretKey, time)
        when(mailChangesTokenRepository.get(token))
          .thenReturn(Future.successful(Some(ChangeMailToken(token, time))))
        when(userRepository.find("test")).thenReturn(Future.successful(None))
        awaitForResult(userService.confirmEmail(token)) shouldBe false
      }

      "return true if all success" in new Context {
        val time: LocalDateTime = LocalDateTime.now().plusHours(1)
        val token: String = UserService.generateChangeEmailToken("test", "test2", testSecretKey, time)
        when(mailChangesTokenRepository.get(token))
          .thenReturn(Future.successful(Some(ChangeMailToken(token, time))))
        when(userRepository.find("test")).thenReturn(Future.successful(Some(UserData(1L, "test", "", "", None))))
        when(render.renderI18n(Matchers.eq("mails/mail-change-confirm.ssp"), any[Map[String, Any]](), any[I18n]()))
          .thenReturn(Random.nextString(20))
        when(sender.send(any[MailMessage]())).thenReturn(Future.successful(Right(())))
        when(userRepository.save(any[UserData]())).thenReturn(Future.successful(Right(UserData(1L, "", "", "", None))))
        when(authService.revokeAllTokens(any[UserID]())).thenReturn(Future.successful(()))
        awaitForResult(userService.confirmEmail(token)) shouldBe true
      }

      "return true if remove tokens return error" in new Context {
        val time: LocalDateTime = LocalDateTime.now().plusHours(1)
        val token: String = UserService.generateChangeEmailToken("test", "test2", testSecretKey, time)
        when(mailChangesTokenRepository.get(token))
          .thenReturn(Future.successful(Some(ChangeMailToken(token, time))))
        when(userRepository.find("test"))
          .thenReturn(Future.successful(Some(UserData(1L, "test", "", "", None))))
        when(render.renderI18n(Matchers.eq("mails/mail-change-confirm.ssp"), any[Map[String, Any]](), any[I18n]()))
          .thenReturn(Random.nextString(20))
        when(sender.send(any[MailMessage]()))
          .thenReturn(Future.successful(Right(())))
        when(userRepository.save(any[UserData]()))
          .thenReturn(Future.successful(Right(UserData(1L, "", "", "", None))))
        when(authService.revokeAllTokens(any[UserID]()))
          .thenReturn(Future.failed[Unit](new IllegalStateException()))
        awaitForResult(userService.confirmEmail(token)) shouldBe true
      }
    }

    "changeAccount" should {
      "return None if user not found " in new Context {
        when(userRepository.get(testId)).thenReturn(Future.successful(None))

        awaitForResult(userService.changeAccount(testId, "test", Some("ru"))).isDefined shouldBe false
      }

      "return Some(new userdata) if all ok" in new Context {
        when(userRepository.get(testId)).thenReturn(Future.successful(Some(testUser)))
        when(userRepository.save(Matchers.eq[UserData](testUser.copy(name = "newName", language = Some("en")))))
            .thenReturn(Future.successful(Right(testUser.copy(name = "newName", language = Some("en")))))
        val userData: Option[UserData] = awaitForResult(userService.changeAccount(testId, "newName", Some("en")))
        userData.isDefined shouldBe true
        userData.get.name shouldBe "newName"
        userData.get.language.get shouldBe "en"
      }

      "return None if save end with error" in new Context {
        when(userRepository.get(testId)).thenReturn(Future.successful(Some(testUser)))
        when(userRepository.save(any[UserData]))
          .thenReturn(Future.successful(Left(UnhandledException(new Exception))))

        awaitForResult(userService.changeAccount(testId, "test", Some("ru"))).isDefined shouldBe false
      }
    }

    "changePassword" should {
      "return user not found" in new Context {
        when(userRepository.get(testId)).thenReturn(Future.successful(None))
        awaitForResult(userService.changePassword(testId, "123", "123")) match {
          case Right(_) => throw new IllegalStateException
          case Left(e) => e match {
            case ResourceNotFound(_) =>
            case _ => throw new IllegalStateException
          }
        }
      }

      "return password mismatch" in new Context {
        when(userRepository.get(testId)).thenReturn(Future.successful(Some(testUser)))
        awaitForResult(userService.changePassword(testId, "123", "123")) match {
          case Right(_) => throw new IllegalStateException
          case Left(e) => e match {
            case PasswordMismatch() =>
            case _ => throw new IllegalStateException
          }
        }
      }

      "return unit if success" in new Context {
        when(userRepository.get(testId)).thenReturn(Future.successful(Some(testUser)))
        when(userRepository.save(Matchers.eq[UserData](testUser.copy(password = "123".sha256.hex))))
          .thenReturn(Future.successful(Right(testUser.copy(password = "123".sha256.hex))))
        when(authService.revokeAllTokens(testId)).thenReturn(Future.successful(()))
        awaitForResult(userService.changePassword(testId, testPassword, "123")) match {
          case Right(_) =>
          case Left(_) => throw new IllegalStateException
        }
      }
    }

    "resetPassword" should {

      "return unit if succeed" in new Context {
        when(userRepository.find(testEmail))
          .thenReturn(Future.successful(Some(testUser)))
        when(sender.send(any[MailMessage]))
          .thenReturn(Future.successful(Right(())))
        when(resetPasswordTokenRepository.save(ResetPasswordToken(1L, "test", any[LocalDateTime])))
          .thenReturn(Future.successful(Right(ResetPasswordToken(1L, "test", LocalDateTime.now()))))
        awaitForResult(userService.resetPassword(testEmail, redirectUrl = "test")) shouldBe Right(())
      }

      "return unit if user wasn't found" in new Context {
        when(userRepository.find(testEmail))
          .thenReturn(Future.successful(None))
        when(sender.send(any[MailMessage]))
          .thenReturn(Future.successful(Right(())))
        when(resetPasswordTokenRepository.save(ResetPasswordToken(1L, "test", any[LocalDateTime])))
          .thenReturn(Future.successful(Right(ResetPasswordToken(1L, "test", LocalDateTime.now()))))
        awaitForResult(userService.resetPassword(testEmail, redirectUrl = "test")) shouldBe Right(())
      }

      "return unit if msg wasn't sent" in new Context {
        when(userRepository.find(testEmail))
          .thenReturn(Future.successful(Some(testUser)))
        private val exception = new Exception
        when(sender.send(any[MailMessage]))
          .thenReturn(Future.successful(Left(exception)))
        when(resetPasswordTokenRepository.save(ResetPasswordToken(1L, "test", any[LocalDateTime])))
          .thenReturn(Future.successful(Right(ResetPasswordToken(1L, "test", LocalDateTime.now()))))
        awaitForResult(userService.resetPassword(testEmail, redirectUrl = "test")) shouldBe Left(
          UnhandledException(exception))
      }
    }

  }

  trait Context {
    val sender: MailSenderService[Throwable, Unit] = mock[MailSenderService[Throwable, Unit]]
    val render: MailRenderService[I18n] = mock[MailRenderService[I18n]]

    val mailService: MailService[I18n, Throwable, Unit] = new MailService[I18n, Throwable, Unit] {

      override def mailRender: MailRenderService[I18n] = render

      override def mailSender: MailSenderService[Throwable, Unit] = sender

    }

    val testSecretKey = "test-key"
    val userRepository: UserRepository = mock[UserRepository]
    val mailChangesTokenRepository: MailChangesTokenRepository = mock[MailChangesTokenRepository]
    val resetPasswordTokenRepository: ResetPasswordTokenRepository =
      mock[ResetPasswordTokenRepository]
    val authService: AuthService = mock[AuthService]

    val userService = new UserService(userRepository,
                                      authService,
                                      testSecretKey,
                                      mailService,
                                      mailChangesTokenRepository,
                                      resetPasswordTokenRepository) with I18nStub

    val testId: Long = Random.nextLong()
    val testName: String = Random.nextString(10)
    val testEmail: String = Random.nextString(10)
    val newEmail: String = "valekseev@sportadvisor.io"
    val testPassword: String = Random.nextString(10)

    val testUser = UserData(testId, testEmail, testPassword.sha256.hex, testName, None)

  }

}
