package io.sportadvisor.http.route.trainer

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.gis.GisService
import io.sportadvisor.http
import io.sportadvisor.http.route.ResourceRoute
import io.sportadvisor.http.route.trainer.TrainerRouteProtocol.CreateTrainer

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
class TrainerRoute(val gis: GisService)(implicit executionContext: ExecutionContext, authService: AuthService)
    extends FailFastCirceSupport
    with ResourceRoute {

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

        ???
      }
    }
  }

}
