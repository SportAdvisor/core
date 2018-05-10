package io.sportadvisor.util.i18n

import java.util.Locale

/**
  * @author sss3 (Vladimir Alekseev)
  */
@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
trait I18n {

  def language: String

  /**
    * The language should have a corresponding file
    * `i18n/bundle_language.po` in classpath (the language should be in IETF BCP 47 format).
    */
  def bundle: String

  /** The locale corresponding to the [[language]]. It will be updated automatically when you update [[language]]. */
  def locale: Locale = Locale.forLanguageTag(language)

  def t(singular: String): String = po.t(singular)

  def tc(ctx: String, singular: String): String = po.tc(ctx, singular)

  def tn(singular: String, plural: String, n: Long): String = po.tn(singular, plural, n)

  def tcn(ctx: String, singular: String, plural: String, n: Long): String =
    po.tcn(ctx, singular, plural, n)

  /** `formatLocal` using the current locale. */
  def t(singular: String, args: Any*): String = po.t(singular).formatLocal(locale, args: _*)

  /** `formatLocal` using the current locale. */
  def tc(ctx: String, singular: String, args: Any*): String =
    po.tc(ctx, singular).formatLocal(locale, args: _*)

  /** `formatLocal` using the current locale. */
  def tn(singular: String, plural: String, n: Long, args: Any*): String =
    po.tn(singular, plural, n).formatLocal(locale, args: _*)

  /** `formatLocal` using the current locale. */
  def tcn(ctx: String, singular: String, plural: String, n: Long, args: Any*): String =
    po.tcn(ctx, singular, plural, n).formatLocal(locale, args: _*)

  protected def po: scaposer.I18n
}

class I18nImpl(val language: String, val bundle: String) extends I18n {

  override protected def po: scaposer.I18n = PoLoader.get(language, bundle)

}
