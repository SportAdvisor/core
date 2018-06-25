package io.sportadvisor

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

  final case class ResourceNotFound(id: Long) extends ApiError(s"Resource [id = $id] not found", None)

  final case class PasswordMismatch() extends ApiError("Password mismatch", None)

}
