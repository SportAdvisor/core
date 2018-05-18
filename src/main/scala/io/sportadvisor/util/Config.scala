package io.sportadvisor.util

import io.sportadvisor.util.Config.{DatabaseConfig, HttpConfig, MailConfig}
import pureconfig.loadConfig

/**
  * @author sss3 (Vladimir Alekseev)
  */
final case class Config(secretKey: String,
                        http: HttpConfig,
                        database: DatabaseConfig,
                        mail: MailConfig)

@SuppressWarnings(Array("org.wartremover.warts.Throw"))
object Config {

  private[util] final case class HttpConfig(host: String, port: Int)
  private[util] final case class DatabaseConfig(jdbcUrl: String, username: String, password: String)
  private[util] final case class MailConfig(smtp: String, smtpPort: Int, user: String, pass: String)

  def load(): Config = loadConfig[Config] match {
    case Right(c) => c
    case Left(e) =>
      throw new RuntimeException("Cannot read config file, errors:\n" + e.toList.mkString("\n"))
  }
}
