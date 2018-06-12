package io.sportadvisor

import scala.util.control.NoStackTrace

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object exception {

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  abstract class SAException(val msg: String, val error: Option[Throwable])
      extends RuntimeException(msg)
      with NoStackTrace {}

  final case class DuplicateException() extends SAException("Duplication error", None) {}

  final case class UnhandledException(err: Throwable)
      extends SAException(err.getMessage, Some(err)) {}

  final case class UserNotFound() extends SAException("User not found", None) {}

}
