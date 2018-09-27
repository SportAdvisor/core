package io.sportadvisor

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.Language
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import io.circe.{Encoder, Json}
import io.sportadvisor.http.Response._
import io.circe.syntax._
import io.sportadvisor.core.user.UserModels.UserID
import io.sportadvisor.http.common._
import io.sportadvisor.util.I18nService
import org.slf4s.Logging
import cats.syntax.eq._
import cats.instances.long._
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.util.i18n.I18nModel.{Language => SALanguage}

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object http extends CirceSupport with Logging with Response.Encoders {

  import akka.http.scaladsl.server.directives.BasicDirectives._
  import akka.http.scaladsl.server.directives.RouteDirectives._
  import akka.http.scaladsl.server.directives.HeaderDirectives._
  import akka.http.scaladsl.server.directives.MarshallingDirectives._

  val authorizationHeader = "Authorization"

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: Throwable =>
      log.error("request failed", e)
      complete(StatusCodes.InternalServerError -> Response.fail(None))
  }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case rejection: SARejection =>
        complete(rejection.code -> rejection.response)
    }
    .result()
    .withFallback(RejectionHandler.default)

  def validate[T: FromRequestUnmarshaller: Validated](implicit i18nService: I18nService): Directive1[T] = {
    entity(as[T]).flatMap { entity =>
      selectLanguage().flatMap { language =>
        Validated[T].validate(entity) match {
          case Nil => provide(entity)
          case errors: List[ValidationResult] =>
            reject(ValidationError(errors.map(_.toFormError(i18nService.errors(language)))))
        }
      }
    }
  }

  def selectLanguage(): Directive1[SALanguage] = {
    extractRequest.map { request ⇒
      val negotiator = LanguageNegotiator(request.headers)
      val pickLanguage = negotiator.pickLanguage(SALanguage.supported.map(mapLang).toList) getOrElse mapLang(
        SALanguage.default)
      negotiator.acceptedLanguageRanges
        .find(l => l.matches(pickLanguage))
        .map(l => l.primaryTag)
        .flatMap(SALanguage.find)
        .getOrElse(SALanguage.default)
    }
  }

  def authenticate()(implicit authService: AuthService): Directive1[UserID] = {
    optionalHeaderValueByName(authorizationHeader).flatMap {
      case Some(token) =>
        authService.userId(token) match {
          case Some(id) => provide(id)
          case _        => reject(Unauthorized())
        }
      case None => reject(Unauthorized())
    }
  }

  def checkAccess(pathId: UserID, fromToken: UserID): Directive0 = {
    if (pathId === fromToken) {
      pass
    } else {
      reject(Forbidden())
    }
  }

  def r[A](response: Response[A])(implicit e: Encoder[A]): (StatusCode, Json) =
    StatusCode.int2StatusCode(response.code) -> response.asJson

  private def mapLang(language: SALanguage): Language = Language(language.entryName.toLowerCase)

}
