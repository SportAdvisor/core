package io.sportadvisor

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.stream.ActorMaterializer
import io.sportadvisor.core.user._
import io.sportadvisor.http.HttpRoute
import io.sportadvisor.util.Config
import io.sportadvisor.util.Config.ConfigReaderFailuresExt
import io.sportadvisor.util.db.{DatabaseConnector, DatabaseMigration}
import io.sportadvisor.util.mail.MailService
import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Application extends Logging {

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executor: ExecutionContext = actorSystem.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val config = Config.load() match {
      case Left(e) =>
        log.info("failed load config " + e.printError())
        System.exit(1)
        Config.empty()
      case Right(c) => c
    }

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

    val tokenRepository = new AuthTokenRepositorySQL(databaseConnector)
    val usersService = UserService(config, databaseConnector, mailService)
    val httpRoute = new HttpRoute(usersService)

    val tokenCleaner = new TokenCleaner(tokenRepository)
    actorSystem.scheduler.schedule(12.hour, 3.hour)(tokenCleaner.clean())

    val clientRouteLogged =
      DebuggingDirectives.logRequestResult("request tracer" -> Logging.InfoLevel)(httpRoute.route)
    val _: Future[_] = Http().bindAndHandle(clientRouteLogged, config.http.host, config.http.port)
  }

}
