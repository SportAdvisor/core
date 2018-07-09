package io.sportadvisor.core.system

import java.time.{Duration, LocalDateTime}

import io.sportadvisor.core.auth.TokenRepository

import scala.concurrent.Future
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class TokenCleaner(repository: TokenRepository,
                   val rememberTime: FiniteDuration = 14.days,
                   val notRememberTime: FiniteDuration = 12.hour) {

  def clean(): Unit = {
    val currentTime = LocalDateTime.now()
    val rememberExpired = currentTime.minus(Duration.ofMillis(rememberTime.toMillis))
    val notRememberExpired = currentTime.minus(Duration.ofMillis(notRememberTime.toMillis))
    val _: Future[Unit] = repository.removeByDate(rememberExpired, notRememberExpired)

  }

}
