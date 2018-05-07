package io.sportadvisor.core.user

import java.time.{Duration, LocalDateTime}

import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}
import java.time.LocalDateTime
import scala.concurrent.duration._
import java.time.Duration

/**
  * @author sss3 (Vladimir Alekseev)
  */
class TokenRepositoryTest extends BaseTest {

  "TokenRepositorySQL" when {

    val rememberTime: FiniteDuration = 14.days
    val notRememberTime: FiniteDuration = 12.hour
    val currentTime: LocalDateTime = LocalDateTime.now()
    val rememberExpired: LocalDateTime = currentTime.minus(Duration.ofMillis(rememberTime.toMillis))
    val notRememberExpired: LocalDateTime = currentTime.minus(Duration.ofMillis(notRememberTime.toMillis))

    "save" should {
      "return token " in new Context {
        val token: RefreshToken = awaitForResult(tokenRepository.save(RefreshToken(1L, "token", remember = true, LocalDateTime.now())))
        token should not be null
      }
    }

    "removeByDate" should {
      "not remove valid long-live refresh token" in new Context {
        tokenRepository.save(RefreshToken(1L, "token", remember = true, currentTime))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens1: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(1L))
        tokens1 shouldBe Vector(RefreshToken(1L, "token", remember = true, currentTime))
      }

      "remove expired long-live refresh token" in new Context {
        tokenRepository.save(RefreshToken(2L, "token", remember = true, currentTime.minusDays(15L)))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens2: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(2L))
        tokens2 shouldBe Vector()
      }

      "not remove valid refresh token" in new Context {
        tokenRepository.save(RefreshToken(3L, "token", remember = false, currentTime))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens3: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(3L))
        tokens3 shouldBe Vector(RefreshToken(3L, "token", remember = false, currentTime))
      }

      "remove expired refresh token" in new Context {
        tokenRepository.save(RefreshToken(4L, "token", remember = false, currentTime.minusHours(14L)))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens4: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(4L))
        tokens4 shouldBe Vector()
      }
    }
  }

  trait Context {
    val tokenRepository: TokenRepository = new TokenRepositorySQL(InMemoryPostgresStorage.databaseConnector)
  }

}
