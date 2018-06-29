package io.sportadvisor.http.user

import akka.http.scaladsl.model.StatusCodes._
import io.sportadvisor.core.user.UserModels.AuthToken
import io.sportadvisor.http.Response._
import io.sportadvisor.http.route.user.UserRouteValidators
import io.sportadvisor.{BaseE2ETest, MailContainer}
import scala.concurrent.duration._

/**
  * @author sss3 (Vladimir Alekseev)
  */
class ResetPasswordTest
    extends BaseE2ETest
    with UserMappings
    with MailContainer
    with DefaultUsersData {

  "not found user by email " should "return ok" in {
    val email = "not-found@sportadvisor.io"
    val resp = post(req("reset-password"), s"""{"email": "$email", "redirectUrl": ""}""").asString
    resp.code shouldBe OK.intValue

    val body = r[EmptyResponse](resp.body)
    body.code shouldBe OK.intValue
    messages().exists(m => m.to.contains(email)) shouldBe false
  }

  "correct data" should "return ok" in {
    val password = "Passw0rd"
    val resp =
      post(req("reset-password"), s"""{"email": "${user.email}", "redirectUrl": ""}""").asString
    resp.code shouldBe OK.intValue

    val body = r[EmptyResponse](resp.body)
    body.code shouldBe OK.intValue

    val token = messages()
      .find(m => m.to.contains(user.email) && m.subject == "Reset password on SportAdvisor")
      .flatMap(getTokenFromMail) getOrElse ""
    val confirmResp =
      post(req("password-confirm"), s"""{"token": "$token", "password": "$password"}""").asString
    confirmResp.code shouldBe OK.intValue

    val confirmBody = r[EmptyResponse](confirmResp.body)
    confirmBody.code shouldBe OK.intValue
    user = user.copy(password = password)

    val authResp =
      post(req("sign-in"),
           s"""{"email": "${user.email}", "password": "$password", "remember":true}""").asString
    authResp.code shouldBe OK.intValue

    val authBody = r[DataResponse[AuthToken, ObjectData[AuthToken]]](authResp.body)
    authBody.code shouldBe OK.intValue
    authBody.data.data.token.isEmpty shouldBe false
  }

  "expired token (not found)" should "return 400 and error" in {
    val confirmResp = post(req("password-confirm"),
                           s"""{"token": "not-found", "password": "${user.password}"}""").asString
    confirmResp.code shouldBe BadRequest.intValue
    val body = r[ErrorResponse[FormError]](confirmResp.body)
    body.errors should (contain(
      FormError("token",
                "Your password reset link has expired. Please initiate a new password reset"))
      and have size 1)
    body.code shouldBe BadRequest.intValue
  }

  "invalid password and retry with correct" should "change password" in {
    val invalidPass = "weakpasswor"
    val resp =
      post(req("reset-password"), s"""{"email": "${user.email}", "redirectUrl": ""}""").asString
    resp.code shouldBe OK.intValue

    val body = r[EmptyResponse](resp.body)
    body.code shouldBe OK.intValue

    val token = messages()
      .find(m => m.to.contains(user.email) && m.subject == "Reset password on SportAdvisor")
      .flatMap(getTokenFromMail) getOrElse ""

    val confirmResp =
      post(req("password-confirm"), s"""{"token": "$token", "password": "$invalidPass"}""").asString
    confirmResp.code shouldBe BadRequest.intValue

    val confirmBody = r[ErrorResponse[FormError]](confirmResp.body)
    confirmBody.code shouldBe BadRequest.intValue
    confirmBody.errors should (contain(FormError("password", UserRouteValidators.passwordIsWeak))
      and have size 1)
    confirmBody.code shouldBe BadRequest.intValue

    val password = "str0nGpasS"
    val confirmRespRetry =
      post(req("password-confirm"), s"""{"token": "$token", "password": "$password"}""").asString
    confirmRespRetry.code shouldBe OK.intValue

    val confirmBodyRetry = r[EmptyResponse](confirmRespRetry.body)
    confirmBodyRetry.code shouldBe OK.intValue

    user = user.copy(password = password)

    val authResp =
      post(req("sign-in"),
           s"""{"email": "${user.email}", "password": "$password", "remember":true}""").asString
    authResp.code shouldBe OK.intValue

    val authBody = r[DataResponse[AuthToken, ObjectData[AuthToken]]](authResp.body)
    authBody.code shouldBe OK.intValue
    authBody.data.data.token.isEmpty shouldBe false
  }

  "double reset pass by one token" should "return error" in {
    sleep(2.seconds)
    val resp =
      post(req("reset-password"), s"""{"email": "${user.email}", "redirectUrl": ""}""").asString
    resp.code shouldBe OK.intValue
    val body = r[EmptyResponse](resp.body)

    body.code shouldBe OK.intValue
    val token = messages()
      .find(m => m.to.contains(user.email) && m.subject == "Reset password on SportAdvisor")
      .flatMap(getTokenFromMail) getOrElse ""

    val password = "Passw0rd123"

    val confirmResp =
      post(req("password-confirm"), s"""{"token": "$token", "password": "$password"}""").asString
    confirmResp.code shouldBe OK.intValue

    val confirmBody = r[EmptyResponse](confirmResp.body)
    confirmBody.code shouldBe OK.intValue
    user = user.copy(password = password)

    val authResp =
      post(req("sign-in"),
           s"""{"email": "${user.email}", "password": "$password", "remember":true}""").asString
    authResp.code shouldBe OK.intValue

    val authBody = r[DataResponse[AuthToken, ObjectData[AuthToken]]](authResp.body)
    authBody.code shouldBe OK.intValue
    authBody.data.data.token.isEmpty shouldBe false

    val confirmRespRepeat =
      post(req("password-confirm"), s"""{"token": "$token", "password": "$password"}""").asString
    confirmRespRepeat.code shouldBe BadRequest.intValue

    val confirmBodyRepeat = r[ErrorResponse[FormError]](confirmRespRepeat.body)
    confirmBodyRepeat.code shouldBe BadRequest.intValue
    confirmBodyRepeat.errors should (contain(
      FormError("token",
                "Your password reset link has expired. Please initiate a new password reset"))
      and have size 1)
  }

  "reset password by old token" should "return error" in {
    sleep(2.seconds)
    val resp =
      post(req("reset-password"), s"""{"email": "${user.email}", "redirectUrl": ""}""").asString
    resp.code shouldBe OK.intValue
    val body = r[EmptyResponse](resp.body)

    body.code shouldBe OK.intValue
    val token = messages()
      .find(m => m.to.contains(user.email) && m.subject == "Reset password on SportAdvisor")
      .flatMap(getTokenFromMail) getOrElse ""

    sleep(2.seconds)
    val resp2 =
      post(req("reset-password"), s"""{"email": "${user.email}", "redirectUrl": ""}""").asString
    resp2.code shouldBe OK.intValue
    val body2 = r[EmptyResponse](resp2.body)

    body2.code shouldBe OK.intValue
    val token2 = messages()
      .filter(m => m.to.contains(user.email) && m.subject == "Reset password on SportAdvisor")
      .flatMap(getTokenFromMail(_).toList).find(t => t != token) getOrElse ""

    val password = "ASDasd123"

    val confirmResp =
      post(req("password-confirm"), s"""{"token": "$token", "password": "$password"}""").asString
    confirmResp.code shouldBe OK.intValue

    val confirmBody = r[EmptyResponse](confirmResp.body)
    confirmBody.code shouldBe OK.intValue
    user = user.copy(password = password)

    val authResp =
      post(req("sign-in"),
        s"""{"email": "${user.email}", "password": "$password", "remember":true}""").asString
    authResp.code shouldBe OK.intValue

    val authBody = r[DataResponse[AuthToken, ObjectData[AuthToken]]](authResp.body)
    authBody.code shouldBe OK.intValue
    authBody.data.data.token.isEmpty shouldBe false

    val confirmResp2 =
      post(req("password-confirm"), s"""{"token": "$token2", "password": "$password"}""").asString
    confirmResp2.code shouldBe BadRequest.intValue

    val confirmBody2 = r[ErrorResponse[FormError]](confirmResp2.body)
    confirmBody2.code shouldBe BadRequest.intValue
    confirmBody2.errors should (contain(FormError("token", "???")) and have size 1)
  }

  "concurrent reset" should "return error" in {
    sleep(2.seconds)
    val resp1 =
      post(req("reset-password"), s"""{"email": "${user.email}", "redirectUrl": ""}""").asString
    val resp2 =
      post(req("reset-password"), s"""{"email": "${user.email}", "redirectUrl": ""}""").asString
    resp1.code shouldBe OK.intValue
    resp2.code shouldBe BadRequest.intValue

    val body = r[ErrorResponse[FormError]](resp2.body)
    body.code shouldBe BadRequest.intValue
    body.errors should (contain(FormError("email", "???")) and have size 1)
  }
}
