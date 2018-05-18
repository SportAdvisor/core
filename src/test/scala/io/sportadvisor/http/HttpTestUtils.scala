package io.sportadvisor.http

import java.time.LocalDateTime

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import io.sportadvisor.core.user.{AuthTokenContent, UserID}
import io.sportadvisor.util.JwtUtil

/**
  * @author sss3 (Vladimir Alekseev)
  */
object HttpTestUtils {

  def authHeader(userId: UserID, exp: LocalDateTime, secretKey: String): HttpHeader = {
    val token = JwtUtil.encode(AuthTokenContent(userId), secretKey, Option(exp))
    HttpHeader.parse(authorizationHeader, token) match {
      case ParsingResult.Ok(h, _) => h
      case _                      => throw new IllegalArgumentException
    }
  }

  def authHeader(userID: UserID, secretKey: String): HttpHeader =
    authHeader(userID, LocalDateTime.now().plusHours(1), secretKey)

}
