package io.sportadvisor.util.i18n

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
  * @author sss3 (Vladimir Alekseev)
  */
object I18nModel {

  type I18nMap = Map[Language, String]

  sealed trait Language extends EnumEntry

  object Language extends Enum[Language] {

    private lazy val enums = findValues

    def default: Language = Language.EN

    override def values: immutable.IndexedSeq[Language] = enums

    def find(key: String): Option[Language] = withNameLowercaseOnlyOption(key.toLowerCase)

    def supported: Seq[Language] = values

    case object RU extends Language
    case object EN extends Language
  }

  // scalastyle:off
  object implicits {
    implicit class I18nMapOps(val map: I18nMap) extends AnyVal {

      def orDefault(language: Language): Option[String] = map.get(language).orElse(map.get(Language.default))

      def anyText(): Option[String] = map.headOption.map(_._2)

    }
  }
}
