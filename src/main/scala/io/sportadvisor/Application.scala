package io.sportadvisor

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.stream.ActorMaterializer
import io.sportadvisor.core.user.{UserRepositorySQL, UserService}
import io.sportadvisor.http.HttpRoute
import io.sportadvisor.util.Config
import io.sportadvisor.util.db.{DatabaseConnector, DatabaseMigration}

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Application extends App {

  def startApplication() = {
    implicit val actorSystem = ActorSystem()
    implicit val executor: ExecutionContext = actorSystem.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val config = Config.load()

    new DatabaseMigration(
      config.database.jdbcUrl,
      config.database.username,
      config.database.password
    ).migrateDatabaseSchema()

    val databaseConnector = new DatabaseConnector(
      config.database.jdbcUrl,
      config.database.username,
      config.database.password
    )

    val userRepository = new UserRepositorySQL(databaseConnector)
    val usersService = new UserService(userRepository, config.secretKey)
    val httpRoute = new HttpRoute(usersService)

    val clientRouteLogged = DebuggingDirectives.logRequestResult("request tracer", Logging.InfoLevel)(httpRoute.route)
    Http().bindAndHandle(clientRouteLogged, config.http.host, config.http.port)
  }

  startApplication()

}
