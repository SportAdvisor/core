package io.sportadvisor.http

import io.sportadvisor.util.i18n.I18n

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait I18nStub extends I18nService {

  override def errors(lang: String): I18n = new StubI18n(lang)

  override def messages(lang: String): I18n = new StubI18n(lang)

}

class StubI18n(val language: String) extends I18n {
  override protected def po: scaposer.I18n = new scaposer.I18n(Map.empty)

  override def bundle: String = "stub"
}
