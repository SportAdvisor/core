package io.sportadvisor.http

import io.circe._
import io.circe.generic.semiauto._
import io.sportadvisor.BaseTest
import io.sportadvisor.http.Response.{CollectionData, EmptyResponse, ErrorResponse, FailResponse, FormError, ObjectData}
import io.sportadvisor.http.json._
/**
  * @author sss3 (Vladimir Alekseev)
  */
class ResponseMarshallerTest extends BaseTest {

  "Response" when {
    "EmptyResponse" should {
      "serialize and deserialize" in {
        val resp = Response.emptyResponse(200)
        val json = marshall(resp)
        val response = unmarshall(json, implicitly[Decoder[EmptyResponse]])
        resp shouldEqual response
      }
    }

    "FailResponse" should {
      "serialize and deserialize" in {
        val resp = Response.failResponse(Option("test"))
        val json = marshall(resp)
        val response = unmarshall(json, implicitly[Decoder[FailResponse]])
        resp shouldEqual response
      }
    }

    "ErrorResponse" should {
      "serialize and deserialize" in {
        val resp = Response.errorResponse(List(FormError("test", ErrorCode.invalidField)))
        val json = marshall(resp)
        val response = unmarshall(json, implicitly[Decoder[ErrorResponse[FormError]]])
        resp shouldEqual response
      }
    }

    "DataResponse" should {
      "serialize and deserialize [ObjectData]" in {
        val resp = Response.dataResponse[D](D(1, true), "https://sportadvisor.io")
        val json = marshall(resp)
        val data = unmarshall(json, dataResponseDecoder[D])
        resp shouldEqual data
      }

      "serialize and deserialize [CollectionData]" in {
        val resp = Response.dataResponse[D](List(D(1, true), D(2, false)), (d: D) => s"""https://sportadvisor.io/api/d/${d.int}""",
          "https://sportadvisor.io/api/d?page=3", "https://sportadvisor.io/api/d?page=1", "https://sportadvisor.io/api/d?page=7",
          "https://sportadvisor.io/api/d?page=2", "https://sportadvisor.io/api/d?page=4")
        val json = marshall(resp)
        val data = unmarshall(json, dataResponseDecoder[D])
        resp shouldEqual data
      }
    }
  }

  "Data" when {
    "ObjectData" should {
      "serialize and deserealize" in {
        val data = Response.data[D](D(1, true), "https://sportadvisor.io")
        val json = marshall(data)
        val response = unmarshall(json, implicitly[Decoder[ObjectData[D]]])
        data shouldEqual response
      }
    }

    "CollectionData" should {
      "serialize and deserealize" in {
        val data = Response.data[D](List(D(1, true), D(2, false)), (d: D) => s"""https://sportadvisor.io/api/d/${d.int}""",
          "https://sportadvisor.io/api/d?page=3", "https://sportadvisor.io/api/d?page=1", "https://sportadvisor.io/api/d?page=7",
          "https://sportadvisor.io/api/d?page=2", "https://sportadvisor.io/api/d?page=4")
        val json = marshall(data)

        val response = unmarshall(json, implicitly[Decoder[CollectionData[D]]])
        data shouldEqual response
      }
    }
  }

  private def marshall[T](value: T)(implicit encoder: Encoder[T]): Json = {
    encoder(value)
  }

  private def unmarshall[T](json: Json, decoder: Decoder[T]) : T = {
    decoder(HCursor.fromJson(json)).right.get
  }

  case class D(int: Int, boolean: Boolean)


  implicit val dEncoder: Encoder[D] = deriveEncoder[D]
  implicit val dDecoder: Decoder[D] = deriveDecoder
}
