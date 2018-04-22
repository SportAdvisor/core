package io.sportadvisor.http.route

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.{Decoder, Json}
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.sportadvisor.core.user.UserService
import io.sportadvisor.http
import io.sportadvisor.http.Response.{FormError, Response}
import io.sportadvisor.http.json._
import io.sportadvisor.http.json.Codecs._

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserRoute(userService: UserService)(implicit executionContext: ExecutionContext) extends FailFastCirceSupport {

  import userService._
  import http._

  val route = pathPrefix("users") {
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        path("sign-up") {
          pathEndOrSingleSlash {
            post {
              handleSignUp()
            }
          }
        }
      }
    }
  }

  def handleSignUp() : Route = {
    entity(as[UsernamePasswordEmail]) { entity =>
      validatorDirective(entity, regValidator) { request =>
        complete(
          signUp(request.email, request.password, request.name).map {
            case Left(e) => r(Response.errorResponse(List(FormError("email", ErrorCode.duplicationError))))
            case Right(token) => r(Response.dataResponse(token, null))
          }
        )
      }
    }
  }

  def r(response: Response) : (StatusCode, Json) = StatusCode.int2StatusCode(response.code) -> response.asJson

  case class UsernamePasswordEmail(name: String, email: String, password: String)

  private implicit val regValidator: Validator[UsernamePasswordEmail] = Validator[UsernamePasswordEmail](
    u => if (u.name.isEmpty) {Some(FormError("name", ErrorCode.invalidField))} else None,
    u => if (!u.email.matches(".+@.+\\..+")) {Some(FormError("email", ErrorCode.invalidField))} else None,
    u => if (!u.password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"))
      {Some(FormError("password", ErrorCode.invalidField))} else None
  )

  implicit val userNamePasswordDecoder: Decoder[UsernamePasswordEmail] = deriveDecoder
}
