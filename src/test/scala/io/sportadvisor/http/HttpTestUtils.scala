package io.sportadvisor.http

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.user.UserModels.UserID
import org.mockito.Mockito.when

/**
  * @author sss3 (Vladimir Alekseev)
  */
object HttpTestUtils {

  def authHeader(userID: UserID)(implicit authService: AuthService): HttpHeader = {
    val token = "asd.asd.asd"
    when(authService.userId(token)).thenReturn(Option(userID))
    HttpHeader.parse(authorizationHeader, token) match {
      case ParsingResult.Ok(h, _) => h
      case ParsingResult.Error(e) => throw new IllegalArgumentException(e.formatPretty)
    }
  }

  def authHeader(token: String): HttpHeader = HttpHeader.parse(authorizationHeader, token) match {
    case ParsingResult.Ok(h, _) => h
    case ParsingResult.Error(e) => throw new IllegalArgumentException(e.formatPretty)
  }
}
