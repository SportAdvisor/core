package io.sportadvisor.http.user

import akka.http.scaladsl.model.StatusCodes.OK
import io.sportadvisor.BaseE2ETest
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.http.Response.{DataResponse, ErrorResponse, FormError, ObjectData}
import io.sportadvisor.http.route.user.{UserRoute, UserRouteValidators}

import scala.concurrent.duration._

class ChangePasswordTest extends BaseE2ETest with UserMappings with DefaultUsersData {

  "PUT /api/users/{id}/password with valid passwords" should "return OK code" in {
    val firsSignInResp = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "${user.password}", "remember":true}""").asString
    firsSignInResp.code shouldBe OK.intValue

    val token1 = r[DataResponse[AuthToken, ObjectData[AuthToken]]](firsSignInResp.body).data.data.token

    val newPassword = user.password + "1"
    val response = put(req(user.id + "/password"),
                       s"""{"password": "${user.password}", "newPassword": "$newPassword" }""",
                       token1).timeout(10.seconds).asString
    response.code shouldBe 200

    val signInResponse2 = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "$newPassword", "remember": true""").asString
    signInResponse2.code shouldBe 200

    val body = r[DataResponse[AuthToken, ObjectData[AuthToken]]](signInResponse2.body)
    body.code shouldBe 200

    val token2 = body.data.data.token
    val aboutMeResponse = get(req(userId(token2).toString), token2).asString
    aboutMeResponse.code shouldBe 200
  }

  "PUT /api/users/{id}/password with a wrong old password" should "return 400 code" in {
    val firsSignInResp = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "${user.password}", "remember":true}""").asString
    firsSignInResp.code shouldBe OK.intValue

    val token = r[DataResponse[AuthToken, ObjectData[AuthToken]]](firsSignInResp.body).data.data.token

    val newPassword = user.password + "1"
    val response = put(req(user.id + "/password"),
                       s"""{"password": "${user.password}1", "newPassword": "$newPassword" }""", token).asString
    response.code shouldBe 400

    val body = r[ErrorResponse[FormError]](response.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("password", UserRoute.passwordIncorrect)) and have size 1)
  }

  "PUT /api/users/{id}/password with a new weak password" should "return 400 code" in {
    val firsSignInResp = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "${user.password}", "remember":true}""").asString
    firsSignInResp.code shouldBe OK.intValue

    val token = r[DataResponse[AuthToken, ObjectData[AuthToken]]](firsSignInResp.body).data.data.token

    val newPassword = "weak"
    val response = put(req(user.id + "/password"),
                       s"""{"password": "${user.password}", "newPassword": "$newPassword" }""", token).asString
    response.code shouldBe 400

    val body = r[ErrorResponse[FormError]](response.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("password", UserRouteValidators.passwordIsWeak)) and have size 1)
  }

  "PUT /api/users/{id}/password to another user" should "return 403 code" in {
    val firsSignInResp = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "${user.password}", "remember":true}""").asString
    firsSignInResp.code shouldBe OK.intValue

    val token = r[DataResponse[AuthToken, ObjectData[AuthToken]]](firsSignInResp.body).data.data.token

    val newPassword = user.password + "1"
    val userId = user.id + 1
    val response = put(req(userId + "/password"),
                       s"""{"password": "${user.password}", "newPassword": "$newPassword" }""", token).asString
    response.code shouldBe 403

    val body = r[ErrorResponse[FormError]](response.body)
    body.code shouldBe 403
    body.errors should (contain(FormError("password", UserRoute.authError)) and have size 1)
  }

  "PUT /api/users/{id}/password unauthorized request" should "return 401 code" in {

    val newPassword = user.password + "1"
    val response = put(req(user.id + "/password"),
      s"""{"password": "${user.password}", "newPassword": "$newPassword" }""").timeout(10.seconds).asString
    response.code shouldBe 401

    val body = r[ErrorResponse[FormError]](response.body)
    body.code shouldBe 403

    body.errors should (contain(FormError("password", UserRoute.authError)) and have size 1)
  }

}
