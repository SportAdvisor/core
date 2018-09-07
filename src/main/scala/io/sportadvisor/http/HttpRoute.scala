package io.sportadvisor.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.gis.GisService
import io.sportadvisor.core.sport.StubSportService
import io.sportadvisor.core.trainer.StubTrainerService
import io.sportadvisor.core.user.UserService
import io.sportadvisor.http.route.SystemRoute
import io.sportadvisor.http.route.trainer.TrainerRoute
import io.sportadvisor.http.route.user.UserRoute
import io.sportadvisor.util.I18nService

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
class HttpRoute(userService: UserService, gis: GisService)(implicit executionContext: ExecutionContext,
                                                           authService: AuthService,
                                                           i18nServiceImpl: I18nService) {

  private val userRoute = new UserRoute(userService)
  private val trainerRoute = new TrainerRoute(gis, StubTrainerService, StubSportService)
  private val systemRoute = new SystemRoute

  val route: Route =
    cors() {
      handleExceptions(exceptionHandler) {
        handleRejections(rejectionHandler) {
          pathPrefix("api") {
            userRoute.route ~ systemRoute.route ~ trainerRoute.route
          } ~ pathPrefix("healthcheck") {
            pathEndOrSingleSlash {
              complete(StatusCodes.OK -> "OK")
            }
          }
        }
      }
    }

}
