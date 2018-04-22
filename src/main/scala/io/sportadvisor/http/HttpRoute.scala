package io.sportadvisor.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.sportadvisor.core.user.UserService
import io.sportadvisor.http.route.UserRoute

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
class HttpRoute(userService: UserService)(implicit executionContext: ExecutionContext) {

  private val userRoute = new UserRoute(userService)

  val route: Route =
    cors() {
      pathPrefix("api") {
        userRoute.route
      }
    }

}
