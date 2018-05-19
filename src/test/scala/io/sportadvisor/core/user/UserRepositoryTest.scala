package io.sportadvisor.core.user

import io.sportadvisor.{BaseTest, InMemoryPostgresStorage}

import scala.util.Random

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

      "successful update" in new Context {
        val value = awaitForResult(userRepository.save(CreateUser(Random.nextString(5), "test", "test")))
        value.isRight shouldBe true
        val user: UserData = value.right.get
        awaitForResult(userRepository.save(user.copy(email = "newEmail"))).isRight shouldBe true
        val updatedUser = awaitForResult(userRepository.find("newEmail")).get
        updatedUser.id shouldEqual user.id
      }
    }
  }


  trait Context {
    val userRepository: UserRepository = new UserRepositorySQL(InMemoryPostgresStorage.databaseConnector)
  }
}
