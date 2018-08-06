package io.sportadvisor.util.db

import java.sql.SQLException

import cats.syntax.eq._
import cats.instances.string._
import io.sportadvisor.exception.ApiError
import io.sportadvisor.exception.Exceptions.{DuplicateException, UnhandledException}

import scala.util.{Failure, Success, Try}

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Extractors {

  def handleDuplicate[T, R](res: Try[T], f: T => R): Either[ApiError, R] = res match {
    case Success(v) => Right(f(v))
    case Failure(e: SQLException) =>
      if (e.getSQLState === "23505") { Left(new DuplicateException) } else {
        Left(UnhandledException(e))
      }
    case Failure(e) => Left(UnhandledException(e))
  }

}
