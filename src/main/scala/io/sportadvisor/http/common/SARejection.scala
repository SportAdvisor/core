package io.sportadvisor.http.common

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Rejection
import io.circe.Json
import io.circe.syntax._
import io.sportadvisor.http.Response
import io.sportadvisor.http.json.encoder

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait SARejection extends Rejection {
  def code: StatusCode

  def response: Json = Response.emptyResponse(code.intValue).asJson
}

final case class Forbidden() extends SARejection {
  override def code: StatusCode = StatusCodes.Forbidden
}

final case class Unauthorized() extends SARejection {
  override def code: StatusCode = StatusCodes.Unauthorized
}
