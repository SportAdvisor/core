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
    } ~ path("currencies") {
      get {
        currencies()
      }
    } ~ path("sex") {
      get {
        sex()
      }
    }
  }

  def languages(): Route = {
    val langs = SystemService.supportedLanguage()
    complete(r(Response.data(langs, Some(s"/api/system/languages"))))
  }

  def currencies(): Route = {
    val curr = SystemService.supportedCurrency()
    complete(r(Response.data(curr, Some(s"/api/system/currencies"))))
  }

  def sex(): Route = {
    val sex = SystemService.supportedSex()
    complete(r(Response.data(sex, Some(s"/api/system/sex"))))
  }
}
