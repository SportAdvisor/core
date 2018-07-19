package io.sportadvisor.util.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/**
  * @author sss3 (Vladimir Alekseev)
  */
class DatabaseConnector(jdbcUrl: String, dbUser: String, dbPassword: String) {

  private val maxConnections = 20
  private val hikariDataSource = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(jdbcUrl)
    hikariConfig.setUsername(dbUser)
    hikariConfig.setPassword(dbPassword)
    hikariConfig.setMaximumPoolSize(maxConnections)
    new HikariDataSource(hikariConfig)
  }

  val profile: SAPostgresProfile = SAPostgresProfile
  import profile.api._

  val db: profile.backend.DatabaseDef = Database.forDataSource(
    hikariDataSource,
    Option(maxConnections),
    AsyncExecutor.default("SlickExecutor.SA", maxConnections))
  val session: profile.backend.SessionDef = db.createSession()
}
