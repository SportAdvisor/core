package io.sportadvisor.core.user

import io.sportadvisor.util.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TokenRepository {

  def save(token: RefreshToken): Future[RefreshToken]

}

class TokenRepositorySQL(val connector: DatabaseConnector)
                        (implicit executionContext: ExecutionContext) extends TokenTable with TokenRepository {

  import connector._
  import connector.profile.api._

  override def save(token: RefreshToken): Future[RefreshToken] = {
    db.run(tokens += token).map(_ => token)
  }
}
