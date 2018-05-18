package io.sportadvisor

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.stream.ActorMaterializer
import io.sportadvisor.core.user.{MailChangesTokenRepositorySQL, TokenRepositorySQL, UserRepositorySQL, UserService}
import io.sportadvisor.http.HttpRoute
import io.sportadvisor.util.{Config, I18nServiceImpl}
import io.sportadvisor.util.db.{DatabaseConnector, DatabaseMigration}
import io.sportadvisor.util.mail.MailService

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Application extends App {

  def startApplication(): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executor: ExecutionContext = actorSystem.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val config = Config.load()

    new DatabaseMigration(
      config.database.jdbcUrl,
      config.database.username,
      config.database.password
    ).migrateDatabaseSchema()

    implicit val databaseConnector: DatabaseConnector = new DatabaseConnector(
      config.database.jdbcUrl,
      config.database.username,
      config.database.password
    )

    val mailService = MailService(config.mail)

    val userRepository = new UserRepositorySQL(databaseConnector)
    val tokenRepository = new TokenRepositorySQL(databaseConnector)
    val mailTokenRepository = new MailChangesTokenRepositorySQL(databaseConnector)
    val usersService = UserService(config, databaseConnector, mailService)
    val httpRoute = new HttpRoute(usersService)

    val clientRouteLogged =
      DebuggingDirectives.logRequestResult("request tracer", Logging.InfoLevel)(httpRoute.route)
    val _: Future[_] = Http().bindAndHandle(clientRouteLogged, config.http.host, config.http.port)
    ()
  }

  startApplication()

}
