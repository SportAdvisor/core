package io.sportadvisor.util

import io.sportadvisor.util.i18n.I18nModel.Language
import io.sportadvisor.util.i18n.{I18n, I18nImpl}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait I18nService {

  def errors(lang: Language): I18n

  def messages(lang: Language): I18n

  def mails(lang: Language): I18n

}

object I18nServiceImpl extends I18nService {

  override def errors(lang: Language): I18n = new I18nImpl(lang, "error")

  override def messages(lang: Language): I18n = new I18nImpl(lang, "messages")

  override def mails(lang: Language): I18n = new I18nImpl(lang, "mails")

}
