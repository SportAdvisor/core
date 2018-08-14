package io.sportadvisor.http.route

import akka.http.scaladsl.server.Route

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait ResourceRoute {

  def route: Route

}
