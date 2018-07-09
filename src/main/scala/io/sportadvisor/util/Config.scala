package io.sportadvisor.util

import io.sportadvisor.util.Config.{DatabaseConfig, HttpConfig, MailConfig}
import pureconfig.error.ConfigReaderFailures
import pureconfig.loadConfig

/**
  * @author sss3 (Vladimir Alekseev)
  */
final case class Config(authKey: String,
                        secretKey: String,
                        http: HttpConfig,
                        database: DatabaseConfig,
                        mail: MailConfig)

object Config {

  private[util] final case class HttpConfig(host: String, port: Int)
  private[util] final case class DatabaseConfig(jdbcUrl: String, username: String, password: String)
  private[util] final case class MailConfig(smtp: String, smtpPort: Int, user: String, pass: String)

  def load(): Either[ConfigReaderFailures, Config] = loadConfig[Config]

  implicit class ConfigReaderFailuresExt(val value: ConfigReaderFailures) extends AnyVal {
    def printError(): String = value.toList.foldLeft("")((acc, e) => acc + e.description + "\n")
  }

  def empty(): Config =
    new Config("", "", HttpConfig("", 0), DatabaseConfig("", "", ""), MailConfig("", 0, "", ""))
}
