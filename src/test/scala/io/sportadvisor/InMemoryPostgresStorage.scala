package io.sportadvisor

import java.nio.file.Paths

import de.flapdoodle.embed.process.runtime.Network._
import io.sportadvisor.util.db.{DatabaseConnector, DatabaseMigration}
import ru.yandex.qatools.embed.postgresql.PostgresStarter
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.{Credentials, Net, Storage, Timeout}
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig
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

  val psqlConfig = new PostgresConfig(
    Version.V9_6_8, new Net(dbHost, dbPort),
    new Storage(dbName, Paths.get(System.getProperty("java.io.tmpdir"), "pgembed").toAbsolutePath.toString), new Timeout(),
    new Credentials(dbUser, dbPassword)
  )
  val psqlInstance = PostgresStarter.getDefaultInstance
  val flywayService = new DatabaseMigration(jdbcUrl, dbUser, dbPassword)
  val process = psqlInstance.prepare(psqlConfig).start()
  flywayService.dropDatabase()
  flywayService.migrateDatabaseSchema()

  val databaseConnector = new DatabaseConnector(
    InMemoryPostgresStorage.jdbcUrl,
    InMemoryPostgresStorage.dbUser,
    InMemoryPostgresStorage.dbPassword
  )

}
