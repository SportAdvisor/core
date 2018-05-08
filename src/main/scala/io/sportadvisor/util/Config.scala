package io.sportadvisor.util

import io.sportadvisor.util.Config.{DatabaseConfig, HttpConfig}
import pureconfig.loadConfig

/**
  * @author sss3 (Vladimir Alekseev)
  */
case class Config(secretKey: String, http: HttpConfig, database: DatabaseConfig)

object Config {

  private[util] case class HttpConfig(host: String, port: Int)
  private[util] case class DatabaseConfig(jdbcUrl: String, username: String, password: String)

  def load(): Config = loadConfig[Config] match {
    case Right(c) => c
    case Left(e) =>
      throw new RuntimeException("Cannot read config file, errors:\n" + e.toList.mkString("\n"))
  }
}
