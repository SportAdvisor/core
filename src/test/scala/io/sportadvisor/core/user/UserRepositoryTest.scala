package io.sportadvisor.core.user

import io.sportadvisor.exception.DuplicateException
import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

/**
  * @author sss3 (Vladimir Alekseev)
  */

class UserRepositoryTest extends BaseTest {

  "UserRepositorySQL" when {
    "save" should {
      "successful save " in new Context {
        val value = awaitForResult(userRepository.save(CreateUser("test", "test", "test")))
        value.isRight shouldBe true
      }

      "duplicate error on new user" in new Context {
        awaitForResult(userRepository.save(CreateUser("duplicate", "t", "t")))
        val value = awaitForResult(userRepository.save(CreateUser("duplicate", "t", "t")))
        value.isLeft shouldBe true
      }
    }
  }


  trait Context {
    val userRepository: UserRepository = new UserRepositorySQL(InMemoryPostgresStorage.databaseConnector)
  }
}
