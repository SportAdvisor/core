package io.sportadvisor.core.user

import java.sql.SQLException

import io.sportadvisor.core.user.UserModels.{ResetPasswordToken, UserID}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.exception.Exceptions.{DuplicateException, UnhandledException}
import io.sportadvisor.util.db.DatabaseConnector
import cats.instances.string._
import cats.syntax.eq._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait ResetPasswordTokenRepository {

  def save(token: ResetPasswordToken): Future[Either[ApiError, ResetPasswordToken]]

  def get(token: String): Future[Option[ResetPasswordToken]]

  def removeByUser(userID: UserID): Future[Int]

}

class ResetPasswordTokenRepositorySQL(val connector: DatabaseConnector)(
    implicit executionContext: ExecutionContext)
    extends ResetPasswordTokenTable
    with ResetPasswordTokenRepository {

  import connector._
  import connector.profile.api._

  override def save(token: ResetPasswordToken): Future[Either[ApiError, ResetPasswordToken]] = {
    db.run((tokens += token).asTry)
      .map {
        case Success(_) => Right(token)
        case Failure(e: SQLException) =>
          if (e.getSQLState === "23505") { Left(new DuplicateException) } else {
            Left(UnhandledException(e))
          }
        case Failure(e) => Left(UnhandledException(e))
      }
  }

  override def get(token: String): Future[Option[ResetPasswordToken]] =
    db.run(tokens.filter(t => t.token === token).take(1).result.headOption)

  override def removeByUser(userID: UserID): Future[Int] =
    db.run(tokens.filter(t => t.userId === userID).delete)

}
