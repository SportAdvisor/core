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

  private[auth] final case class AuthTokenContent(refreshTokenId: Long, userID: UserID)
  private[auth] final case class RefreshTokenContent(userID: UserID, dateOfCreation: Long)

  private[auth] final case class RefreshTokenData(id: Long,
                                                  userId: UserID,
                                                  token: Token,
                                                  remember: Boolean,
                                                  lastTouch: LocalDateTime)
      extends RefreshToken

  private[auth] final case class CreateRefreshToken(userId: UserID,
                                                    token: Token,
                                                    remember: Boolean,
                                                    lastTouch: LocalDateTime)
      extends RefreshToken

  final case object BadToken extends ApiError("Token is expired", None)

  private[auth] implicit val tokenDecoder: Decoder[AuthTokenContent] = deriveDecoder
  private[auth] implicit val tokenEncoder: Encoder[AuthTokenContent] = deriveEncoder
  private[auth] implicit val refreshTokenDecoder: Decoder[RefreshTokenContent] = deriveDecoder
  private[auth] implicit val refreshTokenEncoder: Encoder[RefreshTokenContent] = deriveEncoder

  implicit val authTokenEncoder: Encoder[AuthToken] = deriveEncoder
  implicit val authTokenDecoder: Decoder[AuthToken] = deriveDecoder
}
