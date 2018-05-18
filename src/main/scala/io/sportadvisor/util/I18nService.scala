package io.sportadvisor.util

import io.sportadvisor.util.i18n.{I18n, I18nImpl}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait I18nService {

  def errors(lang: String): I18n

  def messages(lang: String): I18n

  def mails(lang: String): I18n

}

trait I18nServiceImpl extends I18nService {

  override def errors(lang: String): I18n = new I18nImpl(lang, "error")

  override def messages(lang: String): I18n = new I18nImpl(lang, "messages")

  override def mails(lang: String): I18n = new I18nImpl(lang, "mails")

}
