package io.sportadvisor.typeclass

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait Isomorphism[A, B] {

  def map(a: A): B

  def unmap(b: B): A

}

object Isomorphism {

  def apply[A, B]()(implicit iso: Isomorphism[A, B]): Isomorphism[A, B] = iso

}
