package io.sportadvisor.http

import io.circe.HCursor.fromJson
import io.sportadvisor.http.Response._
import io.sportadvisor.http.Response.Error
import io.circe._
import io.circe.generic.extras.AutoDerivation

/**
  * @author sss3 (Vladimir Alekseev)
  */
object json extends AutoDerivation {

  import io.circe.generic.semiauto._

  implicit val formErrorEncoder: Encoder[FormError] = deriveEncoder[FormError]
  implicit val errorEncoder: Encoder[Error] = {
    case e@FormError(_, _) => formErrorEncoder(e)
  }

  implicit val emptyResponseEncoder: Encoder[EmptyResponse] = deriveEncoder[EmptyResponse]
  implicit val failResponseEncoder: Encoder[FailResponse] = deriveEncoder[FailResponse]
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]

  implicit val linkEncoder: Encoder[Link] = deriveEncoder
  implicit val objectLinksEncoder: Encoder[ObjectLinks] = deriveEncoder
  implicit val collectionLinksEncoder: Encoder[CollectionLinks] = deriveEncoder

  implicit final def encoderObjectData[A](implicit e: Encoder[A]): Encoder[ObjectData[A]] = (a: ObjectData[A]) => {
    Json.obj("data" -> e(a.data), "_links" -> objectLinksEncoder(a._links))
  }

  implicit final def encoderCollectionData[A](implicit e: Encoder[A]): Encoder[CollectionData[A]] = (a: CollectionData[A]) => {
    Json.obj("data" -> Encoder.encodeList[ObjectData[A]].apply(a.data), "_links" -> collectionLinksEncoder(a._links))
  }

  implicit final def dataEncoder[A](implicit e: Encoder[A]): Encoder[Data] = {
    case d@ObjectData(_, _) => encoderObjectData[A](e)(d.asInstanceOf[ObjectData[A]])
    case d@CollectionData(_, _) => encoderCollectionData[A](e)(d.asInstanceOf[CollectionData[A]])
  }

  implicit final def encoder[A](implicit e: Encoder[A]): Encoder[Response] = {
    case r@EmptyResponse(_) => emptyResponseEncoder(r)
    case r@FailResponse(_, _) => failResponseEncoder(r)
    case r@ErrorResponse(_,_) => errorResponseEncoder(r)
    case r@DataResponse(_, _) => Json.obj("code" -> Json.fromInt(r.code),
      "data" -> dataEncoder[A](e).apply(r.data))
  }


  /*
   Decoder need for tests
   */
  implicit val emptyResponseDecoder: Decoder[EmptyResponse] = deriveDecoder[EmptyResponse]
  implicit val failResponseDecoder: Decoder[FailResponse] = deriveDecoder[FailResponse]
  implicit val formErrorDecoder: Decoder[FormError] = deriveDecoder[FormError]
  implicit val errorDecoder: Decoder[Error] = (c: HCursor) => formErrorDecoder(c)
  implicit val errorResponseDecoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  implicit val linkDecoder: Decoder[Link] = deriveDecoder
  implicit val objectLinksDecoder: Decoder[ObjectLinks] = deriveDecoder
  implicit val collectionLinksDecoder: Decoder[CollectionLinks] = deriveDecoder

  implicit final def objectDataDecoder[A](implicit d: Decoder[A]): Decoder[ObjectData[A]] = (c: HCursor) => {
    c.value.asObject.map {obj =>
      val data = obj("data").map(data => d(fromJson(data))).orNull.right.get
      val links = obj("_links").map(links => objectLinksDecoder(fromJson(links))).orNull.right.get
      Right(ObjectData[A](data, links))
    }.orNull
  }

  implicit final def collectionDataDecoder[A](implicit d: Decoder[A]): Decoder[CollectionData[A]] = (c: HCursor) => {
    c.value.asObject.map {obj =>
      val links = obj("_links").map(links => collectionLinksDecoder(fromJson(links))).orNull.right.get
      val data = obj("data").map(data => Decoder.decodeList[ObjectData[A]].apply(fromJson(data))).orNull.right.get
      Right(CollectionData[A](data, links))
    }.orNull
  }

  implicit final def dataDecoder[A](implicit d: Decoder[A]): Decoder[Data] = (c: HCursor) => {
    c.value.asObject.map { data =>
      val isCollection = data("data").exists(j => j.isArray)
      if (isCollection) {
        collectionDataDecoder[A](d).apply(c)
      } else {
        objectDataDecoder[A](d).apply(c)
      }
    }.orNull
  }

  implicit final def dataResponseDecoder[A](implicit decoder: Decoder[A]): Decoder[DataResponse] = (c: HCursor) => {
    val res = c.value.asObject.map { obj =>
      val code = obj("code").map(c => c.as[Int]).orNull.right.get
      val data = obj("data").map(d => dataDecoder[A](decoder).apply(fromJson(d))).orNull.right.get
      DataResponse(code, data)
    }.orNull
    Right(res)
  }

}
