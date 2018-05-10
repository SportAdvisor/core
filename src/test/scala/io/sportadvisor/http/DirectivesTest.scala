package io.sportadvisor.http

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import io.sportadvisor.BaseTest

/**
  * @author sss3 (Vladimir Alekseev)
  */
class DirectivesTest extends BaseTest {

  type Lang = (String, Double)

  "Select language" should {
    "Empty lang header return `en`" in new Context {
      Get("/lang").withHeaders(langHeader(None)) ~> route ~> check {
        responseAs[String] shouldEqual "en"
      }
    }

    "Undefined lang header return `en`" in new Context {
      Get("/lang").withHeaders(langHeader(Some("fr-CH"), ("fr", 0.9))) ~> route ~> check {
        responseAs[String] shouldEqual "en"
      }
    }

    "Valid lang header" when {
      "has en/ru" should {
        "ru-RU, ru;q=0.9, en;q=0.8 return `ru`" in new Context {
          Get("/lang").withHeaders(langHeader(Some("ru-RU"), ("ru", 0.9), ("en", 0.8))) ~> route ~> check {
            responseAs[String] shouldEqual "ru"
          }
        }

        "en-US, en;q=0.9, ru;q=0.8 return `en`" in new Context {
          Get("/lang").withHeaders(langHeader(Some("en-US"), ("en", 0.9), ("ru", 0.8))) ~> route ~> check {
            responseAs[String] shouldEqual "en"
          }
        }
      }

      "has en and another" should {
        "fr-CH, fr;q=0.9, en;q=0.8 return en" in new Context {
          Get("/lang").withHeaders(langHeader(Some("fr-CH"), ("fr", 0.9), ("en", 0.8))) ~> route ~> check {
            responseAs[String] shouldEqual "en"
          }
        }
      }
    }
  }

  def langHeader(locale: Option[String], langs: Lang*): HttpHeader = {
    val language = langs.map(l => l._1 + ";q=" + l._2).mkString(", ")
    val headerValue = (locale.map(l => l + ", ") getOrElse "") + language
    HttpHeader.parse("Accept-Language", headerValue) match {
      case ParsingResult.Ok(h, _) => h
      case _ => throw new IllegalArgumentException
    }
  }

  trait Context {
    val route: Route = pathPrefix("lang") {
      pathEndOrSingleSlash {
        selectLanguage() { lang =>
          complete(lang)
        }
      }
    }
  }

}
