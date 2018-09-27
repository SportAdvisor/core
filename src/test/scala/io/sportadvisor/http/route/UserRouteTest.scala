package io.sportadvisor.http.route

import java.time.ZonedDateTime

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import io.sportadvisor.BaseTest
import io.sportadvisor.core.auth.AuthModels._
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.user.UserModels.{PasswordMismatch, UserData, UserID}
import io.sportadvisor.core.user.UserService
import io.sportadvisor.exception.ApiError
import io.sportadvisor.exception.Exceptions._
import io.sportadvisor.exception.Exceptions.{DuplicateException, ResourceNotFound}
import io.sportadvisor.http.Codecs._
import io.sportadvisor.http.HttpTestUtils._
import io.sportadvisor.http.{exceptionHandler, rejectionHandler, I18nStub}
import io.sportadvisor.http.Response._
import io.sportadvisor.http.common.CommonValidations
import io.sportadvisor.http.route.user.UserRouteProtocol.UserView
import io.sportadvisor.http.route.user.{UserRoute, UserRouteValidators}
import io.sportadvisor.util.I18nService
import org.mockito.Mockito._

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserRouteTest extends BaseTest {

  "UserRoute" when {
    "POST /api/users/sign-up" should {
      "return 200 and token if sign up successful" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":true}""")
        when(userService.signUp("test@test.com", "test123Q", "test"))
          .thenReturn(Future.successful(Right(AuthToken("", "", ZonedDateTime.now()))))
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[DataResponse[AuthToken, ObjectData[AuthToken]]]
          resp.code should be(200)
          val data = resp.data.data
          data.token should not be null
          data.refreshToken should not be null
          data.expireAt should not be null

          resp.data.links should be(None)
          status.isSuccess should be(true)
        }
      }

      "return 400 if name invalid" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test123Q", "name":"", "EULA":true}""")
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code should be(400)
          resp.errors should (contain(FieldFormError("name", CommonValidations.requiredField)) and have size 1)
        }
      }

      "return 400 if email invalid" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "testtest.com", "password": "test123Q", "name":"test", "EULA":true}""")
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code should be(400)
          resp.errors should (contain(FieldFormError("email", UserRouteValidators.emailInvalid)) and have size 1)
        }
      }

      "return 400 if password invalid" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test123", "name":"test", "EULA":true}""")
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code should be(400)
          resp.errors should (contain(FieldFormError("password", UserRouteValidators.passwordIsWeak)) and have size 1)
        }
      }

      "return 400 if password contains space symbol" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "Pass w0rd", "name":"test", "EULA":true}""")
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code should be(400)
          resp.errors should (contain(FieldFormError("password", UserRouteValidators.passwordIsWeak)) and have size 1)
        }
      }

      "return 200 and token if sign up successful and password contains special characters" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test!@123Q", "name":"test", "EULA":true}""")
        when(userService.signUp("test@test.com", "test!@123Q", "test"))
          .thenReturn(Future.successful(Right(AuthToken("", "", ZonedDateTime.now()))))
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[DataResponse[AuthToken, ObjectData[AuthToken]]]
          resp.code should be(200)
          val data = resp.data.data
          data.token should not be null
          data.refreshToken should not be null
          data.expireAt should not be null

          resp.data.links should be(None)
          status.isSuccess should be(true)
        }
      }

      "return 400 if email is exists" in new Context {
        when(userService.signUp("test@test.com", "test123Q", "test"))
          .thenReturn(Future.successful(Left(DuplicateException())))
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":true}""")
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code should be(400)
          resp.errors should (contain(FieldFormError("email", UserRoute.emailDuplication)) and have size 1)
        }
      }

      "return 400 if email and password invalid" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "testtest.com", "password": "test123", "name":"test", "EULA":true}""")
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code should be(400)
          resp.errors should (contain(FieldFormError("email", UserRouteValidators.emailInvalid)) and contain(
            FieldFormError("password", UserRouteValidators.passwordIsWeak)) and have size 2)
        }
      }

      "return 500 if was internal error" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":true}""")
        when(userService.signUp("test@test.com", "test123Q", "test"))
          .thenThrow(new RuntimeException)
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code should be(500)
        }
      }

      "return 400 if user not agree EULA" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test123Q", "name":"test", "EULA":false}""")
        Post("/api/users/sign-up", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code should be(400)
          resp.errors should (contain(FieldFormError("EULA", UserRouteValidators.EUALIsRequired)) and have size 1)
        }
      }
    }

    "POST /api/users/sign-in" should {
      "return 200 and token if auth successful" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`,
                                       s"""{"email": "test@test.com", "password": "test123Q",
             |"remember":true}""".stripMargin)
        when(userService.signIn("test@test.com", "test123Q", remember = true))
          .thenReturn(Future.successful(Some(AuthToken("t", "t", ZonedDateTime.now()))))
        Post("/api/users/sign-in", requestEntity) ~> userRoute ~> check {
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
        Post("/api/users/sign-in", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code should be(400)
        }
      }

      "return 500 if was internal error" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`,
                     s"""{"email": "test@test.com", "password": "test123Q", "name":"test",
             |"remember":true}""".stripMargin)
        when(userService.signIn("test@test.com", "test123Q", remember = true))
          .thenThrow(new RuntimeException)
        Post("/api/users/sign-in", requestEntity) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code should be(500)
        }
      }
    }

    "POST /api/users/sign-in/refresh" should {
      "return 200 and token if refresh was succesful" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"refreshToken": "token"}""")
        when(authService.refreshAccessToken("token"))
          .thenReturn(Future.successful(Right(AuthToken("token", "token", ZonedDateTime.now()))))
        Post("/api/users/sign-in/refresh", requestEntity) ~> userRoute ~> check {
          val resp = r[DataResponse[AuthToken, ObjectData[AuthToken]]]
          resp.code should be(200)
          val data = resp.data.data
          data.token should not be null
          data.refreshToken should not be null
          data.expireAt should not be null

          status.isSuccess should be(true)
        }
      }

      "return 400 if was error" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"refreshToken": "token"}""")
        when(authService.refreshAccessToken("token"))
          .thenReturn(Future.successful(Left(TokenExpired("RefreshAuthToken"))))
        Post("/api/users/sign-in/refresh", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code should be(400)
        }
      }

      "return 500 if was internal error" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"refreshToken": "token"}""")
        when(authService.refreshAccessToken("token"))
          .thenThrow(new RuntimeException)
        Post("/api/users/sign-in/refresh", requestEntity) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code should be(500)
        }
      }
    }

    "PUT /api/users/{id}/email" should {
      "return 400 if email is exists" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Left(DuplicateException())))
        Put(s"/api/users/$testUserId/email", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FieldFormError("email", UserRoute.emailDuplication)) and have size 1)
        }
      }

      "return 400 if email is invalid" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "testtest.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Left(DuplicateException())))
        Put(s"/api/users/$testUserId/email", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FieldFormError("email", UserRouteValidators.emailInvalid)) and have size 1)
        }
      }

      "return 401 if user unauthorized" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "testtest.com", "redirectUrl":"test"}""")
        Put(s"/api/users/$testUserId/email", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 401
          response.status.intValue shouldBe StatusCodes.Unauthorized.intValue
        }
      }

      "return 403" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        Put(s"/api/users/1$testUserId/email", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 403
        }
      }

      "return 200 if all success" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Right(())))
        Put(s"/api/users/$testUserId/email", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
        }
      }

      "return 500 if user not found" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.changeEmail(testUserId, "test@test.com", "test"))
          .thenReturn(Future.successful(Left(ResourceNotFound(testUserId))))
        Put(s"/api/users/$testUserId/email", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[FailResponse]
          resp.code shouldBe 500
          resp.message.isDefined shouldBe true
          resp.message.get shouldBe UserRoute.authError
        }
      }
    }

    "POST /api/users/email" should {
      "return 200 if change success" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"token":"test"}""")
        when(userService.confirmEmail("test")).thenReturn(Future.successful(true))
        Post("/api/users/email-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
        }
      }

      "return 400 if token invalid" in new Context {
        val requestEntity = HttpEntity(MediaTypes.`application/json`, s"""{"token":"test"}""")
        when(userService.confirmEmail("test")).thenReturn(Future.successful(false))
        Post("/api/users/email-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 400
        }
      }
    }

    "GET /api/users/me" should {
      "return 400 if have not token" in new Context {
        Get("/api/users/me") ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 401
        }
      }

      "return redirect" in new Context {
        Get("/api/users/me").withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          response.status.intValue shouldBe 303
          val header = response.getHeader("Location")
          header.isPresent shouldBe true
          header.get().value() shouldBe "/api/users/1"
        }
      }
    }

    "GET /api/users/{id}" should {
      "return 400 if have not token" in new Context {
        Get(s"/api/users/$testUserId") ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 401
        }
      }

      "return 403 if user hasnt access" in new Context {
        Get(s"/api/users/2$testUserId")
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 403
        }
      }

      "return 404 if user not found" in new Context {
        when(userService.findUser(testUserId))
          .thenReturn(Future.successful(None))
        Get(s"/api/users/$testUserId")
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 404
        }
      }

      "return 200 and user data" in new Context {
        when(userService.findUser(testUserId))
          .thenReturn(Future.successful(
            Option(UserData(testUserId, "testemail", "testpassword", "testname", Some("ru")))))
        Get(s"/api/users/$testUserId")
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[DataResponse[UserView, ObjectData[UserView]]]
          resp.code shouldBe 200
          val data = resp.data
          data.links.isDefined shouldBe true
          data.links.get.self.href shouldBe "/api/users/1"
          data.data.id shouldBe 1L
          data.data.email shouldBe "testemail"
          data.data.name shouldBe "testname"
          data.data.language.isDefined shouldBe true
          data.data.language.get shouldBe "ru"

          println(response)
        }
      }
    }

    "PUT /api/users/{id}" should {
      "return 400 if name is empty" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"name": "", "language":"ru"}""")

        Put(s"/api/users/$testUserId", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FieldFormError("name", CommonValidations.requiredField)) and have size 1)
        }
      }

      "return 400 if language not supported" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"name": "test", "language":"ch"}""")

        Put(s"/api/users/$testUserId", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FieldFormError("language", UserRouteValidators.langNotSupported)) and have size 1)
        }
      }

      "return 403 if initiate change another user" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"name": "", "language":"ch"}""")

        Put(s"/api/users/2$testUserId", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 403
        }
      }

      "return 200 if language is empty" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"name": "test", "language": null}""")
        when(userService.changeAccount(testUserId, "test", None))
          .thenReturn(Future.successful(Some(UserData(testUserId, "t@t.t", "t", "test", None))))

        Put(s"/api/users/$testUserId", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
          val location = header("Location")
          location.isDefined shouldBe true
        }
      }

      "return 500 if return None" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"name": "test", "language": null}""")
        when(userService.changeAccount(testUserId, "test", None))
          .thenReturn(Future.successful(None))

        Put(s"/api/users/$testUserId", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 500
        }
      }
    }

    "PUT /api/users/{id}/password" should {
      "return 400 if new password invalid" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"password": "asd", "newPassword":"easypass"}""")

        Put(s"/api/users/$testUserId/password", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FieldFormError("newPassword", UserRouteValidators.passwordIsWeak)) and have size 1)
        }
      }

      "return 400 if old password mismatch" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"password": "asd", "newPassword":"str0nGpass"}""")
        when(userService.changePassword(testUserId, "asd", "str0nGpass"))
          .thenReturn(Future.successful(Left(PasswordMismatch())))
        Put(s"/api/users/$testUserId/password", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe 400
          resp.errors should (contain(FieldFormError("password", UserRoute.passwordIncorrect)) and have size 1)
        }
      }

      "return 200 if success" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"password": "asd", "newPassword":"str0nGpass"}""")
        when(userService.changePassword(testUserId, "asd", "str0nGpass"))
          .thenReturn(Future.successful(Right(())))
        Put(s"/api/users/$testUserId/password", requestEntity)
          .withHeaders(authHeader(testUserId)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
        }
      }
    }

    "POST /api/users/sign-out" should {
      "return 200 success" in new Context {
        val token = "authToken"
        when(userService.logout(token)).thenReturn(Future.successful(Right(())))
        Post(s"/api/users/sign-out", token).withHeaders(authHeader(token)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 200
        }
      }

      "return 401 if token was expired" in new Context {
        val token = "authToken"
        when(userService.logout(token)).thenReturn(Future.successful(Left(BadToken())))
        Post(s"/api/users/sign-out", token)
          .withHeaders(authHeader(token)) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 401
        }
      }

      "return 401 if token wasnt transferred to post" in new Context {
        Post(s"/api/users/sign-out") ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 401
        }
      }
    }

    "POST /api/users/reset-password" should {
      "return 200 anyway" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.resetPassword(testEmail, "test")).thenReturn(Future.successful(Right(())))
        Post("/api/users/reset-password", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code should be(200)
        }
      }

      "return 500 if save failed" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"email": "test@test.com", "redirectUrl":"test"}""")
        when(userService.resetPassword(testEmail, "test"))
          .thenReturn(Future.failed[Either[ApiError, Unit]](new RuntimeException))
        Post("/api/users/reset-password", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 500
        }
      }
    }

    "POST /api/users/password-confirm" should {
      "return 200 if confirmation succeed" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"token": "token", "password":"P1sswwqard"}""")
        when(userService.confirmResetPassword("token", "P1sswwqard"))
          .thenReturn(Future.successful(Right(())))
        Post("/api/users/password-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          println(resp)
          resp.code should be(200)
        }
      }

      "return 400 if password is too weak" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"token": "token", "password":"test"}""")
        when(userService.confirmResetPassword("token", "test"))
          .thenReturn(Future.successful(Right(())))
        Post("/api/users/password-confirm", requestEntity) ~> userRoute ~> check {
          println(response)
          val resp = r[ErrorResponse[FieldFormError]]
          resp.errors should (contain(FieldFormError("password", UserRouteValidators.passwordIsWeak)) and have size 1)
          resp.code shouldBe 400
        }
      }

      "return 200 if confirmation failed (token doesn't exist)" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"token": "token", "password":"P1sswwqard"}""")
        when(userService.confirmResetPassword("token", "P1sswwqard"))
          .thenReturn(Future.successful(Left(TokenDoesNotExist("reset password"))))
        Post("/api/users/password-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          println(response)
          resp.errors should (contain(FieldFormError("token", UserRoute.resetPwdExpired)) and have size 1)
          resp.code shouldBe 400
        }
      }

      "return 200 if confirmation failed (token expired)" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"token": "token", "password":"P1sswwqard"}""")
        when(userService.confirmResetPassword("token", "P1sswwqard"))
          .thenReturn(Future.successful(Left(TokenExpired("reset password"))))
        Post("/api/users/password-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]] // Your password reset link has expired. Please initiate a new password reset
          resp.errors should (contain(FieldFormError("token", UserRoute.resetPwdExpired)) and have size 1)
          resp.code shouldBe 400
        }
      }

      "return 200 if confirmation failed (User Not Found)" in new Context {
        val requestEntity =
          HttpEntity(MediaTypes.`application/json`, s"""{"token": "token", "password":"P1sswwqard"}""")
        when(userService.confirmResetPassword("token", "P1sswwqard"))
          .thenReturn(Future.successful(Left(ResourceNotFound(-1L))))
        Post("/api/users/password-confirm", requestEntity) ~> userRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe 500
        }
      }
    }
  }

  trait Context {
    val testSecret: String = "secret"
    val testUserId: UserID = 1L
    val testRefreshTokenId: Long = 1L
    val testEmail: String = "test@test.com"
    val userService: UserService = mock[UserService]
    implicit val authService: AuthService = mock[AuthService]
    implicit val i18n: I18nService = I18nStub
    when(userService.secret).thenReturn(testSecret)

    val userRoute: Route = handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        pathPrefix("api") {
          new UserRoute(userService).route
        }
      }
    }
  }
}
