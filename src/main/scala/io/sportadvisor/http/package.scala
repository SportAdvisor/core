package io.sportadvisor

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Language
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.sportadvisor.http.Response._
import io.sportadvisor.http.json._
import io.sportadvisor.http.json.Codecs._
import io.circe.syntax._
import io.sportadvisor.util.i18n.I18n

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object http extends FailFastCirceSupport {

  import akka.http.scaladsl.server.directives.BasicDirectives._
  import akka.http.scaladsl.server.directives.RouteDirectives._

  case class ValidationError(errors: List[FormError]) extends Rejection
  case class ValidationResult(field: String, msgId: String) {
    def toFormError(i18n: I18n): FormError = {
      FormError(field, i18n.t(msgId))
    }
  }

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case _: Throwable => complete(StatusCodes.InternalServerError -> Response.failResponse())
  }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationError(errors) =>
        complete((StatusCodes.BadRequest, Response.errorResponse(errors).asJson))
    }
    .result()
    .withFallback(RejectionHandler.default)

  trait Validator[T] extends (T => List[ValidationResult])

  final class DefaultValidator[T](rules: Seq[T => Option[ValidationResult]]) extends Validator[T] {
    override def apply(v1: T): List[ValidationResult] = {
      rules
        .map(rule => rule(v1))
        .filter(o => o.isDefined)
        .map(o => o.get)
        .toList
    }
  }

  object Validator {
    def apply[T](rules: (T => Option[ValidationResult])*): Validator[T] =
      new DefaultValidator[T](rules)
  }

  def validatorDirective[T](model: T,
                            validator: Validator[T],
                            i18nService: I18nService): Directive1[T] = {
    selectLanguage().tflatMap(t =>
      validator(model) match {
        case Nil => provide(model)
        case errors: List[ValidationResult] =>
          reject(ValidationError(errors.map(_.toFormError(i18nService.errors(t._1)))))
    })
  }

  def selectLanguage(): Directive1[String] = {
    extractRequest.map { request ⇒
      val negotiator = LanguageNegotiator(request.headers)
      val pickLanguage = negotiator.pickLanguage(List(Language("ru"), Language("en"))) getOrElse Language(
        "en")
      negotiator.acceptedLanguageRanges
        .find(l => l.matches(pickLanguage))
        .map(l => l.primaryTag)
        .getOrElse("en")
    }
  }

}
