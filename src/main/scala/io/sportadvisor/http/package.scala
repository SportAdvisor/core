package io.sportadvisor

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.Language
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.{Encoder, Json}
import io.sportadvisor.http.Response._
import io.sportadvisor.http.json._
import io.circe.syntax._
import io.sportadvisor.core.user.{AuthTokenContent, UserID}
import io.sportadvisor.util.{I18nService, JwtUtil}
import io.sportadvisor.util.i18n.I18n
import org.slf4s.Logging

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object http extends FailFastCirceSupport with Logging {

  import akka.http.scaladsl.server.directives.BasicDirectives._
  import akka.http.scaladsl.server.directives.RouteDirectives._
  import akka.http.scaladsl.server.directives.HeaderDirectives._

  final case class ValidationError(errors: List[FormError]) extends Rejection
  final case class ValidationResult(field: String, msgId: String) {
    def toFormError(i18n: I18n): FormError = {
      FormError(field, i18n.t(msgId))
    }
  }
  trait SARejection extends Rejection {
    def code: StatusCode
  }
  final case class Forbidden() extends SARejection {
    override def code: StatusCode = StatusCodes.Forbidden
  }

  val authorizationHeader = "Authorization"

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: Throwable =>
      log.error("request failed", e)
      complete(StatusCodes.InternalServerError -> Response.failResponse(None))
  }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationError(errors) =>
        complete((StatusCodes.BadRequest, Response.errorResponse(errors)))
      case AuthorizationFailedRejection =>
        complete(
          (StatusCodes.Unauthorized, Response.emptyResponse(StatusCodes.Unauthorized.intValue)))
      case rejection: SARejection =>
        complete(rejection.code -> Response.emptyResponse(rejection.code.intValue()))
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
                            i18nService: I18nService): Directive0 = {
    selectLanguage().tflatMap(t =>
      validator(model) match {
        case Nil => pass
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

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  def checkAccess(pathId: UserID, fromToken: UserID): Directive0 = {
    if (pathId == fromToken) {
      pass
    } else {
      reject(Forbidden())
    }
  }

  def r[A](response: Response[A])(implicit e: Encoder[A]): (StatusCode, Json) =
    StatusCode.int2StatusCode(response.code) -> response.asJson

}
