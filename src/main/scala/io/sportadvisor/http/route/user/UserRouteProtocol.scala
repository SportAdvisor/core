package io.sportadvisor.http.route.user

import io.circe.generic.semiauto._
import io.circe.Decoder

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserRouteProtocol {

  final case class UsernamePasswordEmail(name: String, email: String, password: String)
  final case class EmailChange(email: String, redirectUrl: String)
  final case class EmailPassword(email: String, password: String, remember: Boolean)

  implicit val userNamePasswordDecoder: Decoder[UsernamePasswordEmail] = deriveDecoder
  implicit val emailPasswordDecoder: Decoder[EmailPassword] = deriveDecoder
  implicit val emailChangeDecoder: Decoder[EmailChange] = deriveDecoder
}
