package io.sportadvisor.core.user

import io.sportadvisor.util.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TokenRepository {

  def save(token: RefreshToken): Future[RefreshToken]

  def removeByUser(id: UserID): Future[Unit]

}

class TokenRepositorySQL(val connector: DatabaseConnector)(
    implicit executionContext: ExecutionContext)
    extends TokenTable
    with TokenRepository {

  import connector._
  import connector.profile.api._

  override def save(token: RefreshToken): Future[RefreshToken] = {
    db.run(tokens += token).map(_ => token)
  }

  override def removeByUser(id: UserID): Future[Unit] = {
    val query = tokens.filter(t => t.userId === id)
    db.run(query.delete).map(_ => ())
  }
}
