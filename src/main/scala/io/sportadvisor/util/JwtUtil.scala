package io.sportadvisor.util

import java.time.{LocalDateTime, ZoneId}

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe._
import io.circe.parser._
import pdi.jwt._
import io.circe.syntax._

/**
  * @author sss3 (Vladimir Alekseev)
  */
object JwtUtil extends FailFastCirceSupport {

  def encode[C](content: C, secret: String, expAt: Option[LocalDateTime])(
      implicit encoder: Encoder[C]): String = {
    val claim = JwtClaim.apply(
      content = content.asJson.noSpaces,
      expiration = expAt.map(e => e.atZone(ZoneId.systemDefault()).toInstant.getEpochSecond))
    Jwt.encode(claim, secret, JwtAlgorithm.HS256)
  }

  def decode[C](content: String, secret: String)(implicit d: Decoder[C]): Option[C] = {
    Jwt
      .decodeRaw(content, secret, Seq(JwtAlgorithm.HS256))
      .toOption
      .flatMap(s => parse(s).toOption)
      .flatMap(j => d(HCursor.fromJson(j)).toOption)
  }

}
