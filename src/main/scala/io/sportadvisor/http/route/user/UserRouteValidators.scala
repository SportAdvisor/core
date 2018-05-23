package io.sportadvisor.http.route.user

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
  val EUALIsRequired = ""

  val regValidator: Validator[RegistrationModel] =
    Validator[RegistrationModel](
      u => if (u.name.isEmpty) { Some(ValidationResult("name", nameIsEmpty)) } else None,
      u => emailValidator("email")(u.email),
      u => {
        if (!u.password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$")) {
          Some(ValidationResult("password", passwordIsWeak))
        } else {
          None
        }
      },
      u =>
        if (!u.EULA) {
          Some(ValidationResult("EULA", "You need to agree to the end-user license agreement"))
        } else {
          None
      }
    )

  val changeMailValidator: Validator[EmailChange] =
    Validator[EmailChange](
      u => emailValidator("email")(u.email)
    )

  private def emailValidator(field: String): (String => Option[ValidationResult]) = u => {
    if (!u.matches(".+@.+\\..+")) {
      Some(ValidationResult(field, emailInvalid))
    } else {
      None
    }
  }

}
