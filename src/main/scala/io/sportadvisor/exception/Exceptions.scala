package io.sportadvisor.exception

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Exceptions {

  final case class DuplicateException() extends ApiError("Duplication error", None) {}

  final case class UnhandledException(err: Throwable) extends ApiError(err.getMessage, Some(err))

  final case class ResourceNotFound[T](id: T) extends ApiError(s"Resource [id = $id] not found", None)

  final case class TokenDoesNotExist(tokenType: String) extends ApiError(s"$tokenType doesnt exists", None)

  final case class TokenExpired(tokenType: String) extends ApiError(s"$tokenType expired", None)
}
