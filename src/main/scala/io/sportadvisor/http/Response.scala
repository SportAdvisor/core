package io.sportadvisor.http

import akka.http.scaladsl.model.StatusCodes
import org.slf4s.Logging

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Response extends Logging {

  sealed trait Response {
    def code: Int
  }

  sealed trait Data[A] {}

  final case class Link(href: String)

  sealed trait Links {
    def self: Link
  }

  sealed trait Error {}

  final case class EmptyResponse(code: Int) extends Response
  final case class DataResponse[A, D <: Data[A]](code: Int, data: D) extends Response
  final case class ErrorResponse[E <: Error](code: Int, errors: List[E]) extends Response
  final case class FailResponse(code: Int, message: Option[String]) extends Response

  final case class FormError(field: String, msg: String) extends Error

  final case class ObjectLinks(self: Link) extends Links
  final case class CollectionLinks(self: Link, first: Link, last: Link, previous: Link, next: Link)
      extends Links

  final case class ObjectData[A](data: A, _links: Option[ObjectLinks]) extends Data[A]
  final case class CollectionData[A](data: List[ObjectData[A]], _links: CollectionLinks) extends Data[A]

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

  def emptyResponse(code: Int): Response = {
    if (code >= 100 && code < 600) {
      EmptyResponse(code)
    } else {
      log.warn(s"undefined code $code")
      failResponse(None)
    }
  }

  def objectResponse[A](data: A, self: Option[String]): Response = {
    val value: ObjectData[A] = Response.objectData(data, self)
    DataResponse[A, ObjectData[A]](StatusCodes.OK.intValue, value)
  }

  def collectionResponse[A](data: List[A],
                            selfGenerator: A => String,
                            self: String,
                            first: String,
                            last: String,
                            previous: String,
                            next: String): Response = {
    val value = Response.collectionData(data, selfGenerator, self, first, last, previous, next)
    DataResponse[A, CollectionData[A]](StatusCodes.OK.intValue, value)
  }

  def errorResponse[E <: Error](errors: List[E]): ErrorResponse[E] = {
    ErrorResponse[E](StatusCodes.BadRequest.intValue, errors)
  }

  def failResponse(message: Option[String]): Response = {
    FailResponse(StatusCodes.InternalServerError.intValue, message)
  }

  private[this] implicit def stringToLink(link: String): Link = Link(link)

}
