package io.sportadvisor.core.user

import io.sportadvisor.util.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait MailChangesTokenRepository {

  def save(token: ChangeMailToken): Future[ChangeMailToken]

  def get(token: String): Future[Option[ChangeMailToken]]

}

class MailChangesTokenRepositorySQL(val connector: DatabaseConnector)(
    implicit executionContext: ExecutionContext)
    extends MailTokenTable
    with MailChangesTokenRepository {

  import connector._
  import connector.profile.api._

  override def save(token: ChangeMailToken): Future[ChangeMailToken] = {
    db.run(tokens += token).map(_ => token)
  }

  override def get(token: String): Future[Option[ChangeMailToken]] =
    db.run(tokens.filter(t => t.token === token).take(1).result.headOption)

}
