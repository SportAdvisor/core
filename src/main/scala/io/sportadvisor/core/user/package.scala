package io.sportadvisor.core

import java.time.LocalDateTime

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object user {

  type UserID = Long
  type Token = String

  sealed trait User {
    def email: String
    def password: String
  }

  final case class AuthToken(token: Token, refreshToken: Token, expireAt: LocalDateTime) {
    def updateRefreshToken(r: Token): AuthToken = AuthToken(token, r, expireAt)
  }

  final case class CreateUser(email: String, password: String, name: String) extends User
  final case class UserData(id: UserID, email: String, password: String, name: String) extends User

  final case class AuthTokenContent(userID: UserID)
  final case class RefreshTokenContent(userID: UserID, dateOfCreation: Long)

  final case class RefreshToken(userId: UserID,
                                token: Token,
                                remember: Boolean,
                                lastTouch: LocalDateTime)

  final class UserAlreadyExists extends Exception
}
