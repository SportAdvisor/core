package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
class MailTokensRepositoryTest extends BaseTest {

  "MailTokenRepositorySQL" when {
    "save and get" should {
      "successful save" in new Context {
        val f: Future[ChangeMailToken] =
          mailTokenRepository.save(ChangeMailToken(testToken, LocalDateTime.now().plusDays(1)))
        awaitForResult(f)
        val token: Option[ChangeMailToken] = awaitForResult(mailTokenRepository.get(testToken))
        token.isDefined shouldBe true
      }
    }
  }

  trait Context {
    val testToken = "token"

    val mailTokenRepository: MailChangesTokenRepository = new MailChangesTokenRepositorySQL(
      InMemoryPostgresStorage.databaseConnector)
  }
}
