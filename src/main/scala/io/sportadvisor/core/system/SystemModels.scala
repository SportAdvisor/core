package io.sportadvisor.core.system

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable
/**
  * @author sss3 (Vladimir Alekseev)
  */
object SystemModels {

  sealed trait Currency extends EnumEntry {
    def id: Int
  }

  private abstract class AbstractCurrency(val id: Int) extends Currency {
  }

  object Currency extends Enum[Currency] {
    private lazy val enums = findValues

    override def values: immutable.IndexedSeq[Currency] = enums

    def supported: Seq[Currency] = values

    case object RUR extends AbstractCurrency(1)
    case object USD extends AbstractCurrency(2)
    case object EUR extends AbstractCurrency(3)
  }

}
