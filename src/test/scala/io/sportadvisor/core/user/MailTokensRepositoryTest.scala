package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.{ChangeMailToken, UserID}
import io.sportadvisor.core.user.token.{TokenRepository, TokenType}
import io.sportadvisor.core.user.token.TokenRepository._
import io.sportadvisor.exception.ApiError
import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

import scala.concurrent.Future
import scala.util.Random

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

    "remove by user" should {
      "remove tokens by user" in new Context {
        private val id: UserID = testUserId + 1
        awaitForResult(mailTokenRepository.save(ChangeMailToken(id, "asd", LocalDateTime.now())))
        awaitForResult(mailTokenRepository.save(ChangeMailToken(id, "asd2", LocalDateTime.now())))

        awaitForResult(mailTokenRepository.get("asd")).isDefined shouldBe true
        awaitForResult(mailTokenRepository.get("asd2")).isDefined shouldBe true

        awaitForResult(mailTokenRepository.removeByUser(id)) shouldBe 2

        awaitForResult(mailTokenRepository.get("asd")).isDefined shouldBe false
        awaitForResult(mailTokenRepository.get("asd2")).isDefined shouldBe false
      }

      "not remove foreign tokens" in new Context {
        private val id = Random.nextLong()
        awaitForResult(mailTokenRepository.save(ChangeMailToken(id, "q1", LocalDateTime.now())))
        awaitForResult(mailTokenRepository.save(ChangeMailToken(id, "q2", LocalDateTime.now())))

        awaitForResult(mailTokenRepository.get("q1")).isDefined shouldBe true
        awaitForResult(mailTokenRepository.get("q2")).isDefined shouldBe true

        awaitForResult(mailTokenRepository.removeByUser(id - Random.nextInt())) shouldBe 0

        awaitForResult(mailTokenRepository.get("q1")).isDefined shouldBe true
        awaitForResult(mailTokenRepository.get("q2")).isDefined shouldBe true
      }
    }

    "remove expired tokens" should {
      "remove only expired tokens" in new Context {
        mailTokenRepository.removeByUser(testUserId)
        awaitForResult(
          mailTokenRepository.save(ChangeMailToken(testUserId, "q11", LocalDateTime.now().plusDays(1))))
        awaitForResult(
          mailTokenRepository.save(ChangeMailToken(testUserId, "q22", LocalDateTime.now().minusDays(1))))
        awaitForResult(mailTokenRepository.removeExpiredTokens())

        awaitForResult(mailTokenRepository.get("q11")).isDefined shouldBe true
        awaitForResult(mailTokenRepository.get("q22")).isDefined shouldBe false
      }
    }
  }

  trait Context {
    val testToken = "token"
    val testUserId = 1L

    val mailTokenRepository: TokenRepository[ChangeMailToken] =
      TokenRepository[ChangeMailToken](TokenType.MailChange, InMemoryPostgresStorage.databaseConnector)
  }
}
