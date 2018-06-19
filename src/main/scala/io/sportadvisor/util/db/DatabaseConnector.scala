package io.sportadvisor.util.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/**
  * @author sss3 (Vladimir Alekseev)
  */
@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class DatabaseConnector(jdbcUrl: String, dbUser: String, dbPassword: String) {

  private val hikariDataSource = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(jdbcUrl)
    hikariConfig.setUsername(dbUser)
    hikariConfig.setPassword(dbPassword)

    new HikariDataSource(hikariConfig)
  }

  val profile: SAPostgresProfile = SAPostgresProfile
  import profile.api._

  val db: profile.backend.DatabaseDef = Database.forDataSource(hikariDataSource, None)
  db.createSession()
}
