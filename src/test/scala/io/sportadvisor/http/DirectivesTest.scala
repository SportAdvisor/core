package io.sportadvisor.http

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import io.sportadvisor.BaseTest
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.http.Response.EmptyResponse
import io.sportadvisor.http.Decoders._
import io.sportadvisor.http.HttpTestUtils._
import org.mockito.Mockito.when

/**
  * @author sss3 (Vladimir Alekseev)
  */
class DirectivesTest extends BaseTest {

  type Lang = (String, Double)

  "Select language" should {
    "Empty lang header return `en`" in new Context {
      Get("/lang").withHeaders(langHeader(None)) ~> selectLang ~> check {
        responseAs[String] shouldEqual "en"
      }
    }

    "Undefined lang header return `en`" in new Context {
      Get("/lang").withHeaders(langHeader(Some("fr-CH"), ("fr", 0.9))) ~> selectLang ~> check {
        responseAs[String] shouldEqual "en"
      }
    }

    "Valid lang header" when {
      "has en/ru" should {
        "ru-RU, ru;q=0.9, en;q=0.8 return `ru`" in new Context {
          Get("/lang").withHeaders(langHeader(Some("ru-RU"), ("ru", 0.9), ("en", 0.8))) ~> selectLang ~> check {
            responseAs[String] shouldEqual "ru"
          }
        }

        "en-US, en;q=0.9, ru;q=0.8 return `en`" in new Context {
          Get("/lang").withHeaders(langHeader(Some("en-US"), ("en", 0.9), ("ru", 0.8))) ~> selectLang ~> check {
            responseAs[String] shouldEqual "en"
          }
        }
      }

      "has en and another" should {
        "fr-CH, fr;q=0.9, en;q=0.8 return en" in new Context {
          Get("/lang").withHeaders(langHeader(Some("fr-CH"), ("fr", 0.9), ("en", 0.8))) ~> selectLang ~> check {
            responseAs[String] shouldEqual "en"
          }
        }
      }
    }
  }

  "authorize" should {
    "reject 401 if header not contains" in new Context {
      Get("/authorize") ~> authorizeRoute ~> check {
        r[EmptyResponse].code shouldBe 401
      }
    }

    "reject if token is expired" in new Context {
      val token = "token"
      when(authService.userId(token)).thenReturn(None)
      Get("/authorize").withHeaders(authHeader(token)) ~> authorizeRoute ~> check {
        r[EmptyResponse].code shouldBe 401
      }
    }

    "return user id if all success" in new Context {
      Get("/authorize").withHeaders(authHeader(1L)) ~> authorizeRoute ~> check {
        r[Long] shouldBe 1L
      }
    }
  }

  "check access" should {
    "reject 403" in new Context {
      Get("/access/2").withHeaders(authHeader(1L)) ~> checkAccessRoute ~> check {
        r[EmptyResponse].code shouldBe 403
      }
    }

    "return true" in new Context {
      Get("/access/1").withHeaders(authHeader(1L)) ~> checkAccessRoute ~> check {
        r[Boolean] shouldBe true
      }
    }
  }

  def langHeader(locale: Option[String], langs: Lang*): HttpHeader = {
    val language = langs.map(l => l._1 + ";q=" + l._2).mkString(", ")
    val headerValue = (locale.map(l => l + ", ") getOrElse "") + language
    HttpHeader.parse("Accept-Language", headerValue) match {
      case ParsingResult.Ok(h, _) => h
      case _                      => throw new IllegalArgumentException
    }
  }

  trait Context {
    implicit val authService: AuthService = mock[AuthService]

    val selectLang: Route = pathPrefix("lang") {
      pathEndOrSingleSlash {
        selectLanguage() { lang =>
          complete(lang)
        }
      }
    }

    val authorizeRoute: Route = pathPrefix("authorize") {
      pathEndOrSingleSlash {
        handleRejections(rejectionHandler) {
          authenticate.apply { userId =>
            complete(userId)
          }
        }
      }
    }

    val checkAccessRoute: Route = path("access" / LongNumber) { id =>
      handleRejections(rejectionHandler) {
        authenticate.apply { userId =>
          checkAccess(id, userId) {
            complete(id == userId)
          }
        }
      }
    }
  }

}
