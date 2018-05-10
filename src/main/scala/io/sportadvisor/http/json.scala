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
    case e @ FormError(_, _) => formErrorEncoder(e)
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

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit final def dataEncoder[A](implicit e: Encoder[A]): Encoder[Data[A]] = {
    case d @ ObjectData(_, _)     => encoderObjectData[A](e)(d.asInstanceOf[ObjectData[A]])
    case d @ CollectionData(_, _) => encoderCollectionData[A](e)(d.asInstanceOf[CollectionData[A]])
  }

  implicit final def dataResponseEncoder[A](implicit e: Encoder[A]): Encoder[DataResponse[A, _]] =
    (a: DataResponse[A, _]) => {
      Json.obj("code" -> Json.fromInt(a.code),
               "data" -> dataEncoder[A].apply(a.data.asInstanceOf[Data[A]]))
    }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit final def encoder[A, D <: Data[A]](implicit e: Encoder[A]): Encoder[Response] = {
    case r @ EmptyResponse(_)    => emptyResponseEncoder(r)
    case r @ FailResponse(_, _)  => failResponseEncoder(r)
    case r @ ErrorResponse(_, _) => errorResponseEncoder(errorEncoder)(r)
    case r @ DataResponse(_, _) =>
      dataResponseEncoder[A](e).apply(r.asInstanceOf[DataResponse[A, Data[A]]])
  }

  object Codecs {

    import io.circe.java8.time._

    implicit val authTokenEncoder: Encoder[AuthToken] = deriveEncoder
    implicit val authTokenDecoder: Decoder[AuthToken] = deriveDecoder
  }

}
