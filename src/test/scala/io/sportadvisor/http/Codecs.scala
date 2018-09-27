package io.sportadvisor.http

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.HCursor.fromJson
import io.circe.generic.extras.AutoDerivation
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.sportadvisor.core.gis.GisModels.Coordinate
import io.sportadvisor.core.trainer.TrainerModels._
import io.sportadvisor.http.Response._
import io.sportadvisor.http.route.user.UserRouteProtocol.UserView

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Codecs extends AutoDerivation {

  import io.circe.java8.time._

  implicit val emptyResponseDecoder: Decoder[EmptyResponse] = deriveDecoder[EmptyResponse]
  implicit val failResponseDecoder: Decoder[FailResponse] = deriveDecoder[FailResponse]
  implicit val fieldFormErrorDecoder: Decoder[FieldFormError] = deriveDecoder[FieldFormError]
  implicit val formErrorDecoder: Decoder[FormError] = deriveDecoder
  implicit val errorDecoder: Decoder[Error] = (c: HCursor) => {
    if (c.downField("field").succeeded) {
      fieldFormErrorDecoder(c)
    } else {
      formErrorDecoder(c)
    }
  }
  implicit final def errorResponseDecoder[E <: Error](implicit e: Decoder[E]): Decoder[ErrorResponse[E]] =
    (c: HCursor) => {
      c.value.asObject.map { obj =>
        val code = obj("code").map(Decoder[Int].decodeJson).orNull.right.get
        val errors =
          obj("errors").map(v => Decoder.decodeVector[E].decodeJson(v)).orNull.right.get.toList
        Right(ErrorResponse[E](code, errors))
      }.orNull
    }

  implicit val linkDecoder: Decoder[Link] = deriveDecoder
  implicit val objectLinksDecoder: Decoder[ObjectLinks] = deriveDecoder
  implicit val collectionLinksDecoder: Decoder[CollectionLinks] = deriveDecoder

  implicit final def objectDataDecoder[A](implicit d: Decoder[A]): Decoder[ObjectData[A]] =
    (c: HCursor) => {
      c.value.asObject.map { obj =>
        val data = obj("data").map(data => d(fromJson(data))).orNull.right.get
        val links =
          obj("_links").map(links => objectLinksDecoder(fromJson(links))).orNull.right.toOption
        Right(ObjectData[A](data, links))
      }.orNull
    }

  implicit final def collectionDataDecoder[A](implicit d: Decoder[A]): Decoder[CollectionData[A]] =
    (c: HCursor) => {
      c.value.asObject.map { obj =>
        val links =
          obj("_links").map(links => collectionLinksDecoder(fromJson(links))).orNull.right.get
        val data = obj("data")
          .map(data => Decoder.decodeList[ObjectData[A]].apply(fromJson(data)))
          .orNull
          .right
          .get
        Right(CollectionData[A](data, links))
      }.orNull
    }

  implicit final def dataDecoder[A](implicit d: Decoder[A]): Decoder[Data[A]] = (c: HCursor) => {
    c.value.asObject.map { data =>
      val isCollection = data("data").exists(j => j.isArray)
      if (isCollection) {
        collectionDataDecoder[A](d).apply(c)
      } else {
        objectDataDecoder[A](d).apply(c)
      }
    }.orNull
  }

  implicit final def dataResponseDecoder[A, D <: Data[A]](
      implicit decoder: Decoder[A]): Decoder[DataResponse[A, D]] = (c: HCursor) => {
    val res = c.value.asObject.map { obj =>
      val code = obj("code").map(c => c.as[Int]).orNull.right.get
      val data = obj("data").map(d => dataDecoder[A](decoder).apply(fromJson(d))).orNull.right.get
      DataResponse[A, D](code, data.asInstanceOf[D])
    }.orNull
    Right(res)
  }

  implicit val userViewDecoder: Decoder[UserView] = deriveDecoder

  implicit val groupTrainingEncoder: Encoder[GroupTraining] = deriveEncoder
  implicit val personalTrainingEncoder: Encoder[PersonalTraining] = deriveEncoder
  implicit val coordinateEncoder: Encoder[Coordinate] = deriveEncoder
  implicit val placeEncoder: Encoder[Place] = deriveEncoder
  implicit val contactEncoder: Encoder[Contacts] = deriveEncoder
  implicit val createTrainerEncoder: Encoder[CreateTrainer] = deriveEncoder
}
