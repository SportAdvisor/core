package io.sportadvisor

import io.sportadvisor.core.user.UserID

import scala.util.control.NoStackTrace

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object exception {

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  abstract class ApiError(val msg: String, val error: Option[Throwable])
      extends RuntimeException(msg)
      with NoStackTrace

  final case class DuplicateException() extends ApiError("Duplication error", None) {}

  final case class UnhandledException(err: Throwable) extends ApiError(err.getMessage, Some(err))

  final case class UserNotFound(id: UserID) extends ApiError("User not found", None)

  final case class PasswordMismatch() extends ApiError("Password mismatch", None)

}
