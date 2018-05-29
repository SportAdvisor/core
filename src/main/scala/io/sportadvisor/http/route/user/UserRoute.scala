package io.sportadvisor.http.route.user

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
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
import scala.util.Success

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
          } ~ put {
            handleChangeAccount(userId)
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
    entity(as[UsernamePasswordEmail]) { request =>
      selectLanguage() { lang =>
        validatorDirective(request, regValidator, this) {
          complete(
            signUp(request.email, request.password, request.name).map {
              case Left(e)      => handleApiError(e, lang)
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
    entity(as[EmailChange]) { request =>
      authenticate(userService.secret) { userId =>
        checkAccess(id, userId) {
          selectLanguage() { lang =>
            validatorDirective(request, changeMailValidator, this) {
              complete(
                changeEmail(userId, request.email, request.redirectUrl).map {
                  case Left(e)  => handleApiError(e, lang)
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

  def handleChangeAccount(id: UserID): Route = {
    entity(as[AccountSettings]) { req =>
      authenticate(userService.secret) { userId =>
        checkAccess(id, userId) {
          validatorDirective(req, accountSettingsValidator, this) {
            onComplete(changeAccount(userId, req.name, req.language)) {
              case Success(o) =>
                o match {
                  case Some(u) =>
                    respondWithHeaders(Location(s"/api/users/$userId")) {
                      complete(r(Response.emptyResponse(StatusCodes.OK.intValue)))
                    }
                  case _ => complete(r(Response.failResponse(None)))
                }

              case _ => complete(r(Response.failResponse(None)))
            }
          }
        }
      }
    }
  }

  private def handleApiError(err: ApiError, lang: String): (StatusCode, Json) = {
    err.exception
      .map {
        case DuplicateException() =>
          r(Response.errorResponse(List(FormError("email", errors(lang).t(emailDuplication)))))
        case e @ _ =>
          e.error.foreach(t => log.warn("API ERROR", t))
          r(Response.failResponse(None))
      }
      .fold(r(Response.failResponse(None)))(r => r)
  }

}
