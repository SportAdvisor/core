package io.sportadvisor.core.user.token

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.UserID

/**
  * @author sss3 (Vladimir Alekseev)
  */
final case class ExpiredToken(userID: UserID,
                              token: String,
                              expireAt: LocalDateTime,
                              tokenType: TokenType)
