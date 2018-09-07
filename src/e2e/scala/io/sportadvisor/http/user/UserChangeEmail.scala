package io.sportadvisor.http.user

import akka.http.scaladsl.model.StatusCodes._
import io.sportadvisor.http.Response._
import io.sportadvisor.http.route.user.UserRouteProtocol.UserView
import io.sportadvisor.http.route.user.UserRouteValidators
import io.sportadvisor.{BaseE2ETest, MailContainer}

import scala.concurrent.duration._

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserChangeEmail
    extends BaseE2ETest
    with UserMappings
    with MailContainer
    with DefaultUsersData {

  "User change email with correct email" should "return ok" in {
    val newMail = "test123@sportadvisor.io"
    val token = auth().token
    val changeEmailResp =
      put(req(s"${user.id}/email"),
          s"""{"email": "$newMail", "redirectUrl": ""}""",
          token).timeout(10.seconds).asString
    changeEmailResp.code shouldBe OK.intValue
    val changeToken = messages()
      .find(m => m.subject == "Change email on SportAdvisor")
      .flatMap(getTokenFromMail) getOrElse ""
    val confirmMailResp = post(req("email-confirm"), s"""{"token": "$changeToken"}""").asString
    confirmMailResp.code shouldBe OK.intValue
    r[EmptyResponse](confirmMailResp.body).code shouldBe OK.intValue
    val confirmMail = messages().find(m =>
      m.to.contains(user.email) && m.subject == "Change email on SportAdvisor")
    confirmMail.isDefined shouldBe true
    val meInfo = get(req(user.id.toString), token).asString
    meInfo.code shouldBe OK.intValue
    val me = r[DataResponse[UserView, ObjectData[UserView]]](meInfo.body)
    me.code shouldBe OK.intValue
    val data = me.data
    data.data.id shouldBe user.id
    data.data.email shouldBe newMail
    data.links.isDefined shouldBe true
    data.links.get.self.href endsWith user.id.toString shouldBe true
    user = user.copy(email = newMail)
  }

  "User change with broken email" should "return 400 and form error" in {
    val token = auth().token
    val changeEmailResp =
      put(req(s"${user.id}/email"),
        s"""{"email": "bad-email", "redirectUrl": ""}""",
        token).asString
    changeEmailResp.code shouldBe BadRequest.intValue
    val errors = r[ErrorResponse[FieldFormError]](changeEmailResp.body)
    errors.code shouldBe BadRequest.intValue
    errors.errors should (contain(FieldFormError("email", UserRouteValidators.emailInvalid)) and have size 1)
  }

  "User change another email" should "return 403" in {
    val token = auth().token
    val changeEmailResp =
      put(req(s"${user.id + 1}/email"),
        s"""{"email": "email@sportadvisor.io", "redirectUrl": ""}""",
        token).asString
    changeEmailResp.code shouldBe Forbidden.intValue
    r[EmptyResponse](changeEmailResp.body).code shouldBe Forbidden.intValue
  }

  "User change email without auth token" should "return 401" in {
    val changeEmailResp =
      put(req(s"${user.id}/email"),
        s"""{"email": "email@sportadvisor.io", "redirectUrl": ""}""").asString
    changeEmailResp.code shouldBe Unauthorized.intValue
    r[EmptyResponse](changeEmailResp.body).code shouldBe Unauthorized.intValue
  }

}
