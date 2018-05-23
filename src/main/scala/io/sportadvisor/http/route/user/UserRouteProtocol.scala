package io.sportadvisor.http.route.user

import io.circe.generic.semiauto._
import io.circe.Decoder

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserRouteProtocol {

  final case class RegistrationModel(name: String, email: String, password: String, EULA: Boolean)
  final case class EmailChange(email: String, redirectUrl: String)
  final case class EmailPassword(email: String, password: String, remember: Boolean)
  final case class EmailToken(token: String)

  implicit val userNamePasswordDecoder: Decoder[RegistrationModel] = deriveDecoder
  implicit val emailPasswordDecoder: Decoder[EmailPassword] = deriveDecoder
  implicit val emailChangeDecoder: Decoder[EmailChange] = deriveDecoder
  implicit val emailTokenDecoder: Decoder[EmailToken] = deriveDecoder
}
