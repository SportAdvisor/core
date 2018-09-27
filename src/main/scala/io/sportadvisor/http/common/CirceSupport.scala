package io.sportadvisor.http.common

import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport
import io.circe.{CursorOp, Decoder, DecodingFailure, Json}
import io.sportadvisor.http.common.CirceSupport.CirceError

import scala.annotation.tailrec
import scala.util.control.NoStackTrace

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait CirceSupport extends BaseCirceSupport {

  override implicit final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    def decode(json: Json) = Decoder[A].decodeJson(json).fold(e => throw mapError(e, json), identity)
    jsonUnmarshaller.map(decode)
  }

  private[this] def mapError(err: DecodingFailure, json: Json): CirceError = {
    val field = fieldFromHistory(err.history)
    if (json.hcursor.downField(field).succeeded) {
      CirceError(field, CommonValidations.invalidField)
    } else {
      CirceError(field, CommonValidations.requiredField)
    }
  }

  @tailrec
  private[this] def fieldFromHistory(hist: List[CursorOp], arrayIndex: Int = 0, out: List[String] = Nil): String = hist match {
    case some :: rest ⇒ some match {
      case CursorOp.MoveRight    ⇒ fieldFromHistory(rest, arrayIndex + 1, out)
      case CursorOp.MoveLeft     ⇒ fieldFromHistory(rest, arrayIndex - 1, out)
      case CursorOp.DownArray    ⇒ fieldFromHistory(rest, 0, s"[$arrayIndex]" :: out)
      case CursorOp.DownField(f) ⇒ fieldFromHistory(rest, arrayIndex, f :: out)
      case _                     ⇒ fieldFromHistory(rest, arrayIndex, out)
    }
    case Nil          ⇒ out.mkString(".")
  }

}

object CirceSupport extends CirceSupport {

  case class CirceError(field: String, msg: String) extends RuntimeException with NoStackTrace
}
