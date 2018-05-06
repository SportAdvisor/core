package io.sportadvisor.core.user

import java.time.LocalDateTime
import scala.concurrent.duration._
import java.time.Duration

class TokenCleaner(repository: TokenRepository, val rememberTime: FiniteDuration = 14.days,
                   val notRememberTime: FiniteDuration = 12.hour)  {

  def clean() : Unit = {
    val currentTime = LocalDateTime.now()
    val rememberExpired = currentTime.minus(Duration.ofMillis(rememberTime.toMillis))
    val notRememberExpired = currentTime.minus(Duration.ofMillis(notRememberTime.toMillis))
    repository.removeByDate(rememberExpired, notRememberExpired)
  }

}
