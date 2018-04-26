package io.sportadvisor.core.user

import java.time.LocalDateTime

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
  }

  trait Context {
    val tokenRepository: TokenRepository = new TokenRepositorySQL(InMemoryPostgresStorage.databaseConnector)
  }

}
