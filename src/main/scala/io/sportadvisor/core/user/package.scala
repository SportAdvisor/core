package io.sportadvisor.core

import java.time.LocalDateTime

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object user {

  type UserID = Long
  type Token = String

  final case class AuthToken(token: Token, refreshToken: Token, expireAt: LocalDateTime) {
    def updateRefreshToken(r: Token) : AuthToken = AuthToken(token, r, expireAt)
  }
  final case class RefreshToken(id: UserID, refreshToken: Token, expireAt: LocalDateTime, remember: Boolean) {
    def updateExp(exp: LocalDateTime) : RefreshToken = RefreshToken(id, refreshToken, exp, remember)
  }
  final case class UserData(id: UserID, email: String, password: String, name: String)

  final case class AuthTokenContent(userID: UserID, exp: Long)
  final case class RefreshTokenContent(userID: UserID, dateOfCreation: LocalDateTime)
}
