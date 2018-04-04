package io.sportadvisor.core.user

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TokenRepository {

  def save(token: RefreshToken) : Future[RefreshToken]

  def find(token: Token) : Future[Option[RefreshToken]]

  def remove(refreshToken: RefreshToken) : Future[RefreshToken]

  def removeAll(userID: UserID) : Future[List[RefreshToken]]

}
