package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.ResetPasswordToken
import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

import scala.concurrent.Future

class ResetPasswordTokenRepositoryTest extends BaseTest {

  "ResetPasswordTokenRepositorySQL" when {

    "save and get" should {
      "successful save" in new Context {
        val f: Future[ResetPasswordToken] =
          resetTokenRepository.save(ResetPasswordToken(testToken, LocalDateTime.now().plusDays(1)))
        awaitForResult(f)
        val token: Option[ResetPasswordToken] = awaitForResult(resetTokenRepository.get(testToken))
        token.isDefined shouldBe true
      }
    }

  }


  trait Context {
    val testToken = "token"

    val resetTokenRepository: ResetPasswordTokenRepository = new ResetPasswordTokenRepositorySQL(
      InMemoryPostgresStorage.databaseConnector)
  }

}
