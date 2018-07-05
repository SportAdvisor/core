package io.sportadvisor.core.user.token

import java.sql.SQLException
import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.{ChangeMailToken, UserID}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.exception.Exceptions.{DuplicateException, UnhandledException}
import io.sportadvisor.util.db.DatabaseConnector
import cats.instances.string._
import cats.syntax.eq._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * @author sss3 (Vladimir Alekseev)
  */
class MailChangesTokenRepositorySQL(val connector: DatabaseConnector)(
    implicit executionContext: ExecutionContext)
    extends MailTokenTable
    with TokenRepository[ChangeMailToken] {

  import connector._
  import connector.profile.api._

  override def save(token: ChangeMailToken): Future[Either[ApiError, ChangeMailToken]] = {
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

  override def get(token: String): Future[Option[ChangeMailToken]] =
    db.run(tokens.filter(t => t.token === token).take(1).result.headOption)

  override def removeByUser(userID: UserID): Future[Int] =
    db.run(tokens.filter(t => t.userId === userID).delete)

  override def removeExpiredTokens(): Future[Int] = {
    val now = LocalDateTime.now()
    db.run(tokens.filter(t => t.expireAt <= now).delete)
  }
}
