package io.sportadvisor.http


/**
  * @author sss3 (Vladimir Alekseev)
  */
object Response {

  sealed trait Response {
    def code: Int
  }

  sealed trait Data {}

  case class Link(href: String)

  sealed trait Links {
    def self: Link
  }

  sealed trait Error {}

  case class EmptyResponse(code: Int) extends Response
  case class DataResponse(code: Int, data: Data) extends Response
  case class ErrorResponse(code: Int, errors: List[Error]) extends Response
  case class FailResponse(code: Int, message: Option[String]) extends Response

  case class FormError(field: String, code: String) extends Error

  case class ObjectLinks(self: Link) extends Links
  case class CollectionLinks(self: Link, first: Link, last: Link, previous: Link, next: Link) extends Links

  case class ObjectData[A](data: A, _links: ObjectLinks) extends Data
  case class CollectionData[A](data: List[ObjectData[A]], _links: CollectionLinks) extends Data

  def data[A](data: A, self: String) : ObjectData[A] = {
    ObjectData(data, ObjectLinks(self))
  }

  def data[A](data: List[A], selfGenerator: A => String, self: String, first: String,
              last: String, previous: String, next: String) : CollectionData[A] = {
    val objectWrappers = data.map(e => Response.data(e, selfGenerator(e)))
    val _links = CollectionLinks(self, first, last, previous, next)
    CollectionData[A](objectWrappers, _links)
  }

  def emptyResponse(code: Int) : Response = {
    if (code >= 100 && code < 600) {
      EmptyResponse(code)
    } else {
      throw new IllegalStateException("Unsupported code " + code)
    }
  }

  def dataResponse[A](data: A, self: String): Response = {
    val value : ObjectData[A] = Response.data(data, self)
    DataResponse(200, value)
  }

  def dataResponse[A](data: List[A], selfGenerator: A => String, self: String, first: String,
                      last: String, previous: String, next: String) : Response = {
    val value = Response.data(data, selfGenerator, self, first, last, previous, next)
    DataResponse(200, value)
  }

  def errorResponse[E <: Error](errors: List[E]) : Response = {
    ErrorResponse(400, errors)
  }

  def failResponse(message: Option[String] = None) : Response = {
    FailResponse(500, message)
  }

  private[this] implicit def stringToLink(link: String) : Link = Link(link)


}
