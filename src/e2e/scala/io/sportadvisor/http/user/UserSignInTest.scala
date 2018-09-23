package io.sportadvisor.http.user

import java.time.ZonedDateTime

import io.sportadvisor.BaseE2ETest
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.http.Response.{DataResponse, ObjectData}
import io.sportadvisor.http.route.user.UserRouteProtocol.UserView

class UserSignInTest extends BaseE2ETest with UserMappings with DefaultUsersData {

  "POST /api/users/sign-in with valid data (remember = true)" should "return 200 code and an auth token" in {
    val response = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "${user.password}", "remember":true}""").asString
    response.code shouldBe 200

    val body = r[DataResponse[AuthToken, ObjectData[AuthToken]]](response.body)
    body.code shouldBe 200

    body.data.data.expireAt isAfter ZonedDateTime.now() shouldBe true
    val token = body.data.data.token
    val aboutMeResponse = get(req(userId(token).toString), token).asString
    aboutMeResponse.code shouldBe 200

    val me = r[DataResponse[UserView, ObjectData[UserView]]](aboutMeResponse.body)
    me.data.data.id shouldBe user.id
    me.data.data.email shouldBe user.email
  }

  "POST /api/users/sign-in with valid data (remember = false)" should "return 200 code and an auth token" in {
    val response = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "${user.password}", "remember": false}""").asString
    response.code shouldBe 200

    val body = r[DataResponse[AuthToken, ObjectData[AuthToken]]](response.body)
    body.code shouldBe 200

    body.data.data.expireAt isAfter ZonedDateTime.now() shouldBe true
    val token = body.data.data.token
    val aboutMeResponse = get(req(userId(token).toString), token).asString
    aboutMeResponse.code shouldBe 200

    val me = r[DataResponse[UserView, ObjectData[UserView]]](aboutMeResponse.body)
    me.data.data.id shouldBe user.id
    me.data.data.email shouldBe user.email
  }

  "POST /api/users/sign-in with invalid password" should "return 400" in {
    val response = post(
      req("sign-in"),
      s"""{"email": "${user.email}", "password": "${user.password}1", "remember": true}""").asString
    response.code shouldBe 400
  }

  "POST /api/users/sign-in with not registered email" should "return 400" in {
    val response = post(
      req("sign-in"),
      s"""{"email": "${user.email}1", "password": "${user.password}", "remember": true}""").asString
    response.code shouldBe 400
  }
}
