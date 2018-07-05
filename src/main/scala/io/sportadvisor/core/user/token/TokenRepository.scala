package io.sportadvisor.core.user.token

import io.sportadvisor.core.user.UserModels.UserID
import io.sportadvisor.exception.ApiError

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TokenRepository[T] {

  def save(token: T): Future[Either[ApiError, T]]

  def get(token: String): Future[Option[T]]

  def removeByUser(userID: UserID): Future[Int]

  def removeExpiredTokens(): Future[Int]
}
