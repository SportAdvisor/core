package io.sportadvisor.core

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.sportadvisor.core.user.UserModels.{AuthTokenContent, RefreshTokenContent}

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object user {

  implicit val tokenDecoder: Decoder[AuthTokenContent] = deriveDecoder
  implicit val tokenEncoder: Encoder[AuthTokenContent] = deriveEncoder
  implicit val refreshTokenDecoder: Decoder[RefreshTokenContent] = deriveDecoder
  implicit val refreshTokenEncoder: Encoder[RefreshTokenContent] = deriveEncoder
}
