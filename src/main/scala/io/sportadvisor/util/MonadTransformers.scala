package io.sportadvisor.util

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
object MonadTransformers {

  implicit class FutureOptionMonadTransformer[A](t: Future[Option[A]])(
      implicit executionContext: ExecutionContext) {

    def mapT[B](f: A => B): Future[Option[B]] =
      t.map(_.map(f))

    def filterT(f: A => Boolean): Future[Option[A]] =
      t.map {
        case Some(data) if f(data) =>
          Some(data)
        case _ =>
          None
      }

    def flatMapT[B](f: A => Future[Option[B]]): Future[Option[B]] =
      t.flatMap {
        case Some(data) =>
          f(data)
        case None =>
          Future.successful(None)
      }

    def flatMapTOuter[B](f: A => Future[B]): Future[Option[B]] =
      t.flatMap {
        case Some(data) =>
          f(data).map(Some.apply)
        case None =>
          Future.successful(None)
      }

    def flatMapTInner[B](f: A => Option[B]): Future[Option[B]] =
      t.map(_.flatMap(f))

  }

  implicit class FutureEitherMonadTransformer[L, R](val t: Future[Either[L, R]]) extends AnyVal {

    def flatMapRight[V](f: R => Future[V])(
        implicit executionContext: ExecutionContext): Future[Either[L, V]] =
      t.flatMap {
        case Left(l)  => Future.successful(Left(l))
        case Right(r) => f(r).map(Right(_))
      }

  }

}
