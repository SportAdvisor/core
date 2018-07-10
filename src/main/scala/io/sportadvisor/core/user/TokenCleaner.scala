package io.sportadvisor.core.user

import java.time.LocalDateTime

import scala.concurrent.duration._
import java.time.Duration

import io.sportadvisor.core.user.UserModels.{ChangeMailToken, ResetPasswordToken}
import io.sportadvisor.core.user.token.TokenRepository
import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class TokenCleaner(
    repository: AuthTokenRepository,
    mailTokenRepository: TokenRepository[ChangeMailToken],
    resetPwdTokenRepository: TokenRepository[ResetPasswordToken],
    val rememberTime: FiniteDuration = 14.days,
    val notRememberTime: FiniteDuration = 12.hour)(implicit executionContext: ExecutionContext)
    extends Logging {

  def clean(): Unit = {
    val currentTime = LocalDateTime.now()
    val rememberExpired = currentTime.minus(Duration.ofMillis(rememberTime.toMillis))
    val notRememberExpired = currentTime.minus(Duration.ofMillis(notRememberTime.toMillis))
    logDeleted(mailTokenRepository.removeExpiredTokens(), "mail")
    logDeleted(resetPwdTokenRepository.removeExpiredTokens(), "password")
    logDeleted(repository.removeByDate(rememberExpired, notRememberExpired), "auth")

  }

  private def logDeleted(f: Future[Int], tokenType: String): Unit = {
    f onComplete {
      case Success(c) => log.debug(s"delete $c tokens [$tokenType]")
      case Failure(e) => log.warn(s"failed delete tokens[$tokenType]", e)
    }
  }

}
