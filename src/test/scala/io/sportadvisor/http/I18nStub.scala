package io.sportadvisor.http

import io.sportadvisor.util.I18nService
import io.sportadvisor.util.i18n.I18n
import io.sportadvisor.util.i18n.I18nModel.Language

/**
  * @author sss3 (Vladimir Alekseev)
  */
object I18nStub extends I18nService {

  override def errors(lang: Language): I18n = new StubI18n(lang)

  override def messages(lang: Language): I18n = new StubI18n(lang)

  override def mails(lang: Language): I18n = new StubI18n(lang)

}

class StubI18n(val language: Language) extends I18n {
  override protected def po: scaposer.I18n = new scaposer.I18n(Map.empty)

  override def bundle: String = "stub"
}
