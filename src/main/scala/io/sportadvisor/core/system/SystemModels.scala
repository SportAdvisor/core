package io.sportadvisor.core.system

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
  * @author sss3 (Vladimir Alekseev)
  */
object SystemModels {

  trait IdEnum extends EnumEntry {
    def id: Int
  }

  sealed trait Currency extends IdEnum

  sealed trait Sex extends IdEnum

  private[system] abstract class AbstractId(val id: Int) extends IdEnum

  object Currency extends Enum[Currency] {
    private lazy val enums = findValues

    override def values: immutable.IndexedSeq[Currency] = enums

    def supported: Seq[Currency] = values

    case object RUR extends AbstractId(1)
    case object USD extends AbstractId(2)
    case object EUR extends AbstractId(3)
  }

  object Sex extends Enum[Sex] {
    private lazy val enums = findValues

    override def values: immutable.IndexedSeq[Sex] = enums

    case object MALE extends AbstractId(1)
    case object FEMALE extends AbstractId(2)
  }

}
