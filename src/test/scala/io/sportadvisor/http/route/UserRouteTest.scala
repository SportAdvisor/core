package io.sportadvisor.http.route

import java.time.LocalDateTime

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Route
import io.sportadvisor.BaseTest
import io.sportadvisor.core.user.{AuthToken, UserAlreadyExists, UserService}
import io.sportadvisor.http.Response.{DataResponse, ErrorResponse, FailResponse, FormError, ObjectData}
import io.sportadvisor.http.json._
import io.sportadvisor.http.json.Codecs._
import org.mockito.Mockito._

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserRouteTest extends BaseTest {

  "UserRoute" when {
    "POST /users/sign-up" should {
      "return 200 and token if sign up successful" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"test"}""")
        when(userService.signUp("test@test.com", "test123Q", "test"))
          .thenReturn(Future.successful(Right(AuthToken("", "", LocalDateTime.now()))))
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[DataResponse[AuthToken, ObjectData[AuthToken]]]
          resp.code should be(200)
          val data = resp.data.data
          data.token should not be null
          data.refreshToken should not be null
          data.expireAt should not be null

          resp.data._links should be(None)
          status.isSuccess should be(true)
        }
      }

      "return 400 if name invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":""}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("name", "0x1")) and have size 1)
        }
      }

      "return 400 if email invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "testtest.com", "password": "test123Q", "name":"test"}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("email", "0x1")) and have size 1)
        }
      }

      "return 400 if password invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123", "name":"test"}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("password", "0x1")) and have size 1)
        }
      }

      "return 400 if email is exists" in new Context {
        when(userService.signUp("test@test.com", "test123Q", "test"))
          .thenReturn(Future.successful(Left(new UserAlreadyExists)))
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"test"}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("email", "0x2")) and have size 1)
        }
      }

      "return 400 if email and password invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "testtest.com", "password": "test123", "name":"test"}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("email", "0x1")) and contain(FormError("password", "0x1")) and have size 2)
        }
      }

      "return 500 if was internal error" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"test"}""")
        when(userService.signUp("test@test.com", "test123Q", "test")).thenThrow(new RuntimeException)
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code should be(500)
        }
      }
    }
  }

  trait Context {
    val userService: UserService = mock[UserService]
    val userRoute: Route = new UserRoute(userService).route
  }
}
