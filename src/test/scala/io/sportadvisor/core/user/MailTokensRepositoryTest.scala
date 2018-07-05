package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.ChangeMailToken
import io.sportadvisor.core.user.token.{MailChangesTokenRepositorySQL, TokenRepository}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
class MailTokensRepositoryTest extends BaseTest {

  "MailTokenRepositorySQL" when {
    "save and get" should {
      "successful save" in new Context {
        val f: Future[Either[ApiError, ChangeMailToken]] =
          mailTokenRepository.save(ChangeMailToken(testUserId, testToken, LocalDateTime.now().plusDays(1)))
        awaitForResult(f)
        val token: Option[ChangeMailToken] = awaitForResult(mailTokenRepository.get(testToken))
        token.isDefined shouldBe true
      }
    }
  }

  trait Context {
    val testToken = "token"
    val testUserId = 1L

    val mailTokenRepository: TokenRepository[ChangeMailToken] = new MailChangesTokenRepositorySQL(
      InMemoryPostgresStorage.databaseConnector)
  }
}
