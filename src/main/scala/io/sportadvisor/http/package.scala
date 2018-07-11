package io.sportadvisor

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.Language
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.http.scaladsl.server.directives.MarshallingDirectives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
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

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object http extends FailFastCirceSupport with Logging with Response.Encoders {



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

  def validate[T: FromRequestUnmarshaller: Validated](
      implicit i18nService: I18nService): Directive1[T] = {
    entity(as[T]).tflatMap { eT =>
      selectLanguage().tflatMap { lT =>
        Validated[T].validate(eT._1) match {
          case Nil => provide(eT._1)
          case errors: List[ValidationResult] =>
            reject(ValidationError(errors.map(_.toFormError(i18nService.errors(lT._1)))))
        }
      }
    }
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

}
