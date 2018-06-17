package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

import scala.concurrent.Future

class ResetPasswordRepositoryTest extends BaseTest {

  "ResetPasswordTokenRepositorySQL" when {
    "save and get" should {
      "successful save" in new Context {
        val f: Future[ResetPasswordToken] =
          resetPasswordRepository.save(ResetPasswordToken(testToken, LocalDateTime.now().plusDays(1)))
        awaitForResult(f)
        val token: Option[ResetPasswordToken] = awaitForResult(resetPasswordRepository.get(testToken))
        token.isDefined shouldBe true
      }
    }
  }


  trait Context {
    val testToken = "token"

    val resetPasswordRepository: ResetPasswordTokenRepository = new ResetPasswordTokenRepositorySQL(
      InMemoryPostgresStorage.databaseConnector)
  }

}
