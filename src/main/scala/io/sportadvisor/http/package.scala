package io.sportadvisor

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Language
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.sportadvisor.http.Response._
import io.sportadvisor.http.json._
import io.sportadvisor.http.json.Codecs._
import io.circe.syntax._
import io.sportadvisor.core.user.{AuthTokenContent, UserID}
import io.sportadvisor.util.{I18nService, JwtUtil}
import io.sportadvisor.util.i18n.I18n

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object http extends FailFastCirceSupport {

  import akka.http.scaladsl.server.directives.BasicDirectives._
  import akka.http.scaladsl.server.directives.RouteDirectives._
  import akka.http.scaladsl.server.directives.HeaderDirectives._

  final case class ValidationError(errors: List[FormError]) extends Rejection
  final case class ValidationResult(field: String, msgId: String) {
    def toFormError(i18n: I18n): FormError = {
      FormError(field, i18n.t(msgId))
    }
  }

  val authorizationHeader = "Authorization"

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case _: Throwable => complete(StatusCodes.InternalServerError -> Response.failResponse(None))
  }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationError(errors) =>
        complete((StatusCodes.BadRequest, Response.errorResponse(errors).asJson))
      case AuthorizationFailedRejection =>
        complete(
          (StatusCodes.Unauthorized, Response.emptyResponse(StatusCodes.Unauthorized.intValue)))
    }
    .result()
    .withFallback(RejectionHandler.default)

  trait Validator[T] extends (T => List[ValidationResult])

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
  final class DefaultValidator[T](rules: Seq[T => Option[ValidationResult]]) extends Validator[T] {
    override def apply(v1: T): List[ValidationResult] = {
      rules.flatMap(rule => rule(v1)).toList
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

  def authenticate(secretKey: String): Directive1[UserID] = {
    optionalHeaderValueByName(authorizationHeader).flatMap {
      case Some(token) =>
        JwtUtil.decode[AuthTokenContent](token, secretKey) match {
          case Some(r) => provide(r.userID)
          case _       => reject(AuthorizationFailedRejection)
        }
      case None => reject(AuthorizationFailedRejection)
    }
  }

}
