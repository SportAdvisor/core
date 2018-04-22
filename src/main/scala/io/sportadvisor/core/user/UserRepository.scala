package io.sportadvisor.core.user

import io.sportadvisor.exception.DuplicateException

import scala.concurrent.Future


/**
  * @author sss3 (Vladimir Alekseev)
  */
trait UserRepository {

  def find(email: String) : Future[Option[UserData]]

  def get(userID: UserID) : Future[Option[UserData]]

  def save(user: User) : Future[Either[DuplicateException, UserData]]

  def remove(userID: UserID) : Future[Option[UserData]]

}
