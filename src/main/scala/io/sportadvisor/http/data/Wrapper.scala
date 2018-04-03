package io.sportadvisor.http.data

import io.sportadvisor.http.data.Wrapper.Status.Status

/**
  * @author sss3 (Vladimir Alekseev)
  */
object Wrapper {

  sealed trait Wrapper[A, L <: Links] {
    def data: A
    def _links: L
  }

  sealed trait Links {
    def self: Link
  }

  case class ObjectWrapper[A](data: A, _links: ObjectLinks) extends Wrapper[A, ObjectLinks]

  case class ObjectLinks(self: Link) extends Links

  case class CollectionWrapper[A](data: List[ObjectWrapper[A]], _links: CollectionLinks) extends Wrapper[List[A], CollectionLinks]

  case class CollectionLinks(self: Link, first: Link, last: Link, previous: Link, next: Link) extends Links

  case class Link(href: String)

  case class Response[A, L<: Links, D <: Wrapper[A, L]](code: Int, status: Status, message: Option[String], data: D)

  object Status extends Enumeration {
    type Status = Value
    val success, fail, error = Value
  }

  def wrap[A](data: A, self: String) : ObjectWrapper[A] = {
    ObjectWrapper(data, ObjectLinks(self))
  }

  def wrap[A](data: List[A], selfGenerator: A => String, self: String, first: String,
              last: String, previous: String, next: String) : CollectionWrapper[A] = {
    val objectWrappers = data.map(e => wrap(e, selfGenerator(e)))
    val _links = CollectionLinks(self, first, last, previous, next)
    CollectionWrapper(objectWrappers, _links)
  }

  def response[A, L<: Links, D <: Wrapper[A, L]](data: D) : Response[A, L, D] = {
    response(data)
  }

  def response[A, L<: Links, D <: Wrapper[A, L]](data: D, code: Int = 200, status: Status = Status.success,
                                                 message: Option[String] = None) : Response[A, L, D] = {
    Response(code, status, message, data)
  }

  private[this] implicit def stringToLink(link: String) : Link = Link(link)
}
