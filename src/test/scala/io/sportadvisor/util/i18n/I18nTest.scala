package io.sportadvisor.util.i18n

import io.sportadvisor.BaseTest

/**
  * @author sss3 (Vladimir Alekseev)
  */
class I18nTest extends BaseTest {
  "I18n ru" should {
    "t(language) return Русский" in new RuContext {
      i18n.t("language") shouldEqual "Русский"
    }

    "tс(Casual, language) return Русский" in new RuContext {
      i18n.tc("Casual", "language") shouldEqual "Рашн"
    }

    "tn(I have one apple, 1) return `Я имею 1 яблоко`" in new RuContext {
      i18n.tn("I have one apple", "I have %d apples", 1) shouldEqual "Я имею 1 яблоко"
    }

    "tn(I have one apple, 2) return `Я имею несколько(%d) яблок`" in new RuContext {
      i18n.tn("I have one apple", "I have %d apples", 2) shouldEqual "Я имею несколько(%d) яблок"
    }

    "tn(I have one apple, 2, 2) return `Я имею несколько(2) яблок`" in new RuContext {
      i18n.tn("I have one apple", "I have %d apples", 2, 2) shouldEqual "Я имею несколько(2) яблок"
    }
  }

  "I18n en" should {
    "t(language) return English" in new EnContext {
      i18n.t("language") shouldEqual "English"
    }
  }

  "I18n undefined" should {
    "t(language) return English" in new UndefinedContext {
      i18n.t("language") shouldEqual "language"
    }
  }

  trait RuContext {
    val i18n = new I18nImpl("ru", "test")
  }

  trait EnContext {
    val i18n = new I18nImpl("en", "test")
  }

  trait UndefinedContext {
    val i18n = new I18nImpl("it", "test")
  }
}
