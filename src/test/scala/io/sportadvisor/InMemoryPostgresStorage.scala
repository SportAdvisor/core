package io.sportadvisor

import de.flapdoodle.embed.process.runtime.Network._
import io.sportadvisor.util.db.{DatabaseConnector, DatabaseMigration}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

/**
  * @author sss3 (Vladimir Alekseev)
  */
object InMemoryPostgresStorage {
  val dbHost = getLocalHost.getHostAddress
  val dbPort = 25535
  val dbName = "database-name"
  val dbUser = "user"
  val dbPassword = "password"
  val jdbcUrl = s"jdbc:postgresql://$dbHost:$dbPort/$dbName"

  val flywayService = new DatabaseMigration(jdbcUrl, dbUser, dbPassword)
  val process: String = new EmbeddedPostgres(Version.V9_6_8).start(dbHost, dbPort, dbName, dbUser, dbPassword)
  println(process)
  flywayService.dropDatabase()
  flywayService.migrateDatabaseSchema()

  val databaseConnector = new DatabaseConnector(
    InMemoryPostgresStorage.jdbcUrl,
    InMemoryPostgresStorage.dbUser,
    InMemoryPostgresStorage.dbPassword
  )

}
