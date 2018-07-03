package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.ResetPasswordToken
import io.sportadvisor.exception.ApiError
import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

import scala.concurrent.Future

class ResetPasswordTokenRepositoryTest extends BaseTest {

  "ResetPasswordTokenRepositorySQL" when {

    "save and get" should {
      "successful save" in new Context {
        val f: Future[Either[ApiError, ResetPasswordToken]] =
          resetTokenRepository.save(
            ResetPasswordToken(1L, testToken1, LocalDateTime.now().plusDays(1)))
        awaitForResult(f)
        val token: Option[ResetPasswordToken] = awaitForResult(resetTokenRepository.get(testToken1))
        token.isDefined shouldBe true
      }
    }

    "save and removeAll" should {
      "remove all token by user id" should {
        "return 2" in new Context {
          val f1: Future[Either[ApiError, ResetPasswordToken]] =
            resetTokenRepository.save(
              ResetPasswordToken(2L, testToken2, LocalDateTime.now().plusDays(1)))
          val f2: Future[Either[ApiError, ResetPasswordToken]] =
            resetTokenRepository.save(
              ResetPasswordToken(2L, testToken3, LocalDateTime.now().plusDays(1)))
          awaitForResult(f1)
          awaitForResult(f2)
          val removed: Int = awaitForResult(resetTokenRepository.removeByUser(2L))
          val token2: Option[ResetPasswordToken] =
            awaitForResult(resetTokenRepository.get(testToken2))
          val token3: Option[ResetPasswordToken] =
            awaitForResult(resetTokenRepository.get(testToken2))
          token2.isDefined shouldBe false
          token3.isDefined shouldBe false
          removed shouldBe 2
        }
      }
    }

  }

  trait Context {
    val testToken1 = "token1"
    val testToken2 = "token2"
    val testToken3 = "token3"

    val resetTokenRepository: ResetPasswordTokenRepository = new ResetPasswordTokenRepositorySQL(
      InMemoryPostgresStorage.databaseConnector)
  }

}
