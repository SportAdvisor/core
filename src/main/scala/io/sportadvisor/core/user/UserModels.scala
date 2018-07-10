package io.sportadvisor.core.user

import java.time.{LocalDateTime, ZonedDateTime}

import io.circe.{Decoder, Encoder}
import io.circe.java8.time._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.sportadvisor.exception.ApiError

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserModels {

  type UserID = Long
  type Token = String

  sealed trait User {
    def email: String
    def password: String
  }

  final case class CreateUser(email: String, password: String, name: String) extends User
  final case class UserData(id: UserID,
                            email: String,
                            password: String,
                            name: String,
                            language: Option[String])
      extends User {
    def lang: String = language.fold("ru")(l => l)
  }

  sealed trait RefreshToken {
    def userId: Long
    def token: Token
    def remember: Boolean
    def lastTouch: LocalDateTime
  }

  final case class AuthToken(token: Token, refreshToken: Token, expireAt: ZonedDateTime)

  final case class AuthTokenContent(refreshTokenId: Long, userID: UserID)
  final case class RefreshTokenContent(userID: UserID, dateOfCreation: Long)

  final case class RefreshTokenData(id: Long,
                                    userId: UserID,
                                    token: Token,
                                    remember: Boolean,
                                    lastTouch: LocalDateTime)
      extends RefreshToken

  final case class CreateRefreshToken(userId: UserID,
                                      token: Token,
                                      remember: Boolean,
                                      lastTouch: LocalDateTime)
      extends RefreshToken

  final case class ChangeMailToken(userID: UserID, token: String, expireAt: LocalDateTime)
  final case class ResetPasswordToken(userId: UserID, token: String, expireAt: LocalDateTime)

  final case class PasswordMismatch() extends ApiError("Password mismatch", None)

  implicit val tokenDecoder: Decoder[AuthTokenContent] = deriveDecoder
  implicit val tokenEncoder: Encoder[AuthTokenContent] = deriveEncoder
  implicit val refreshTokenDecoder: Decoder[RefreshTokenContent] = deriveDecoder
  implicit val refreshTokenEncoder: Encoder[RefreshTokenContent] = deriveEncoder

  implicit val authTokenEncoder: Encoder[AuthToken] = deriveEncoder
  implicit val authTokenDecoder: Decoder[AuthToken] = deriveDecoder
}
