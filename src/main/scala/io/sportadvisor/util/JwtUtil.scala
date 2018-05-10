package io.sportadvisor.util

import java.time.{LocalDateTime, ZoneId}

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Encoder
import pdi.jwt._
import io.circe.syntax._
import io.circe.generic.auto._

/**
  * @author sss3 (Vladimir Alekseev)
  */
object JwtUtil extends FailFastCirceSupport {

  def encode[C](content: C, secret: String, expAt: Option[LocalDateTime])
               (implicit encoder: Encoder[C]): String = {
    val claim = JwtClaim.apply(
      content = content.asJson.noSpaces,
      expiration = expAt.map(e => e.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli))
    Jwt.encode(claim, secret, JwtAlgorithm.HS256)
  }

}
