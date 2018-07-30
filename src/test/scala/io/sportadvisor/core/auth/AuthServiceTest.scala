package io.sportadvisor.core.auth

import java.time.LocalDateTime

import io.sportadvisor.BaseTest
import io.sportadvisor.core.auth.AuthModels.{AuthToken, CreateRefreshToken, RefreshToken, RefreshTokenData}
import io.sportadvisor.core.user.UserModels.{UserData, UserID}
import io.sportadvisor.exception.ApiError
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.invocation.InvocationOnMock

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

/**
  * @author sss3 (Vladimir Alekseev)
  */
class AuthServiceTest extends BaseTest {

  "AuthService" when {
    "createToken" should {
      "return valid token" in new Context {
        val authToken: AuthToken = awaitForResult(authService.createToken(testUser, false))
        private val maybeID: Option[UserID] = authService.userId(authToken.token)
        maybeID.isDefined shouldBe true
        maybeID.get shouldBe testUserId
      }
    }

    "revokeToken" should {
      "return api error if token is expired" in new Context {
        val authToken: AuthToken = awaitForResult(authService.createToken(testUser, false))
        sleep(7.seconds)
        awaitForResult(authService.revokeToken(authToken.token)).isLeft shouldBe true
      }

      "return api error if token is invalid" in new Context {
        awaitForResult(authService.revokeToken("blabla")).isLeft shouldBe true
      }

      "return unit if all success" in new Context {
        when(tokenRepository.removeById(anyLong())).thenReturn(Future.successful(unitVal))
        val authToken: AuthToken = awaitForResult(authService.createToken(testUser, false))
        awaitForResult(authService.revokeToken(authToken.token)).isRight shouldBe true
      }
    }

    "userId" should {
      "return None if token is expired" in new Context {
        val authToken: AuthToken = awaitForResult(authService.createToken(testUser, false))
        sleep(7.seconds)
        authService.userId(authToken.token).isDefined shouldBe false
      }

      "return None if token is invalid" in new Context {
        authService.userId("blabla").isDefined shouldBe false
      }

      "return Some(userId) if token is correct" in new Context {
        val authToken: AuthToken = awaitForResult(authService.createToken(testUser, false))
        authService.userId(authToken.token).isDefined shouldBe true
      }
    }

    "refreshAccessToken" should {
      "return AuthToken if refresh was success" in new Context {
        val authToken: AuthToken = awaitForResult(authService.createToken(testUser, false))
        when(tokenRepository.find(authToken.refreshToken)).thenReturn(Future.successful(
          Some(RefreshTokenData(testTokenId, testUserId, authToken.token, false, LocalDateTime.now()))))
        val result: Either[ApiError, AuthToken] =
          awaitForResult(authService.refreshAccessToken(authToken.refreshToken))
        private val userId: Option[UserID] = authService.userId(result.right.get.token)
        userId.get shouldBe testUserId
        result.isRight shouldBe true
      }

      "return api error if token is invalid" in new Context {
        val authToken: AuthToken = awaitForResult(authService.createToken(testUser, false))
        when(tokenRepository.find(authToken.refreshToken)).thenReturn(Future.successful(None))
        val result: Either[ApiError, AuthToken] =
          awaitForResult(authService.refreshAccessToken(authToken.refreshToken))
        result.isLeft shouldBe true
      }
    }
  }

  trait Context {
    val len = 10

    val testTokenId: Long = Random.nextLong()
    val testUserId: Long = Random.nextLong()
    val testName: String = Random.nextString(len)
    val testEmail: String = Random.nextString(len)
    val newEmail: String = "valekseev@sportadvisor.io"
    val testPassword: String = Random.nextString(len)

    val testUser = UserData(testUserId, testEmail, testPassword, testName, None)

    val secret = "secret"
    val tokenRepository: AuthTokenRepository = mock[AuthTokenRepository]

    when(tokenRepository.save(any[RefreshToken])).thenAnswer((invocation: InvocationOnMock) => {
      val arguments = invocation.getArguments
      arguments(0).asInstanceOf[RefreshToken] match {
        case refreshToken: CreateRefreshToken =>
          Future.successful(
            RefreshTokenData(Random.nextLong(),
                             refreshToken.userId,
                             refreshToken.token,
                             refreshToken.remember,
                             refreshToken.lastTouch))
        case refreshTokenData: RefreshTokenData =>
          Future.successful(refreshTokenData)
      }
    })

    val authService: AuthService = new AuthService(tokenRepository, secret, 5.seconds)
  }
}
