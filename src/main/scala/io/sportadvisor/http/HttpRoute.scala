package io.sportadvisor.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.sportadvisor.core.user.UserService
import io.sportadvisor.http.route.user.UserRoute
import io.sportadvisor.util.I18nServiceImpl

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
class HttpRoute(userService: UserService)(implicit executionContext: ExecutionContext) {

  private val userRoute = new UserRoute(userService) with I18nServiceImpl

  val route: Route =
    cors() {
      pathPrefix("api") {
        userRoute.route
      } ~ pathPrefix("healthcheck") {
        pathEndOrSingleSlash {
          complete(StatusCodes.OK -> "OK")
        }
      }
    }

}
