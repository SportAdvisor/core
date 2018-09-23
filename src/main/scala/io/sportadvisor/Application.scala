package io.sportadvisor

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.stream.ActorMaterializer
import io.sportadvisor.core.user.UserModels.{ChangeMailToken, ResetPasswordToken}
import io.sportadvisor.core.auth.{AuthService, AuthTokenRepositorySQL}
import io.sportadvisor.core.system.TokenCleaner
import io.sportadvisor.core.user._
import io.sportadvisor.core.user.token.{TokenRepository, TokenType}
import io.sportadvisor.core.user.token.TokenRepository._
import io.sportadvisor.http.HttpRoute
import io.sportadvisor.util.{Config, I18nService, I18nServiceImpl}
import io.sportadvisor.util.Config.ConfigReaderFailuresExt
import io.sportadvisor.util.db.{DatabaseConnector, DatabaseMigration}
import io.sportadvisor.util.mail.MailService
import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * @author sss3 (Vladimir Alekseev)
  */
@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object Application extends Logging {

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

    implicit val databaseConnector: DatabaseConnector = connectToDb(config)

    val mailService = MailService(config.mail)

    val tokenRepository = new AuthTokenRepositorySQL(databaseConnector)
    val mailTokenRepository =
      TokenRepository[ChangeMailToken](TokenType.MailChange, databaseConnector)
    val resetPasswordTokenRepository =
      TokenRepository[ResetPasswordToken](TokenType.ResetPassword, databaseConnector)

    implicit val authService: AuthService = new AuthService(tokenRepository, config.authKey)
    implicit val i18nService: I18nService = I18nServiceImpl
    val userRepository = new UserRepositorySQL(databaseConnector)
    val usersService = new UserService(userRepository,
                                       authService,
                                       config.secretKey,
                                       mailService,
                                       mailTokenRepository,
                                       resetPasswordTokenRepository)

    val httpRoute = new HttpRoute(usersService)

    val tokenCleaner =
      new TokenCleaner(tokenRepository, mailTokenRepository, resetPasswordTokenRepository)
    actorSystem.scheduler.schedule(12.hour, 3.hour)(tokenCleaner.clean())

    val clientRouteLogged =
      DebuggingDirectives.logRequestResult("request tracer" -> Logging.InfoLevel)(httpRoute.route)
    val _: Future[_] = Http().bindAndHandle(clientRouteLogged, config.http.host, config.http.port)
  }

  def connectToDb(config: Config): DatabaseConnector = {
    new DatabaseMigration(
      config.database.jdbcUrl,
      config.database.username,
      config.database.password
    ).migrateDatabaseSchema()

    new DatabaseConnector(
      config.database.jdbcUrl,
      config.database.username,
      config.database.password
    )
  }

}
