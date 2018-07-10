package io.sportadvisor.core.auth

import io.sportadvisor.BaseTest
import io.sportadvisor.core.auth.AuthModels.{AuthToken, CreateRefreshToken, RefreshToken, RefreshTokenData}
import io.sportadvisor.core.user.UserModels.{UserData, UserID}
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
  }

  trait Context {
    val len = 10

    val testUserId: Long = Random.nextLong()
    val testName: String = Random.nextString(len)
    val testEmail: String = Random.nextString(len)
    val newEmail: String = "valekseev@sportadvisor.io"
    val testPassword: String = Random.nextString(len)

    val testUser = UserData(testUserId, testEmail, testPassword, testName, None)

    val secret = "secret"
    val tokenRepository: TokenRepository = mock[TokenRepository]

    when(tokenRepository.save(any[RefreshToken])).thenAnswer((invocation: InvocationOnMock) => {
      val arguments = invocation.getArguments
      val token = arguments(0).asInstanceOf[CreateRefreshToken]
      Future.successful(RefreshTokenData(Random.nextLong(), token.userId, token.token, token.remember, token.lastTouch))
    })

    val authService: AuthService = new AuthService(tokenRepository, secret, 5.seconds)
  }
}
