package io.sportadvisor.core.user

import io.sportadvisor.core.user.UserModels.{ResetPasswordToken, UserID}
import io.sportadvisor.util.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

trait ResetPasswordTokenRepository {

  def save(token: ResetPasswordToken): Future[ResetPasswordToken]

  def get(token: String): Future[Option[ResetPasswordToken]]

  def removeByUser(userID: UserID): Future[Int]

}

class ResetPasswordTokenRepositorySQL(val connector: DatabaseConnector)(
    implicit executionContext: ExecutionContext)
    extends ResetPasswordTokenTable
    with ResetPasswordTokenRepository {

  import connector._
  import connector.profile.api._

  override def save(token: ResetPasswordToken): Future[ResetPasswordToken] = {
    db.run(tokens += token).map(_ => token)
  }

  override def get(token: String): Future[Option[ResetPasswordToken]] =
    db.run(tokens.filter(t => t.token === token).take(1).result.headOption)

  override def removeByUser(userID: UserID): Future[Int] =
    db.run(tokens.filter(t => t.userId === userID).delete)

}
