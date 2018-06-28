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

  final case class AuthToken(token: Token, refreshToken: Token, expireAt: ZonedDateTime) {
    def updateRefreshToken(r: Token): AuthToken = AuthToken(token, r, expireAt)
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

  final case class AuthTokenContent(userID: UserID)
  final case class RefreshTokenContent(userID: UserID, dateOfCreation: Long)

  final case class RefreshToken(userId: UserID,
                                token: Token,
                                remember: Boolean,
                                lastTouch: LocalDateTime)

  final case class ChangeMailToken(token: String, expireAt: LocalDateTime)
  final case class ResetPasswordToken(token: String, expireAt: LocalDateTime)

  final case class PasswordMismatch() extends ApiError("Password mismatch", None)

  implicit val tokenDecoder: Decoder[AuthTokenContent] = deriveDecoder
  implicit val tokenEncoder: Encoder[AuthTokenContent] = deriveEncoder
  implicit val refreshTokenDecoder: Decoder[RefreshTokenContent] = deriveDecoder
  implicit val refreshTokenEncoder: Encoder[RefreshTokenContent] = deriveEncoder

  implicit val authTokenEncoder: Encoder[AuthToken] = deriveEncoder
  implicit val authTokenDecoder: Decoder[AuthToken] = deriveDecoder
}
