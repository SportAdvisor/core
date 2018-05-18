package io.sportadvisor.core.user

import java.time.LocalDateTime

import com.roundeights.hasher.Implicits._
import io.sportadvisor.BaseTest
import io.sportadvisor.exception.{ApiError, DuplicateException, UserNotFound}
import io.sportadvisor.http.I18nStub
import io.sportadvisor.util.i18n.I18n
import io.sportadvisor.util.mail.{MailMessage, MailRenderService, MailSenderService, MailService}
import org.mockito.Matchers
import pdi.jwt.{Jwt, JwtAlgorithm}
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
        when(tokenRepository.save(any[RefreshToken]()))
          .thenReturn(Future.successful(RefreshToken(1L, "", remember = false, LocalDateTime.now())))
        val value: Either[ApiError, AuthToken] = awaitForResult(userService.signUp(testEmail, testPassword, testName))
        value.isRight shouldBe true
        Jwt.decodeRaw(value.right.get.token, testSecretKey, Seq(JwtAlgorithm.HS256)).isSuccess shouldBe true
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
        when(tokenRepository.save(any[RefreshToken]()))
          .thenReturn(Future.successful(RefreshToken(1L, "", remember = false, LocalDateTime.now())))
        val token: Option[AuthToken] = awaitForResult(userService.signIn(testEmail, testPassword, remember = false))
        token.isDefined shouldBe true
        Jwt.decodeRaw(token.get.token, testSecretKey, Seq(JwtAlgorithm.HS256)).isSuccess shouldBe true
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
            error.exception.isDefined shouldBe true
            error.exception.foreach {
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
            error.exception.isDefined shouldBe true
            error.exception.foreach {
              case UserNotFound() => ()
              case _ => throw new IllegalStateException()
            }
          case Right(_) => throw new IllegalStateException()
        }
      }

      "return Right(Unit) if all success" in new Context {
        when(userRepository.find(newEmail)).thenReturn(Future.successful(None))
        when(userRepository.get(testId)).thenReturn(Future.successful(Some(testUser)))
        when(render.renderI18n(Matchers.eq("mails/mail-change.ssp"), any[Map[String, Any]](), any[I18n]()))
          .thenReturn(Random.nextString(20))
        when(sender.send(any[MailMessage]())).thenReturn(Future.successful(Right()))
        when(mailChangesTokenRepository.save(any[ChangeMailToken]))
          .thenReturn(Future.successful(ChangeMailToken("", LocalDateTime.now())))
        val result: Either[ApiError, Unit] = awaitForResult(userService.changeEmail(testId, newEmail, "https://sportadvisor.io/t"))
        result.isRight shouldBe true
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
    val tokenRepository: TokenRepository = mock[TokenRepository]
    val mailChangesTokenRepository: MailChangesTokenRepository = mock[MailChangesTokenRepository]

    val userService = new UserService(userRepository, tokenRepository, testSecretKey, mailService,
      mailChangesTokenRepository) with I18nStub

    val testId: Long = Random.nextLong()
    val testName: String = Random.nextString(10)
    val testEmail: String = Random.nextString(10)
    val newEmail: String = "valekseev@sportadvisor.io"
    val testPassword: String = Random.nextString(10)

    val testUser = UserData(testId, testEmail, testPassword.sha256.hex, testName)

  }

}
