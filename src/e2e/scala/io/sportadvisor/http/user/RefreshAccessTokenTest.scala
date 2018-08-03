package io.sportadvisor.http.user

import io.sportadvisor.BaseE2ETest
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.http.Response.{DataResponse, EmptyResponse, ObjectData}

/**
  * @author sss3 (Vladimir Alekseev)
  */
class RefreshAccessTokenTest extends BaseE2ETest with UserMappings with DefaultUsersData {

  "POST /api/users/sign-in/refresh with correct refresh token" should "return new access token" in {
    val token = auth()
    val resp = post(req("sign-in/refresh"), s""" {"refreshToken":"${token.refreshToken}"} """).asString
    resp.code shouldBe 200
    val refreshedToken = r[DataResponse[AuthToken, ObjectData[AuthToken]]](resp.body)
    refreshedToken.code shouldBe 200

    token.refreshToken shouldBe refreshedToken.data.data.refreshToken
    token.expireAt isBefore refreshedToken.data.data.expireAt shouldBe true

    val accessToken = refreshedToken.data.data.token
    get(req(userId(accessToken).toString), accessToken).asString.code shouldBe 200
  }

  "POST /api/users/sign-in/refresh with broken refresh token" should "return 400" in {
    val resp = post(req("sign-in/refresh"), s""" {"refreshToken":"badToken"} """).asString
    resp.code shouldBe 400
    val response = r[EmptyResponse](resp.body)
    response.code shouldBe 400
  }

}
