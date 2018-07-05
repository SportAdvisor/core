package io.sportadvisor.http

import java.time.LocalDateTime

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import io.sportadvisor.core.user.UserModels.{AuthTokenContent, UserID}
import io.sportadvisor.util.JwtUtil

/**
  * @author sss3 (Vladimir Alekseev)
  */
object HttpTestUtils {

  def authHeader(refreshTokenId: Long, userId: UserID, exp: LocalDateTime, secretKey: String): HttpHeader = {
    val token = JwtUtil.encode(AuthTokenContent(refreshTokenId, userId), secretKey, Option(exp))
    HttpHeader.parse(authorizationHeader, token) match {
      case ParsingResult.Ok(h, _) => h
      case _                      => throw new IllegalArgumentException
    }
  }

  def authHeader(refreshTokenId: Long, userID: UserID, secretKey: String): HttpHeader =
    authHeader(refreshTokenId, userID, LocalDateTime.now().plusHours(1), secretKey)

}
