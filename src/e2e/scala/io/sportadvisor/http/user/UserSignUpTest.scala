package io.sportadvisor.http.user

import io.sportadvisor.{BaseE2ETest, MailContainer}
import io.sportadvisor.http.Response.{ErrorResponse, FormError}

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserSignUpTest extends BaseE2ETest with UserMappings {

  "POST /api/users/sign-up with broken email" should "return error" in {
    val resp = post(req("sign-up"), s"""{"email": "testtest.com", "password": "test123Q", "name":"test", "EULA":true}""").asString
    resp.code shouldBe 400

    val body = r[ErrorResponse[FormError]](resp.body)
    body.code shouldBe 400
    body.errors should (contain(FormError("email", "Email is invalid")) and have size 1)
  }
}
