package io.sportadvisor.http.route.user

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import io.sportadvisor.core.user.UserData

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserRouteProtocol {

  final case class UsernamePasswordEmail(name: String, email: String, password: String)
  final case class EmailChange(email: String, redirectUrl: String)
  final case class EmailPassword(email: String, password: String, remember: Boolean)
  final case class EmailToken(token: String)

  final case class UserView(id: Long, email: String, name: String)

  implicit val userNamePasswordDecoder: Decoder[UsernamePasswordEmail] = deriveDecoder
  implicit val emailPasswordDecoder: Decoder[EmailPassword] = deriveDecoder
  implicit val emailChangeDecoder: Decoder[EmailChange] = deriveDecoder
  implicit val emailTokenDecoder: Decoder[EmailToken] = deriveDecoder

  implicit val userViewEncoder: Encoder[UserView] = deriveEncoder

  def userView(user: UserData): UserView = UserView(user.id, user.email, user.name)

}
