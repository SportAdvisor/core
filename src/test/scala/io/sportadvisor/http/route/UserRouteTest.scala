package io.sportadvisor.http.route

import java.time.LocalDateTime

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import io.sportadvisor.BaseTest
import io.sportadvisor.core.user.{AuthToken, UserData, UserID, UserService}
import io.sportadvisor.exception._
import io.sportadvisor.http.Response.{DataResponse, EmptyResponse, ErrorResponse, FailResponse, FormError, ObjectData}
import io.sportadvisor.http.I18nStub
import io.sportadvisor.http.json._
import io.sportadvisor.http.Decoders._
import io.sportadvisor.http.json.Codecs._
import io.sportadvisor.http.route.user.UserRoute
import io.sportadvisor.http.HttpTestUtils._
import org.mockito.Mockito._

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserRouteTest extends BaseTest {

  "UserRoute" when {
    "POST /users/sign-up" should {
      "return 200 and token if sign up successful" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":true}""")
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
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"", "EULA":true}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("name", "Name is required")) and have size 1)
        }
      }

      "return 400 if email invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "testtest.com", "password": "test123Q", "name":"test", "EULA":true}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("email", "Email is invalid")) and have size 1)
        }
      }

      "return 400 if password invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123", "name":"test", "EULA":true}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("password", "Your password must be at least 8 characters long, and include at least one lowercase letter, one uppercase letter, and a number")) and have size 1)
        }
      }

      "return 400 if email is exists" in new Context {
        when(userService.signUp("test@test.com", "test123Q", "test"))
          .thenReturn(Future.successful(Left(DuplicateException())))
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":true}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("email", "Email address is already registered")) and have size 1)
        }
      }

      "return 400 if email and password invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "testtest.com", "password": "test123", "name":"test", "EULA":true}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("email", "Email is invalid")) and contain(FormError("password", "Your password must be at least 8 characters long, and include at least one lowercase letter, one uppercase letter, and a number")) and have size 2)
        }
      }

      "return 500 if was internal error" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":true}""")
        when(userService.signUp("test@test.com", "test123Q", "test")).thenThrow(new RuntimeException)
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code should be(500)
        }
      }

      "return 400 if user not agree EULA" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":false}""")
        Post("/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code should be(400)
          resp.errors should (contain(FormError("EULA", "You must accept the end-user license agreement")) and have size 1)
        }
      }
    }

    "POST /users/sign-in" should {
      "return 200 and token if auth successful" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "test@test.com", "password": "test123Q",
             |"remember":true}""".stripMargin)
        when(userService.signIn("test@test.com", "test123Q", remember = true))
          .thenReturn(Future.successful(Some(AuthToken("t", "t", LocalDateTime.now()))))
        Post("/users/sign-in", requestEntity) ~> userRoute ~> check {
          val resp = r[DataResponse[AuthToken, ObjectData[AuthToken]]]
          resp.code should be(200)
          resp.data.data.token should not be null
        }
      }

      "return 400 and empty response if auth invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "test@test.com", "password": "test123Q",
             |"remember":true}""".stripMargin)
        when(userService.signIn("test@test.com", "test123Q", remember = true))
          .thenReturn(Future.successful(None))
        Post("/users/sign-in", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code should be(400)
        }
      }

      "return 500 if was internal error" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "test@test.com", "password": "test123Q", "name":"test",
             |"remember":true}""".stripMargin)
        when(userService.signIn("test@test.com", "test123Q", remember = true)).thenThrow(new RuntimeException)
        Post("/users/sign-in", requestEntity) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code should be(500)
        }
      }
    }

    "PUT /users/{id}/email" should {
      "return 400 if email is exists" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Left(DuplicateException())))
        Put(s"/users/$testUserId/email", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FormError("email", "Email address is already registered")) and have size 1)
        }
      }

      "return 400 if email is invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "testtest.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Left(DuplicateException())))
        Put(s"/users/$testUserId/email", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FormError("email", "Email is invalid")) and have size 1)
        }
      }

      "return 401 if user unauthorized" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "testtest.com", "redirectUrl":"test"}""")
        Put(s"/users/$testUserId/email", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 401
          response.status.intValue shouldBe StatusCodes.Unauthorized.intValue
        }
      }

      "return 403" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        Put(s"/users/1$testUserId/email", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 403
        }
      }

      "return 200 if all success" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Right()))
        Put(s"/users/$testUserId/email", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
        }
      }

      "return 500 if user not found" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Left(UserNotFound(testUserId))))
        Put(s"/users/$testUserId/email", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code shouldBe 500
          resp.message.isDefined shouldBe true
          resp.message.get shouldBe "Authorization error. Relogin please"
        }
      }
    }

    "POST /users/email" should {
      "return 200 if change success" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"token":"test"}""")
        when(userService.confirmEmail("test")).thenReturn(Future.successful(true))
        Post("/users/email-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
        }
      }

      "return 400 if token invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"token":"test"}""")
        when(userService.confirmEmail("test")).thenReturn(Future.successful(false))
        Post("/users/email-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 400
        }
      }
    }

    "PUT /users/{id}" should {
      "return 400 if name is empty" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"name": " ", "language":"ru"}""")

        Put(s"/users/$testUserId", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FormError("name", "Name is required")) and have size 1)
        }
      }

      "return 400 if language not supported" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"name": "test", "language":"ch"}""")

        Put(s"/users/$testUserId", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FormError("language", "Selected language not supported")) and have size 1)
        }
      }

      "return 403 if initiate change another user" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"name": "", "language":"ch"}""")

        Put(s"/users/2$testUserId", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 403
        }
      }

      "return 200 if language is empty" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"name": "test", "language": null}""")
        when(userService.changeAccount(testUserId, "test", None))
            .thenReturn(Future.successful(Some(UserData(testUserId, "t@t.t", "t", "test", None))))

        Put(s"/users/$testUserId", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
          val location = header("Location")
          location.isDefined shouldBe true
        }
      }

      "return 500 if return None" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
          s"""{"name": "test", "language": null}""")
        when(userService.changeAccount(testUserId, "test", None))
          .thenReturn(Future.successful(None))

        Put(s"/users/$testUserId", requestEntity).withHeaders(authHeader(testUserId, testSecret)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 500
        }
      }
    }
  }

  trait Context {
    val testSecret: String = "secret"
    val testUserId: UserID = 1L
    val userService: UserService = mock[UserService]
    when(userService.secret).thenReturn(testSecret)

    val userRoute: Route = (new UserRoute(userService) with I18nStub).route
  }
}
