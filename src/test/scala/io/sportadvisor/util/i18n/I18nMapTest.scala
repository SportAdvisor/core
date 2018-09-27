package io.sportadvisor.util.i18n

import io.sportadvisor.BaseTest
import io.sportadvisor.util.i18n.I18nModel.{I18nMap, Language}
import io.sportadvisor.util.i18n.I18nModel.implicits._

/**
  * @author sss3 (Vladimir Alekseev)
  */
class I18nMapTest extends BaseTest {
  "I18nMap implicit helper orDefault(lang)" should {
    "return default lang if it specified" in new EnContext {
      val maybeString: Option[String] = i18nMap.orDefault(Language.RU)
      maybeString.isDefined shouldBe true
      maybeString.get shouldBe "ENG"
    }

    "return called lang if it specified" in new RuContext {
      val maybeString: Option[String] = i18nMap.orDefault(Language.RU)
      maybeString.isDefined shouldBe true
      maybeString.get shouldBe "RUS"
    }

    "return none if called and default lang not specified" in new EmptyContext {
      val maybeString: Option[String] = i18nMap.orDefault(Language.EN)
      maybeString.isDefined shouldBe false
    }
  }

  "I18nMap implicit helper anyText()" should {
    "return any test if map not empty" in new EnContext {
      val maybeString: Option[String] = i18nMap.anyText()
      maybeString.isDefined shouldBe true
    }

    "return none if map is empty" in new EmptyContext {
      val maybeString: Option[String] = i18nMap.anyText()
      maybeString.isDefined shouldBe false
    }
  }

  trait RuContext {
    val i18nMap: I18nMap = Map(Language.RU -> "RUS")
  }

  trait EnContext {
    val i18nMap: I18nMap = Map(Language.EN -> "ENG")
  }

  trait EmptyContext {
    val i18nMap: I18nMap = Map.empty[Language, String]
  }

}
