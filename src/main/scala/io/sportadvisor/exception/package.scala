package io.sportadvisor

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object exception {

  trait SAException {
    def error: Option[Throwable]
  }

  final case class DuplicateException() extends SAException {
    override def error: Option[Throwable] = None
  }

  final case class UnhandledException(err: Throwable) extends SAException {
    override def error: Option[Throwable] = Option(err)
  }

  final case class ApiError(exception: Option[SAException])

}
