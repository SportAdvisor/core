package io.sportadvisor.http.route.user

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import io.sportadvisor.core.user.UserData

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserRouteProtocol {

  final case class RegistrationModel(name: String, email: String, password: String, EULA: Boolean)
  final case class EmailChange(email: String, redirectUrl: String)
  final case class EmailPassword(email: String, password: String, remember: Boolean)
  final case class EmailToken(token: String)
  final case class ResetPassword(email: String, redirectUrl: String)
  final case class ConfirmPassword(token: String, password: String)
  final case class AccountSettings(name: String, language: Option[String])
  final case class PasswordChange(password: String, newPassword: String)

  final case class UserView(id: Long, email: String, name: String, language: Option[String])

  implicit val userNamePasswordDecoder: Decoder[RegistrationModel] = deriveDecoder
  implicit val emailPasswordDecoder: Decoder[EmailPassword] = deriveDecoder
  implicit val emailChangeDecoder: Decoder[EmailChange] = deriveDecoder
  implicit val emailTokenDecoder: Decoder[EmailToken] = deriveDecoder
  implicit val resetPasswordDecoder: Decoder[ResetPassword] = deriveDecoder
  implicit val accountSettingsDecoder: Decoder[AccountSettings] = deriveDecoder
  implicit val passwordChangeDecoder: Decoder[PasswordChange] = deriveDecoder

  implicit val userViewEncoder: Encoder[UserView] = deriveEncoder

  def userView(user: UserData): UserView = UserView(user.id, user.email, user.name, user.language)

}
