package io.sportadvisor.http

import io.sportadvisor.http.Response._
import io.sportadvisor.http.Response.Error
import io.circe._
import io.circe.generic.extras.AutoDerivation
import io.sportadvisor.core.user.AuthToken

/**
  * @author sss3 (Vladimir Alekseev)
  */
// scalastyle:off object.name
object json extends AutoDerivation {

  import io.circe.generic.semiauto._

  implicit val formErrorEncoder: Encoder[FormError] = deriveEncoder[FormError]
  implicit val errorEncoder: Encoder[Error] = {
    case e: FormError => formErrorEncoder(e)
  }

  implicit val emptyResponseEncoder: Encoder[EmptyResponse] = deriveEncoder[EmptyResponse]
  implicit val failResponseEncoder: Encoder[FailResponse] = deriveEncoder[FailResponse]

  implicit final def errorResponseEncoder[E <: Error](
      implicit encoder: Encoder[E]): Encoder[ErrorResponse[E]] = (a: ErrorResponse[E]) => {
    Json.obj("code" -> Json.fromInt(a.code), "errors" -> Encoder.encodeList[E].apply(a.errors))
  }

  implicit val linkEncoder: Encoder[Link] = deriveEncoder
  implicit val objectLinksEncoder: Encoder[ObjectLinks] = deriveEncoder
  implicit val collectionLinksEncoder: Encoder[CollectionLinks] = deriveEncoder

  implicit final def encoderObjectData[A](implicit e: Encoder[A]): Encoder[ObjectData[A]] =
    (a: ObjectData[A]) => {
      Json.obj("data" -> e(a.data), "_links" -> Encoder.encodeOption[ObjectLinks].apply(a._links))
    }

  implicit final def encoderCollectionData[A](implicit e: Encoder[A]): Encoder[CollectionData[A]] =
    (a: CollectionData[A]) => {
      Json.obj("data" -> Encoder.encodeList[ObjectData[A]].apply(a.data),
               "_links" -> collectionLinksEncoder(a._links))
    }

  implicit final def dataEncoder[A](implicit e: Encoder[A]): Encoder[Data[A]] = {
    case d: ObjectData[A]     => encoderObjectData[A](e)(d)
    case d: CollectionData[A] => encoderCollectionData[A](e)(d)
  }

  implicit final def dataResponseEncoder[A](
      implicit e: Encoder[A]): Encoder[DataResponse[A, Data[A]]] =
    (a: DataResponse[A, Data[A]]) => {
      Json.obj("code" -> Json.fromInt(a.code), "data" -> dataEncoder[A].apply(a.data))
    }

  implicit final def encoder[A](implicit e: Encoder[A]): Encoder[Response[A]] = {
    case r: EmptyResponse            => emptyResponseEncoder(r)
    case r: FailResponse             => failResponseEncoder(r)
    case r: ErrorResponse[Error]     => errorResponseEncoder(errorEncoder)(r)
    case r: DataResponse[A, Data[A]] => dataResponseEncoder[A].apply(r)
  }

  object Codecs {

    import io.circe.java8.time._

    implicit val authTokenEncoder: Encoder[AuthToken] = deriveEncoder
    implicit val authTokenDecoder: Decoder[AuthToken] = deriveDecoder
  }

}
// scalastyle:on object.name
