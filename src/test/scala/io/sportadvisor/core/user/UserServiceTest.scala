package io.sportadvisor.core.user

import java.time.LocalDateTime

import com.roundeights.hasher.Implicits._
import io.sportadvisor.BaseTest
import io.sportadvisor.exception.DuplicateException
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
        val value: Either[UserAlreadyExists, AuthToken] = awaitForResult(userService.signUp(testEmail, testPassword, testName))
        value.isRight shouldBe true
        Jwt.decodeRaw(value.right.get.token, testSecretKey, Seq(JwtAlgorithm.HS256)).isSuccess shouldBe true
      }

      "return user already registered" in new Context {
        when(userRepository.save(CreateUser(testEmail, testPassword.sha256.hex, testName)))
          .thenReturn(Future.successful(Left(new DuplicateException)))
        val value: Either[UserAlreadyExists, AuthToken] = awaitForResult(userService.signUp(testEmail, testPassword, testName))
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
  }

  trait Context {
    val testSecretKey = "test-key"
    val userRepository: UserRepository = mock[UserRepository]
    val tokenRepository: TokenRepository = mock[TokenRepository]
    val userService = new UserService(userRepository, tokenRepository, testSecretKey)

    val testId: Long = Random.nextLong()
    val testName: String = Random.nextString(10)
    val testEmail: String = Random.nextString(10)
    val testPassword: String = Random.nextString(10)

    val testUser = UserData(testId, testEmail, testPassword.sha256.hex, testName)

  }

}
