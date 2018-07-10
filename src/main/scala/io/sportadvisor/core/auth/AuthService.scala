package io.sportadvisor.core.auth

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import io.sportadvisor.core.auth.AuthModels._

import scala.concurrent.duration._
import io.sportadvisor.core.user.UserModels.{UserData, UserID}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.util.JwtUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class AuthService(tokenRepository: TokenRepository,
                  secretKey: String,
                  expPeriod: FiniteDuration = 2.hour)(implicit executionContext: ExecutionContext) {

  def createToken(user: UserData, remember: Boolean): Future[AuthToken] = {
    val refreshToken =
      JwtUtil.encode(RefreshTokenContent(user.id, new Date().getTime), secretKey, None)
    val time = LocalDateTime.now()
    tokenRepository.save(CreateRefreshToken(user.id, refreshToken, remember, time)).map(_.id) map {
      refreshTokenId =>
        val expTime = time.plusNanos(expPeriod.toNanos)
        val token =
          JwtUtil.encode(AuthTokenContent(refreshTokenId, user.id), secretKey, Option(expTime))
        AuthToken(token, refreshToken, expTime.atZone(ZoneId.systemDefault()))
    }
  }

  def revokeAllTokens(userID: UserID): Future[Unit] = tokenRepository.removeByUser(userID)

  def revokeToken(token: String): Future[Either[ApiError, Unit]] =
    JwtUtil.decode[AuthTokenContent](token, secretKey) match {
      case None    => Future.successful(Left(BadToken))
      case Some(t) => tokenRepository.removeById(t.refreshTokenId).map(Right(_))
    }

  def userId(token: String): Option[UserID] =
    JwtUtil.decode[AuthTokenContent](token, secretKey).map(_.userID)
}
