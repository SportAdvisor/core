package io.sportadvisor.http.route.user

import io.sportadvisor.core.system.SystemService
import io.sportadvisor.http.{ValidationResult, Validator}

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserRouteValidators {

  import UserRouteProtocol._

  val emailInvalid = "Email is invalid"
  val nameIsEmpty = "Name is required"
  val passwordIsWeak =
    "Your password must be at least 8 characters long, and include at least one lowercase letter, one uppercase letter, and a number"
  val langNotSupported = "Selected language not supported"

  val regValidator: Validator[UsernamePasswordEmail] =
    Validator[UsernamePasswordEmail](
      u => nameValidator("name")(u.name),
      u => emailValidator("email")(u.email),
      u => {
        if (!u.password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$")) {
          Some(ValidationResult("password", passwordIsWeak))
        } else {
          None
        }
      }
    )

  val changeMailValidator: Validator[EmailChange] =
    Validator[EmailChange](
      u => emailValidator("email")(u.email)
    )

  val accountSettingsValidator: Validator[AccountSettings] =
    Validator[AccountSettings](
      a => nameValidator("name")(a.name),
      a =>
        a.language
          .map(l => SystemService.supportedLanguage().contains(l))
          .filter(b => !b)
          .map(_ => ValidationResult("language", langNotSupported))
    )

  private def emailValidator(field: String): (String => Option[ValidationResult]) = u => {
    if (!u.matches(".+@.+\\..+")) {
      Some(ValidationResult(field, emailInvalid))
    } else {
      None
    }
  }

  private def nameValidator(field: String): (String => Option[ValidationResult]) = name => {
    if (name.isEmpty) { Some(ValidationResult("name", nameIsEmpty)) } else None
  }

}
