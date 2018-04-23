package io.sportadvisor.util.db

import org.flywaydb.core.Flyway

/**
  * @author sss3 (Vladimir Alekseev)
  */
class DatabaseMigration(jdbcUrl: String, dbUser: String, dbPassword: String) {

  private val flyway = new Flyway()
  flyway.setDataSource(jdbcUrl, dbUser, dbPassword)

  def migrateDatabaseSchema() : Unit = flyway.migrate()

  def dropDatabase(): Unit = flyway.clean()

}