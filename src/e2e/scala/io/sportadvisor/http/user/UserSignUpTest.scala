package io.sportadvisor.http.user

import java.time.ZonedDateTime

import io.sportadvisor.BaseE2ETest
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.http.Response.{DataResponse, ErrorResponse, FormError, ObjectData}
import io.sportadvisor.http.route.user.{UserRoute, UserRouteValidators}

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserSignUpTest extends BaseE2ETest with UserMappings {

  "POST /api/users/sign-up with valid data" should "return OK code and JWT token" in {
    val response = post(
      req("sign-up"),
      s"""{"email": "validemail@mail.com", "password": "str0nGpass", "name":"NormalName", "EULA":true}""").asString
    response.code shouldBe 200

    val body = r[DataResponse[AuthToken, ObjectData[AuthToken]]](response.body)
    body.code shouldBe 200
    body.data.data.expireAt isBefore ZonedDateTime.now() shouldBe true

    val responseAboutMe = get(req(userId(body.data.data.token).toString), body.data.data.token).asString
    responseAboutMe.code shouldBe 200
  }

  "POST /api/users/sign-up with broken email" should "return error" in {
    val resp =
      post(req("sign-up"),
           s"""{"email": "testtest.com", "password": "test123Q", "name":"test", "EULA":true}""").asString
    resp.code shouldBe 400

    val body = r[ErrorResponse[FormError]](resp.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("email", UserRouteValidators.emailInvalid)) and have size 1)
  }

  "POST /api/users/sign-up with not valid password" should "return error" in {
    val resp =
      post(
        req("sign-up"),
        s"""{"email": "validemail@mail.com", "password": "test", "name":"NormalName", "EULA":true}""").asString
    resp.code shouldBe 400

    val body = r[ErrorResponse[FormError]](resp.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("password", UserRouteValidators.passwordIsWeak)) and have size 1)
  }

  "POST /api/users/sign-up with not valid name" should "return error" in {
    val resp =
      post(
        req("sign-up"),
        s"""{"email": "validemail@mail.com", "password": "str0nGpass", "name":" ", "EULA":true}""").asString
    resp.code shouldBe 400

    val body = r[ErrorResponse[FormError]](resp.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("name", UserRouteValidators.nameIsEmpty)) and have size 1)
  }

  "POST /api/users/sign-up with declined EULA" should "return error" in {
    val resp =
      post(
        req("sign-up"),
        s"""{"email": "validemail@mail.com", "password": "str0nGpass", "name":"NormalName", "EULA":false}""").asString
    resp.code shouldBe 400

    val body = r[ErrorResponse[FormError]](resp.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("EULA", UserRouteValidators.EUALIsRequired)) and have size 1)
  }

  "POST /api/users/sign-up with a dump user" should "return error" in {
    val resp =
      post(req("sign-up"), s"""{"email": "not valid", "password": "test", "name":" ", "EULA":false}""").asString
    resp.code shouldBe 400

    val body = r[ErrorResponse[FormError]](resp.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("EULA", UserRouteValidators.EUALIsRequired)) and
      contain(FormError("email", UserRouteValidators.emailInvalid)) and
      contain(FormError("password", UserRouteValidators.passwordIsWeak)) and
      contain(FormError("name", UserRouteValidators.nameIsEmpty)) and have size 4)
  }

  "POST /api/users/sign-up with valid data" should "return OK code and JWT token" in {
    val response1 = post(
      req("sign-up"),
      s"""{"email": "validemail2@mail.com", "password": "str0nGpass", "name":"NormalName", "EULA":true}""").asString
    response1.code shouldBe 200

    val response2 = post(
      req("sign-up"),
      s"""{"email": "validemail2@mail.com", "password": "str0nGpass", "name":"NormalName", "EULA":true}""").asString
    response2.code shouldBe 400

    val body = r[ErrorResponse[FormError]](response2.body)
    body.code shouldBe 400
    body.errors should contain(FormError("email", UserRoute.emailDuplication))
  }
}
