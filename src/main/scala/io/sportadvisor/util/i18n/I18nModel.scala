package io.sportadvisor.util.i18n

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
  * @author sss3 (Vladimir Alekseev)
  */
object I18nModel {

  type I18nMap[T] = Map[Language, T]

  sealed trait Language extends EnumEntry

  object Language extends Enum[Language] {

    def default: Language = Language.EN

    override def values: immutable.IndexedSeq[Language] = findValues

    def find(key: String): Option[Language] = withNameLowercaseOnlyOption(key.toLowerCase)

    case object RU extends Language
    case object EN extends Language
  }
}
