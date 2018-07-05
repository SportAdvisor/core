package io.sportadvisor.core.user

import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}
import java.time.LocalDateTime

import scala.concurrent.duration._
import java.time.Duration

import io.sportadvisor.core.user.UserModels.{CreateRefreshToken, RefreshToken, RefreshTokenData}

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
        val token: RefreshTokenData = awaitForResult(tokenRepository.save(CreateRefreshToken(0L, "token", remember = true, LocalDateTime.now())))
        token should not be null
      }
    }

    "removeByDate" should {
      "not remove valid long-live refresh token" in new Context {
        val token: RefreshTokenData = awaitForResult(tokenRepository.save(CreateRefreshToken(1L, "token", remember = true, currentTime)))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens1: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(1L))
        tokens1 shouldBe Vector(RefreshTokenData(token.id, 1L, "token", remember = true, currentTime))
      }

      "remove expired long-live refresh token" in new Context {
        awaitForResult(tokenRepository.save(CreateRefreshToken(1L, "token", remember = true, currentTime.minusDays(15L))))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens2: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(2L))
        tokens2 shouldBe Vector()
      }

      "not remove valid refresh token" in new Context {
        val token: RefreshTokenData = awaitForResult(tokenRepository.save(CreateRefreshToken(3L, "token", remember = false, currentTime)))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens3: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(3L))
        tokens3 shouldBe Vector(RefreshTokenData(token.id, 3L, "token", remember = false, currentTime))
      }

      "remove expired refresh token" in new Context {
        awaitForResult(tokenRepository.save(CreateRefreshToken(4L, "token", remember = false, currentTime.minusHours(14L))))
        awaitForResult(tokenRepository.removeByDate(rememberExpired, notRememberExpired))
        val tokens4: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(4L))
        tokens4 shouldBe Vector()
      }
    }

    "removeByUser" should {
      "not remove by user" in new Context {
        awaitForResult(tokenRepository.save(CreateRefreshToken(5L, "t123", remember = false, currentTime.minusHours(14L))))
        awaitForResult(tokenRepository.save(CreateRefreshToken(5L, "t1232", remember = false, currentTime.minusHours(14L))))
        awaitForResult(tokenRepository.removeByUser(100L))
        val tokens: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(5L))
        tokens.size shouldBe 2
      }

      "remove by user" in new Context {
        awaitForResult(tokenRepository.save(CreateRefreshToken(6L, "t123", remember = false, currentTime.minusHours(14L))))
        awaitForResult(tokenRepository.save(CreateRefreshToken(6L, "t1232", remember = false, currentTime.minusHours(14L))))
        awaitForResult(tokenRepository.removeByUser(6L))
        val tokens: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(6L))
        tokens.isEmpty shouldBe true
      }
    }

    "removeById" should {
      "remove by id" in new Context {
        val token: RefreshTokenData = awaitForResult(tokenRepository.save(CreateRefreshToken(7L, "t123", remember = false, currentTime.minusHours(14L))))
        awaitForResult(tokenRepository.removeById(token.id))
        val tokens: Seq[RefreshToken] = awaitForResult(tokenRepository.getByUserId(7L))
        tokens.isEmpty shouldBe true
      }
    }
  }

  trait Context {
    val tokenRepository: TokenRepository = new TokenRepositorySQL(InMemoryPostgresStorage.databaseConnector)
  }

}
