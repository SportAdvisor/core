package io.sportadvisor.http.route

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.sportadvisor.core.system.SystemService
import io.sportadvisor.http._

/**
  * @author sss3 (Vladimir Alekseev)
  */
class SystemRoute extends FailFastCirceSupport {
  val route: Route = pathPrefix("system") {
    path("languages") {
      get {
        languages()
      }
    }
  }

  def languages(): Route = {
    val langs = SystemService.supportedLanguage()
    complete(r(Response.objectResponse(langs, None)))
  }
}
