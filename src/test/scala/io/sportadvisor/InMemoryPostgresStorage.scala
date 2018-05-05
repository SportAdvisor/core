package io.sportadvisor

import java.io.{BufferedReader, InputStream, InputStreamReader}

import de.flapdoodle.embed.process.config.IRuntimeConfig
import de.flapdoodle.embed.process.distribution.Distribution
import de.flapdoodle.embed.process.runtime.Network._
import io.sportadvisor.util.db.{DatabaseConnector, DatabaseMigration}
import ru.yandex.qatools.embed.postgresql.{Command, PostgresStarter}
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.{Credentials, Net, Storage, Timeout}
import ru.yandex.qatools.embed.postgresql.config.{PostgresConfig, RuntimeConfigBuilder}
import ru.yandex.qatools.embed.postgresql.distribution.Version
import org.slf4s.Logging

/**
  * @author sss3 (Vladimir Alekseev)
  */
object InMemoryPostgresStorage extends Logging {
  val dbHost = getLocalHost.getHostAddress
  val dbPort = 25535
  val dbName = "database-name"
  val dbUser = "user"
  val dbPassword = "password"
  val jdbcUrl = s"jdbc:postgresql://$dbHost:$dbPort/$dbName"

  val psqlConfig = new PostgresConfig(
    Version.V9_6_8, new Net(dbHost, dbPort),
    new Storage(dbName), new Timeout(),
    new Credentials(dbUser, dbPassword)
  )
  val psqlInstance = PostgresStarter.getInstance(config())
  val flywayService = new DatabaseMigration(jdbcUrl, dbUser, dbPassword)
  val process = psqlInstance.prepare(psqlConfig).start()
  flywayService.dropDatabase()
  flywayService.migrateDatabaseSchema()

  val databaseConnector = new DatabaseConnector(
    InMemoryPostgresStorage.jdbcUrl,
    InMemoryPostgresStorage.dbUser,
    InMemoryPostgresStorage.dbPassword
  )

  private def config(): IRuntimeConfig = {
    new RuntimeConfigBuilder()
      .defaultsWithLogger(Command.Postgres, log.underlying)
        .commandLinePostProcessor((distribution: Distribution, args: java.util.List[String]) => {
          val whoami = Runtime.getRuntime.exec("whoami")
          val ls = Runtime.getRuntime.exec("ls -R /tmp | awk '\n/:$/&&f{s=$0;f=0}\n/:$/&&!f{sub(/:$/,\"\");s=$0;f=1;next}\nNF&&f{ print s\"/\"$0 }'")
          printProcess("whoami", whoami, whoami.getInputStream)
          printProcess("whoami", whoami, whoami.getErrorStream)
          whoami.waitFor()
          whoami.destroy()
          printProcess("ls", ls, ls.getInputStream)
          printProcess("ls", ls, ls.getErrorStream)
          ls.waitFor()
          ls.destroy()
          args
        })
        .build()
  }

  private def printProcess(name: String, process: Process, in: => InputStream): Unit = {
    autoClose(new BufferedReader(new InputStreamReader(in))) { br =>
      var s: String = null
      while ({s = br.readLine(); s != null}) {
        log.info(s"$name >> $s")
      }
    }
  }

  def autoClose[A <: AutoCloseable,B](closeable: A)(fun: (A) ⇒ B): B = {
    try {
      fun(closeable)
    } finally {
      closeable.close()
    }
  }
}
