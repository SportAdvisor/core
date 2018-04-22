package io.sportadvisor

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.sportadvisor.http.Response._
import io.sportadvisor.http.json._
import io.circe.syntax._

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object http extends FailFastCirceSupport {

  import akka.http.scaladsl.server.directives.BasicDirectives._
  import akka.http.scaladsl.server.directives.RouteDirectives._

  case class ValidationError(errors: List[FormError]) extends Rejection

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case a: Throwable => complete(StatusCodes.InternalServerError -> FailResponse(500, None))
  }

  val rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case ValidationError(errors) => complete((StatusCodes.BadRequest, Response.errorResponse(errors).asJson))
    }.result().withFallback(RejectionHandler.default)

  trait Validator[T] extends (T => List[FormError])

  final class DefaultValidator[T](rules: Seq[T => Option[FormError]]) extends Validator[T] {
    override def apply(v1: T): List[FormError] = {
      rules.map(rule => rule(v1))
        .filter(o => o.isDefined)
          .map(o => o.get).toList
    }
  }

  object Validator {
    def apply[T](rules: (T => Option[FormError])*): Validator[T] = new DefaultValidator[T](rules)
  }

  def validatorDirective[T](model: T, validator: Validator[T]): Directive1[T] = {
    validator(model) match {
      case Nil => provide(model)
      case errors: List[Error] => reject(ValidationError(errors))
    }
  }

}
