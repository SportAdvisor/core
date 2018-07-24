package io.sportadvisor

import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object util extends Logging {

  //scalastyle:off
  object future {
    implicit class FutureOps[T](val future: Future[T]) extends AnyVal {
      def toSuccess[R](res: => R)(implicit executionException: ExecutionContext): Future[R] =
        future.transform(t =>
          t match {
            case Success(_) => Success(res)
            case Failure(err) =>
              log.error("Failed execute future", err)
              Success(res)
        })
    }
  }

}
