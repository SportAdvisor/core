package io.sportadvisor.http.route.user

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.sportadvisor.core.user.{UserID, UserService}
import io.sportadvisor.exception._
import io.sportadvisor.http
import io.sportadvisor.http.json._
import io.sportadvisor.http.json.Codecs._
import io.sportadvisor.http.Response._
import io.sportadvisor.http.route.user.UserRouteProtocol._
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
  private val authError = "Authorization error. Re-login please"
  private val passwordIncorrect = "Incorrect password"

  import http._
  import userService._

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
          } ~ get {
            handleGetUser(userId)
          } ~ put {
            handleChangeAccount(userId)
          } ~ path("password") {
            put {
              handleChangePassword(userId)
            }
          }
        } ~ path("email-confirm") {
          post {
            handleConfirmEmail()
          }
        } ~ path("reset-password") {
          post {
            handleResetPassword()
          }
        } ~ path("me") {
          get {
            handleGetMe()
          }
        }
      }
    }
  }

  def handleSignUp(): Route = {
    entity(as[RegistrationModel]) { request =>
      selectLanguage() { lang =>
        validatorDirective(request, regValidator, this) {
          complete(
            signUp(request.email, request.password, request.name).map {
              case Left(e)      => handleEmailDuplicate(e, "email", lang)
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
                  case Left(e)  => handleEmailDuplicate(e, "email", lang)
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

  def handleResetPassword(): Route = {
    entity(as[ResetPassword]) { request =>
      validatorDirective(request, resetPasswordValidator, this) {
        complete(
          resetPassword(request.email, request.redirectUrl)
            .map(
              res =>
                r(
                  Response.emptyResponse(StatusCodes.OK.intValue)
              ))
        )
      }

    }
  }

  def handleGetMe(): Route = {
    authenticate(userService.secret) { userId =>
      redirect(s"/api/users/$userId", StatusCodes.SeeOther)
    }
  }

  def handleGetUser(id: UserID): Route = {
    authenticate(userService.secret) { userId =>
      checkAccess(id, userId) {
        complete(
          getById(id).map {
            case Some(u) => r(Response.objectResponse(userView(u), Option(s"/api/users/$userId")))
            case _       => r(Response.emptyResponse(StatusCodes.NotFound.intValue))
          }
        )
      }
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

  def handleChangePassword(id: UserID): Route = {
    entity(as[PasswordChange]) { req =>
      authenticate(userService.secret) { userId =>
        checkAccess(id, userId) {
          validatorDirective(req, changePasswordValidator, this) {
            selectLanguage() { lang =>
              complete(
                changePassword(userId, req.password, req.newPassword).map {
                  case Left(e)  => handlePasswordMismatch(e, "password", lang)
                  case Right(_) => r(Response.emptyResponse(StatusCodes.OK.intValue))
                }
              )
            }
          }
        }
      }
    }
  }

  private def handleEmailDuplicate(err: ApiError,
                                   field: String,
                                   lang: String): (StatusCode, Json) = {
    val handler: PartialFunction[ApiError, (StatusCode, Json)] = {
      case DuplicateException() =>
        r(Response.errorResponse(List(FormError(field, errors(lang).t(emailDuplication)))))
    }
    (handler orElse apiErrorHandler(lang))(err)
  }

  private def handlePasswordMismatch(err: ApiError,
                                     field: String,
                                     lang: String): (StatusCode, Json) = {
    val handler: PartialFunction[ApiError, (StatusCode, Json)] = {
      case PasswordMismatch() =>
        r(Response.errorResponse(List(FormError(field, errors(lang).t(passwordIncorrect)))))
    }
    (handler orElse apiErrorHandler(lang))(err)
  }

  private def apiErrorHandler(lang: String): PartialFunction[ApiError, (StatusCode, Json)] = {
    case UserNotFound(id) =>
      log.warn(s"Api error. User with id $id not found")
      val msg = errors(lang).t(authError)
      r(Response.failResponse(Some(msg)))
    case exception =>
      exception.error.fold(log.error(s"Api error: ${exception.msg}")) { e =>
        log.error(s"Api error: ${exception.msg}", e)
      }
      r(Response.failResponse(None))
  }

}
