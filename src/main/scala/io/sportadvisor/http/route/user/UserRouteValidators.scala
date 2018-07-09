package io.sportadvisor.http.route.user

import io.sportadvisor.core.system.SystemService
import io.sportadvisor.http.common.{Validated, ValidationResult}

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserRouteValidators {

  import UserRouteProtocol._

  val emailInvalid = "Email is invalid"
  val nameIsEmpty = "Name is required"
  val passwordIsWeak: String =
    "Your password must be at least 8 characters long, and include at least one lowercase letter, " +
      "one uppercase letter, and a number. Your password can't contain spaces"
  val EUALIsRequired = "You must accept the end-user license agreement"
  val langNotSupported = "Selected language not supported"

  implicit val regValidator: Validated[RegistrationModel] =
    Validated[RegistrationModel](
      (u: RegistrationModel) => nameValidator("name")(u.name),
      (u: RegistrationModel) => emailValidator("email")(u.email),
      (u: RegistrationModel) => passwordValidator("password")(u.password),
      (u: RegistrationModel) =>
        if (!u.EULA) {
          Some(ValidationResult("EULA", EUALIsRequired))
        } else {
          None
      }
    )

  implicit val changeMailValidator: Validated[EmailChange] =
    Validated[EmailChange](
      (u: EmailChange) => emailValidator("email")(u.email)
    )

  implicit val accountSettingsValidator: Validated[AccountSettings] =
    Validated[AccountSettings](
      (a: AccountSettings) => nameValidator("name")(a.name),
      (a: AccountSettings) =>
        a.language
          .map(l => SystemService.supportedLanguage().contains(l))
          .filter(b => !b)
          .map(_ => ValidationResult("language", langNotSupported))
    )

  implicit val changePasswordValidator: Validated[PasswordChange] =
    Validated[PasswordChange]((p: PasswordChange) =>
      passwordValidator("newPassword")(p.newPassword))

  implicit val resetPasswordValidator: Validated[ResetPassword] =
    Validated[ResetPassword](
      (u: ResetPassword) => emailValidator("email")(u.email)
    )

  implicit val confirmPasswordValidator: Validated[ConfirmPassword] =
    Validated[ConfirmPassword](
      (u: ConfirmPassword) => passwordValidator("password")(u.password)
    )

  private def emailValidator(field: String): (String => Option[ValidationResult]) = u => {
    if (!u.matches(".+@.+\\..+")) {
      Some(ValidationResult(field, emailInvalid))
    } else {
      None
    }
  }

  private def nameValidator(field: String): (String => Option[ValidationResult]) = name => {
    if (name.trim.isEmpty) { Some(ValidationResult(field, nameIsEmpty)) } else None
  }

  private def passwordValidator(field: String): (String => Option[ValidationResult]) = password => {
    if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[\\S]{8,}$")) {
      Some(ValidationResult(field, passwordIsWeak))
    } else {
      None
    }
  }

}
