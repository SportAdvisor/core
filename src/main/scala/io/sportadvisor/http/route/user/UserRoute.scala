package io.sportadvisor.http.route.user

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.syntax._
import io.circe.Json
import io.sportadvisor.core.user.{UserID, UserService}
import io.sportadvisor.exception.{ApiError, DuplicateException}
import io.sportadvisor.http
import io.sportadvisor.http.Response.{FormError, Response}
import io.sportadvisor.http.json._
import io.sportadvisor.http.json.Codecs._
import io.sportadvisor.http.route.user.UserRouteValidators._
import io.sportadvisor.util.I18nService
import org.slf4s.Logging

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
abstract class UserRoute(userService: UserService)(implicit executionContext: ExecutionContext)
    extends FailFastCirceSupport
    with I18nService
    with Logging {

  private val emailDuplication = "Email address is already registered"

  import http._
  import userService._
  import UserRouteProtocol._

  val route: Route = pathPrefix("users") {
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        path("sign-up") {
          post {
            handleSignUp()
          }
        } ~ path("sign-in") {
          post {
            handleSignIn()
          }
        } ~ pathPrefix(LongNumber) { userId =>
          path("email") {
            put {
              handleChangeEmail(userId)
            }
          }
        } ~ path("email-confirm") {
          post {
            handleConfirmEmail()
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
              case Left(e)      => r(handleApiError(e, lang))
              case Right(token) => r(Response.objectResponse(token, None))
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
          case Some(token) => r(Response.objectResponse(token, None))
          case None        => r(Response.emptyResponse(StatusCodes.BadRequest.intValue))
        }
      )
    }
  }

  def handleChangeEmail(id: UserID): Route = {
    entity(as[EmailChange]) { req =>
      authenticate(userService.secret) { userId =>
        checkAccess(id, userId) {
          selectLanguage() { lang =>
            validatorDirective(req, changeMailValidator, this) { entity =>
              complete(
                changeEmail(userId, entity.email, entity.redirectUrl).map {
                  case Left(e)  => r(handleApiError(e, lang))
                  case Right(_) => r(Response.emptyResponse(StatusCodes.OK.intValue))
                }
              )
            }
          }
        }
      }
    }
  }

  def handleConfirmEmail(): Route = {
    entity(as[EmailToken]) { entity =>
      complete(
        confirmEmail(entity.token).map { res =>
          r(
            Response.emptyResponse(
              if (res) StatusCodes.OK.intValue else StatusCodes.BadRequest.intValue))
        }
      )
    }
  }

  private def handleApiError(err: ApiError, lang: String): Response = {
    err.exception
      .map {
        case DuplicateException() =>
          Response.errorResponse(List(FormError("email", errors(lang).t(emailDuplication))))
        case e @ _ =>
          e.error.foreach(t => log.warn("API ERROR", t))
          Response.failResponse(None)
      }
      .fold(Response.failResponse(None))(r => r)
  }

  private def r(response: Response): (StatusCode, Json) =
    StatusCode.int2StatusCode(response.code) -> response.asJson

}
