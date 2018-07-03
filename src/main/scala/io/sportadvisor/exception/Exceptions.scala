package io.sportadvisor.exception

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Exceptions {

  final case class DuplicateException() extends ApiError("Duplication error", None) {}

  final case class UnhandledException(err: Throwable) extends ApiError(err.getMessage, Some(err))

  final case class ResourceNotFound(id: Long)
      extends ApiError(s"Resource [id = $id] not found", None)

  final case class TokenDoesntExist(tokenType: String)
      extends ApiError(s"$tokenType doesnt exists", None)

  final case class TokenExpired(tokenType: String) extends ApiError(s"$tokenType expired", None)
}
