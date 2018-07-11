package io.sportadvisor.core.auth

import java.time.{LocalDateTime, ZonedDateTime}

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.java8.time._

import io.sportadvisor.core.user.UserModels.UserID
import io.sportadvisor.exception.ApiError

/**
  * @author sss3 (Vladimir Alekseev)
  */
object AuthModels {

  type Token = String

  sealed trait RefreshToken {
    def userId: Long
    def token: Token
    def remember: Boolean
    def lastTouch: LocalDateTime
  }

  final case class AuthToken(token: Token, refreshToken: Token, expireAt: ZonedDateTime)

  final private[auth] case class AuthTokenContent(refreshTokenId: Long, userID: UserID)
  final private[auth] case class RefreshTokenContent(userID: UserID, dateOfCreation: Long)

  final private[auth] case class RefreshTokenData(id: Long,
                                                  userId: UserID,
                                                  token: Token,
                                                  remember: Boolean,
                                                  lastTouch: LocalDateTime)
      extends RefreshToken

  final private[auth] case class CreateRefreshToken(userId: UserID,
                                                    token: Token,
                                                    remember: Boolean,
                                                    lastTouch: LocalDateTime)
      extends RefreshToken

  final case object BadToken extends ApiError("Token is expired", None)

  implicit private[auth] val tokenDecoder: Decoder[AuthTokenContent] = deriveDecoder
  implicit private[auth] val tokenEncoder: Encoder[AuthTokenContent] = deriveEncoder
  implicit private[auth] val refreshTokenDecoder: Decoder[RefreshTokenContent] = deriveDecoder
  implicit private[auth] val refreshTokenEncoder: Encoder[RefreshTokenContent] = deriveEncoder

  implicit val authTokenEncoder: Encoder[AuthToken] = deriveEncoder
  implicit val authTokenDecoder: Decoder[AuthToken] = deriveDecoder
}
