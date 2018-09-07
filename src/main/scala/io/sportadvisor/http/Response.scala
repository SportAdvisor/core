package io.sportadvisor.http

import akka.http.scaladsl.model.StatusCodes
import io.circe.{Encoder, Json}
import io.circe.generic.extras.AutoDerivation
import org.slf4s.Logging

import scala.language.implicitConversions

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Response extends Logging {

  sealed trait Response[A] {
    def code: Int
  }

  sealed trait Data[A] {}

  final case class Link(href: String)

  sealed trait Links {
    def self: Link
  }

  sealed trait Error {}

  final case class EmptyResponse(code: Int) extends Response[Unit]
  final case class DataResponse[A, D <: Data[A]](code: Int, data: D) extends Response[A]
  final case class ErrorResponse[E <: Error](code: Int, errors: List[E]) extends Response[E]
  final case class FailResponse(code: Int, message: Option[String]) extends Response[Unit]

  final case class FieldFormError(field: String, msg: String) extends Error
  final case class FormError(msg: String) extends Error

  final case class ObjectLinks(self: Link) extends Links
  final case class CollectionLinks(self: Link, first: Link, last: Link, previous: Link, next: Link)
      extends Links

  final case class ObjectData[A](data: A, links: Option[ObjectLinks]) extends Data[A]
  final case class CollectionData[A](data: List[ObjectData[A]], links: CollectionLinks) extends Data[A]

  def objectData[A](data: A, self: Option[String]): ObjectData[A] = {
    ObjectData(data, self.map(ObjectLinks(_)))
  }

  def collectionData[A](data: List[A],
                        selfGenerator: A => String,
                        self: String,
                        first: String,
                        last: String,
                        previous: String,
                        next: String): CollectionData[A] = {
    val objectWrappers = data.map(e => Response.objectData(e, Some(selfGenerator(e))))
    val _links = CollectionLinks(self, first, last, previous, next)
    CollectionData[A](objectWrappers, _links)
  }

  def empty(code: Int): Response[Unit] = {
    if (code >= 100 && code < 600) {
      EmptyResponse(code)
    } else {
      log.warn(s"undefined code $code")
      fail(None)
    }
  }

  def data[A](data: A, self: Option[String]): Response[A] = {
    val value: ObjectData[A] = Response.objectData(data, self)
    DataResponse[A, ObjectData[A]](StatusCodes.OK.intValue, value)
  }

  def collection[A](data: List[A],
                    selfGenerator: A => String,
                    self: String,
                    first: String,
                    last: String,
                    previous: String,
                    next: String): Response[A] = {
    val value = Response.collectionData(data, selfGenerator, self, first, last, previous, next)
    DataResponse[A, CollectionData[A]](StatusCodes.OK.intValue, value)
  }

  def error[E <: Error](errors: List[E]): Response[E] = {
    ErrorResponse[E](StatusCodes.BadRequest.intValue, errors)
  }

  def fail(message: Option[String]): Response[Unit] = {
    FailResponse(StatusCodes.InternalServerError.intValue, message)
  }

  private[this] implicit def stringToLink(link: String): Link = Link(link)

  trait Encoders extends AutoDerivation {

    import io.circe.generic.semiauto._

    implicit val fieldFormErrorEncoder: Encoder[FieldFormError] = deriveEncoder[FieldFormError]
    implicit val formErrorEncoder: Encoder[FormError] = deriveEncoder
    implicit val errorEncoder: Encoder[Error] = {
      case e: FieldFormError => fieldFormErrorEncoder(e)
      case e: FormError      => formErrorEncoder(e)
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
        Json.obj("data" -> e(a.data), "_links" -> Encoder.encodeOption[ObjectLinks].apply(a.links))
      }

    implicit final def encoderCollectionData[A](implicit e: Encoder[A]): Encoder[CollectionData[A]] =
      (a: CollectionData[A]) => {
        Json.obj("data" -> Encoder.encodeList[ObjectData[A]].apply(a.data),
                 "_links" -> collectionLinksEncoder(a.links))
      }

    implicit final def dataEncoder[A](implicit e: Encoder[A]): Encoder[Data[A]] = {
      case d: ObjectData[A]     => encoderObjectData[A](e)(d)
      case d: CollectionData[A] => encoderCollectionData[A](e)(d)
    }

    implicit final def dataResponseEncoder[A](implicit e: Encoder[A]): Encoder[DataResponse[A, Data[A]]] =
      (a: DataResponse[A, Data[A]]) => {
        Json.obj("code" -> Json.fromInt(a.code), "data" -> dataEncoder[A].apply(a.data))
      }

    implicit final def encoder[A](implicit e: Encoder[A]): Encoder[Response[A]] = {
      case r: EmptyResponse         => emptyResponseEncoder(r)
      case r: FailResponse          => failResponseEncoder(r)
      case ErrorResponse(code, err) => errorResponseEncoder(errorEncoder)(ErrorResponse(code, err))
      case DataResponse(code, data) => dataResponseEncoder[A].apply(DataResponse(code, data))
    }

  }
}
