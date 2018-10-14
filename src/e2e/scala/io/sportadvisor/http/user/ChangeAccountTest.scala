package io.sportadvisor.http.user

import io.sportadvisor.BaseE2ETest
import akka.http.scaladsl.model.StatusCodes._
import io.sportadvisor.http.Response._
import io.sportadvisor.http.route.user.UserRouteValidators

class ChangeAccountTest extends BaseE2ETest with UserMappings with DefaultUsersData {

  "Authorized user changes name with valid data" should "return OK code" in {
    val token = auth().token
    val newName = "New name"
    val language = "ru"
    val changeAccountResp =
      put(req(s"users/${user.id}"), s"""{"name": "$newName", "language": "$language"}""".stripMargin, token).asString
    changeAccountResp.code shouldBe OK.intValue
    val body = r[EmptyResponse](changeAccountResp.body)
    body.code shouldBe OK.intValue
  }

  "Authorized user changes language with valid data" should "return OK code" in {
    val token = auth().token
    val newName = "New name"
    val language = "ru"
    val changeAccountResp =
      put(req(s"users/${user.id}"), s"""{"name": "$newName", "language": "$language"}""".stripMargin, token).asString
    changeAccountResp.code shouldBe OK.intValue
    val body = r[EmptyResponse](changeAccountResp.body)
    body.code shouldBe OK.intValue
  }

  "Authorized user sets name to empty value" should "return 400 code and empty name error" in {
    val token = auth().token
    val newName = ""
    val language = "ru"
    val changeAccountResp =
      put(req(s"users/${user.id}"), s"""{"name": "$newName", "language": "$language"}""".stripMargin, token).asString
    changeAccountResp.code shouldBe BadRequest.intValue
    val errorResponse: ErrorResponse[FormError] = r[ErrorResponse[FormError]](changeAccountResp.body)
    errorResponse.code shouldBe BadRequest.intValue
    errorResponse.errors should (contain(FormError("name", UserRouteValidators.nameIsEmpty)) and have size 1)
  }

  "Authorized user sets language to unsupported language" should "return 400 code and language not supported error" in {
    val token = auth().token
    val newName = "New name"
    val language = "Test"
    val changeAccountResp =
      put(req(s"users/${user.id}"), s"""{"name": "$newName", "language": "$language"}""".stripMargin, token).asString
    changeAccountResp.code shouldBe BadRequest.intValue
    val errorResponse: ErrorResponse[FormError] = r[ErrorResponse[FormError]](changeAccountResp.body)
    errorResponse.code shouldBe BadRequest.intValue
    errorResponse.errors should (contain(FormError("name", UserRouteValidators.langNotSupported)) and have size 1)
  }

  "Authorized user tries to change another's account" should "return 403 code" in {
    val token = auth().token
    val newName = "New name"
    val language = "ru"
    val changeAccountResp =
      put(req(s"users/${2L}"), s"""{"name": "$newName", "language": "$language"}""".stripMargin, token).asString
    changeAccountResp.code shouldBe Forbidden.intValue
    val body = r[EmptyResponse](changeAccountResp.body)
    body.code shouldBe Forbidden.intValue
  }

  "Unauthorized user tries to change another's account" should "return 401 code" in {
    val newName = "New name"
    val language = "ru"
    val changeAccountResp =
      put(req(s"users/${user.id}"), s"""{"name": "$newName", "language": "$language"}""".stripMargin, "").asString
    changeAccountResp.code shouldBe Unauthorized.intValue
    val body = r[EmptyResponse](changeAccountResp.body)
    body.code shouldBe Unauthorized.intValue
  }

}
