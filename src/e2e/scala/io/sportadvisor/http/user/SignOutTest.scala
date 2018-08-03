package io.sportadvisor.http.user

import io.sportadvisor.BaseE2ETest
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.http.Response.{DataResponse, EmptyResponse, ObjectData}

/**
  * @author sss3 (Vladimir Alekseev)
  */
class SignOutTest extends BaseE2ETest with UserMappings with DefaultUsersData {

  "POST /api/users/sign-out after correct sign-out" should "not work refresh token" in {
    val authToken = auth()

    val refreshResp = post(req("sign-in/refresh"), s""" {"refreshToken":"${authToken.refreshToken}"} """).asString
    refreshResp.code shouldBe 200
    val afterRefresh = r[DataResponse[AuthToken, ObjectData[AuthToken]]](refreshResp.body)
    afterRefresh.code shouldBe 200

    post(req("sign-out"), "", authToken.token).asString

    val refreshResp2 = post(req("sign-in/refresh"), s""" {"refreshToken":"${authToken.refreshToken}"} """).asString
    refreshResp2.code shouldBe 400
  }

  "POST /api/users/sign-out without token" should "return 401" in {
    val resp = post(req("sign-out"), "").asString
    resp.code shouldBe 401

    r[EmptyResponse](resp.body).code shouldBe 401
  }

}
