package io.sportadvisor.http.route.user

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.HeaderDirectives.optionalHeaderValueByName
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.sportadvisor.core.user.UserModels.{AuthTokenContent, PasswordMismatch, UserID}
import io.sportadvisor.core.user.UserService
import io.sportadvisor.exception.Exceptions._
import io.sportadvisor.exception._
import io.sportadvisor.http
import io.sportadvisor.http.Response._
import io.sportadvisor.http.route.user.UserRouteProtocol._
import io.sportadvisor.http.route.user.UserRouteValidators._
import io.sportadvisor.util.{I18nService, JwtUtil}
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
  private val resetPwdExpired =
    "Your password reset link has expired. Please initiate a new password reset"

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
        } ~ path("password-confirm") {
          post {
            handleConfirmNewPassword()
          }
        } ~ path("me") {
          get {
            handleGetMe()
          }
        } ~ path("logout") {
          post {
            handleLogOut()
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

  def handleLogOut(): Route = {
    optionalHeaderValueByName(authorizationHeader) {
      case Some(value) => {
        JwtUtil.decode[AuthTokenContent](value, userService.secret) match {
          case Some(token) =>
            complete(
              userService
                .deleteTokenById(token.refreshTokenId)
                .map(_ => r(Response.emptyResponse(StatusCodes.OK.intValue))))
          case _ => complete(r(Response.emptyResponse(StatusCodes.Unauthorized.intValue)))
        }
      }
      case None => complete(r(Response.emptyResponse(StatusCodes.Unauthorized.intValue)))
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
        selectLanguage() { lang =>
          complete(
            resetPassword(request.email, request.redirectUrl)
              .map {
                case Left(e)  => handleTokenDuplicate(e, lang)
                case Right(_) => r(Response.emptyResponse(StatusCodes.OK.intValue))
              }
          )
        }
      }
    }
  }

  def handleConfirmNewPassword(): Route = {
    entity(as[ConfirmPassword]) { request =>
      validatorDirective(request, confirmPasswordValidator, this) {
        selectLanguage() { language =>
          complete(
            setNewPassword(request.token, request.password)
              .map {
                case Left(e)  => handleResetPasswordErrors(e, "token", language)
                case Right(_) => r(Response.emptyResponse(StatusCodes.OK.intValue))
              }
          )
        }
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
                    respondWithHeaders(Location(s"/api/users/${u.id}")) {
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

  private def handleResetPasswordErrors(err: ApiError,
                                        field: String,
                                        lang: String): (StatusCode, Json) = {
    val handler: PartialFunction[ApiError, (StatusCode, Json)] = {
      case TokenDoesntExist(_) =>
        r(Response.errorResponse(List(FormError(field, errors(lang).t(resetPwdExpired)))))
      case TokenExpired(_) =>
        r(Response.errorResponse(List(FormError(field, errors(lang).t(resetPwdExpired)))))
    }
    (handler orElse apiErrorHandler(lang))(err)
  }

  private def handleTokenDuplicate(err: ApiError, lang: String): (StatusCode, Json) = {
    val handler: PartialFunction[ApiError, (StatusCode, Json)] = {
      case DuplicateException() =>
        r(Response.emptyResponse(StatusCodes.OK.intValue))
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
    case ResourceNotFound(id) =>
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
