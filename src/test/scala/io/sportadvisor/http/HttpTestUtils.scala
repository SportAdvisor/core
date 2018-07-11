package io.sportadvisor.http

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.user.UserModels.UserID
import org.mockito.Matchers.anyString
import org.mockito.Mockito.when

import scala.util.Random

/**
  * @author sss3 (Vladimir Alekseev)
  */
object HttpTestUtils {

  private val headerLen = 10

  def authHeader(userID: UserID)(implicit authService: AuthService): HttpHeader = {
    when(authService.userId(anyString)).thenReturn(Option(userID))
    HttpHeader.parse(authorizationHeader, Random.nextString(headerLen)) match {
      case ParsingResult.Ok(h, _) => h
      case ParsingResult.Error(e) => throw new IllegalArgumentException(e.formatPretty)
    }
  }

  def authHeader(token: String): HttpHeader = HttpHeader.parse(authorizationHeader, token) match {
    case ParsingResult.Ok(h, _) => h
    case ParsingResult.Error(e) => throw new IllegalArgumentException(e.formatPretty)
  }
}
