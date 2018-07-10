package io.sportadvisor.core.user.token

import java.sql.SQLException
import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.{ChangeMailToken, ResetPasswordToken, UserID}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.exception.Exceptions.{DuplicateException, UnhandledException}
import io.sportadvisor.util.db.DatabaseConnector
import cats.instances.string._
import cats.syntax.eq._
import io.sportadvisor.typeclass.{Isomorphism => SAIso}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TokenRepository[T] {

  def save(token: T): Future[Either[ApiError, T]]

  def get(token: String): Future[Option[T]]

  def removeByUser(userID: UserID): Future[Int]

  def removeExpiredTokens(): Future[Int]
}

abstract class SqlTokenRepository[T](val connector: DatabaseConnector)(
    implicit executionContext: ExecutionContext)
    extends TokenRepository[T]
    with TokenTable {

  import connector._
  import connector.profile.api._

  override def save(token: T): Future[Either[ApiError, T]] = {
    db.run((tokens += iso.map(token)).asTry)
      .map {
        case Success(_) => Right(token)
        case Failure(e: SQLException) =>
          if (e.getSQLState === "23505") { Left(new DuplicateException) } else {
            Left(UnhandledException(e))
          }
        case Failure(e) => Left(UnhandledException(e))
      }
  }

  override def get(token: String): Future[Option[T]] =
    db.run(
        tokens
          .filter(t => t.token === token && t.tokenType === tokenType)
          .take(1)
          .result
          .headOption)
      .map(_.map(iso.unmap(_)))

  override def removeByUser(userID: UserID): Future[Int] =
    db.run(tokens.filter(t => t.userId === userID && t.tokenType === tokenType).delete)

  override def removeExpiredTokens(): Future[Int] = {
    val now = LocalDateTime.now()
    db.run(tokens.filter(t => t.expireAt <= now && t.tokenType === tokenType).delete)
  }

  implicit def iso: SAIso[T, ExpiredToken]

  def tokenType: TokenType

}

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
object TokenRepository {
  def apply[T](tType: TokenType, connector: DatabaseConnector)(
      implicit saISO: SAIso[T, ExpiredToken],
      executionContext: ExecutionContext): TokenRepository[T] =
    new SqlTokenRepository[T](connector) {
      override implicit def iso: SAIso[T, ExpiredToken] = saISO

      override def tokenType: TokenType = tType
    }

  def apply[T]()(implicit tokenRepository: TokenRepository[T]): TokenRepository[T] = tokenRepository

  implicit val mailIso: SAIso[ChangeMailToken, ExpiredToken] =
    new SAIso[ChangeMailToken, ExpiredToken] {
      override def map(a: ChangeMailToken): ExpiredToken =
        ExpiredToken(a.userID, a.token, a.expireAt, TokenType.MailChange)

      override def unmap(b: ExpiredToken): ChangeMailToken =
        ChangeMailToken(b.userID, b.token, b.expireAt)
    }

  implicit val pwdIso: SAIso[ResetPasswordToken, ExpiredToken] =
    new SAIso[ResetPasswordToken, ExpiredToken] {
      override def map(a: ResetPasswordToken): ExpiredToken =
        ExpiredToken(a.userId, a.token, a.expireAt, TokenType.ResetPassword)

      override def unmap(b: ExpiredToken): ResetPasswordToken =
        ResetPasswordToken(b.userID, b.token, b.expireAt)
    }
}
