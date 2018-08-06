package io.sportadvisor.core.auth

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import cats.data.EitherT
import cats.instances.future._
import io.sportadvisor.core.auth.AuthModels._

import scala.concurrent.duration._
import io.sportadvisor.core.user.UserModels.{UserData, UserID}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.exception.Exceptions.TokenDoesntExist
import io.sportadvisor.util.JwtUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class AuthService(tokenRepository: AuthTokenRepository, secretKey: String, expPeriod: FiniteDuration = 2.hour)(
    implicit executionContext: ExecutionContext) {

  def createToken(user: UserData, remember: Boolean): Future[AuthToken] = {
    val refreshToken =
      JwtUtil.encode(RefreshTokenContent(user.id, new Date().getTime), secretKey, None)
    val time = LocalDateTime.now()
    tokenRepository.save(CreateRefreshToken(user.id, refreshToken, remember, time)).map { refreshTokenData =>
      createAuthToken(refreshTokenData)
    }
  }

  def revokeAllTokens(userID: UserID): Future[Unit] = tokenRepository.removeByUser(userID)

  def revokeToken(token: String): Future[Either[ApiError, Unit]] =
    EitherT
      .fromOption[Future](JwtUtil.decode[AuthTokenContent](token, secretKey), BadToken())
      .semiflatMap(t => tokenRepository.removeById(t.refreshTokenId))
      .value

  def userId(token: String): Option[UserID] =
    JwtUtil.decode[AuthTokenContent](token, secretKey).map(_.userID)

  def refreshAccessToken(token: String): Future[Either[ApiError, AuthToken]] = {
    tokenRepository.find(token).flatMap {
      case Some(refreshTokenData) =>
        tokenRepository
          .save(refreshTokenData.copy(lastTouch = LocalDateTime.now()))
          .map(createAuthToken)
          .map(Right(_))
      case None => Future.successful(Left(TokenDoesntExist("RefreshAuthToken")))
    }
  }

  private def createAuthToken(refreshTokenData: RefreshTokenData): AuthToken = {
    val time = LocalDateTime.now()
    val expTime = time.plusNanos(expPeriod.toNanos)
    val token =
      JwtUtil.encode(AuthTokenContent(refreshTokenData.id, refreshTokenData.userId),
                     secretKey,
                     Option(expTime))
    AuthToken(token, refreshTokenData.token, expTime.atZone(ZoneId.systemDefault()))
  }
}
