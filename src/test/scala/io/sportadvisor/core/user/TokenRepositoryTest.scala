package io.sportadvisor.core.user

import java.time.{Duration, LocalDateTime}

import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

/**
  * @author sss3 (Vladimir Alekseev)
  */
class TokenRepositoryTest extends BaseTest {

  "TokenRepositorySQL" when {
    "save" should {
      "return token " in new Context {
        val token: RefreshToken = awaitForResult(tokenRepository.save(RefreshToken(1L, "token", remember = true, LocalDateTime.now())))
        token should not be null
      }
    }

    "remove" should {
      "removeByDate" in new Context {

        import java.time.LocalDateTime
        import scala.concurrent.duration._
        import java.time.Duration

        val rememberTime: FiniteDuration = 14.days
        val notRememberTime: FiniteDuration = 12.hour
        val currentTime: LocalDateTime = LocalDateTime.now()
        val rememberExpired: LocalDateTime = currentTime.minus(Duration.ofMillis(rememberTime.toMillis))
        val notRememberExpired: LocalDateTime = currentTime.minus(Duration.ofMillis(notRememberTime.toMillis))

        tokenRepository.save(RefreshToken(1L, "token", remember = true, currentTime))
        tokenRepository.save(RefreshToken(2L, "token", remember = true, currentTime.minusDays(15L)))
        tokenRepository.save(RefreshToken(3L, "token", remember = false, currentTime))
        tokenRepository.save(RefreshToken(4L, "token", remember = false, currentTime.minusHours(14L)))

        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))

        val tokens1 : Seq[RefreshToken] = awaitForResult(tokenRepository.get(1L))
        tokens1 shouldBe Vector(RefreshToken(1L, "token", remember = true, currentTime))
        val tokens2 : Seq[RefreshToken] = awaitForResult(tokenRepository.get(2L))
        tokens2 shouldBe Vector()
        val tokens3 : Seq[RefreshToken] = awaitForResult(tokenRepository.get(3L))
        tokens3 shouldBe Vector(RefreshToken(3L, "token", remember = false, currentTime))
        val tokens4 : Seq[RefreshToken] = awaitForResult(tokenRepository.get(4L))
        tokens4 shouldBe Vector()
      }
    }
  }

  trait Context {
    val tokenRepository: TokenRepository = new TokenRepositorySQL(InMemoryPostgresStorage.databaseConnector)
  }

}
