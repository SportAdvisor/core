package io.sportadvisor.http.route

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.{Decoder, Json}
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.sportadvisor.core.user.UserService
import io.sportadvisor.http
import io.sportadvisor.http.I18nService
import io.sportadvisor.http.Response.{FormError, Response}
import io.sportadvisor.http.json._
import io.sportadvisor.http.json.Codecs._

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
abstract class UserRoute(userService: UserService)(implicit executionContext: ExecutionContext)
    extends FailFastCirceSupport
    with I18nService {

  private val emailDuplication = "Email address is already registered"
  private val emailInvalid = "Email is invalid"
  private val nameIsEmpty = "Name is required"
  private val passwordIsWeak =
    "Your password must be at least 8 characters long, and include at least one lowercase letter, one uppercase letter, and a number"

  import userService._
  import http._

  val route: Route = pathPrefix("users") {
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        path("sign-up") {
          pathEndOrSingleSlash {
            post {
              handleSignUp()
            }
          }
        } ~ path("sign-in") {
          pathEndOrSingleSlash {
            post {
              handleSignIn()
            }
          }
        }
      }
    }
  }

  def handleSignUp(): Route = {
    entity(as[UsernamePasswordEmail]) { entity =>
      selectLanguage() { lang =>
        validatorDirective(entity, regValidator, this) { request =>
          complete(
            signUp(request.email, request.password, request.name).map {
              case Left(e) =>
                r(
                  Response.errorResponse(
                    List(FormError("email", errors(lang).t(emailDuplication)))))
              case Right(token) => r(Response.dataResponse(token, None))
            }
          )
        }
      }
    }
  }

  def handleSignIn(): Route = {
    entity(as[EmailPassword]) { req =>
      complete(
        signIn(req.email, req.password, req.remember).map {
          case Some(token) => r(Response.dataResponse(token, None))
          case None        => r(Response.emptyResponse(StatusCodes.BadRequest.intValue))
        }
      )
    }
  }

  def r(response: Response): (StatusCode, Json) =
    StatusCode.int2StatusCode(response.code) -> response.asJson

  case class UsernamePasswordEmail(name: String, email: String, password: String)

  case class EmailPassword(email: String, password: String, remember: Boolean)

  private implicit val regValidator: Validator[UsernamePasswordEmail] =
    Validator[UsernamePasswordEmail](
      u => if (u.name.isEmpty) { Some(ValidationResult("name", nameIsEmpty)) } else None,
      u => {
        if (!u.email.matches(".+@.+\\..+")) {
          Some(ValidationResult("email", emailInvalid))
        } else {
          None
        }
      },
      u => {
        if (!u.password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$")) {
          Some(ValidationResult("password", passwordIsWeak))
        } else {
          None
        }
      }
    )

  implicit val userNamePasswordDecoder: Decoder[UsernamePasswordEmail] = deriveDecoder
  implicit val emailPasswordDecoder: Decoder[EmailPassword] = deriveDecoder
}
