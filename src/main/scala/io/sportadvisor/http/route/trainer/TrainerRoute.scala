package io.sportadvisor.http.route.trainer

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.gis.GisService
import io.sportadvisor.core.sport.SportService
import io.sportadvisor.core.trainer.TrainerModels.{CreateTrainer, LimitPagesOnUser, NotUniqueSports}
import io.sportadvisor.core.trainer.TrainerService
import io.sportadvisor.exception.ApiError
import io.sportadvisor.http
import io.sportadvisor.http.Response.{FieldFormError, FormError}
import io.sportadvisor.http.route.ResourceRoute
import io.sportadvisor.http.route.trainer.TrainerRouteProtocol._
import io.sportadvisor.http.route.trainer.TrainerRouteValidators._
import io.sportadvisor.util.I18nService
import io.sportadvisor.util.i18n.I18nModel.Language
import io.sportadvisor.util.i18n.I18nModel.implicits._
import org.slf4s.Logging

//import scala.concurrent.ExecutionContext
import scala.util.Success

/**
  * @author sss3 (Vladimir Alekseev)
  */
class TrainerRoute(val gis: GisService, service: TrainerService, sportService: SportService)(
    implicit /*executionContext: ExecutionContext,*/
    authService: AuthService,
    i18n: I18nService)
    extends FailFastCirceSupport
    with ResourceRoute
    with Logging {

  import http._

  override val route: Route = pathPrefix("trainers") {
    pathEnd {
      post {
        handleCreate()
      }
    }
  }

  private def handleCreate(): Route = {
    authenticate.apply { userId =>
      validate[CreateTrainer].apply { request =>
        selectLanguage() { lang =>
          onComplete(service.create(request, userId)) {
            case Success(res) =>
              res match {
                case Left(err) => complete(handleCreateTrainerErrors(err, lang))
                case Right(trainer) =>
                  respondWithHeader(Location(s"/api/trainers/${trainer.id}")) {
                    complete(r(Response.empty(StatusCodes.Created.intValue)))
                  }
              }
            case _ => complete(r(Response.fail(None)))
          }
        }
      }
    }
  }

  private def handleCreateTrainerErrors(apiError: ApiError, lang: Language): (StatusCode, Json) = {
    val handler: PartialFunction[ApiError, (StatusCode, Json)] = {
      case NotUniqueSports(ids) =>
        val errors = ids.map { id =>
          FieldFormError(
            "sports",
            i18n
              .errors(lang)
              .t("Page with sport '%s' already registered (%s)",
                 sportService.find(id._1).flatMap(_.value.orDefault(lang)).getOrElse(""),
                 id._2)
          )
        }.toList
        r(Response.error(errors))
      case LimitPagesOnUser() =>
        r(Response.error(List(FormError(i18n.errors(lang).t("Exceeded the limit of trainers pages")))))
    }

    (handler orElse apiErrorHandler).apply(apiError)
  }

  private def apiErrorHandler(): PartialFunction[ApiError, (StatusCode, Json)] = {
    case exception =>
      exception.error.fold(log.error(s"Api error: ${exception.msg}")) { e =>
        log.error(s"Api error: ${exception.msg}", e)
      }
      r(Response.fail(None))
  }

}
