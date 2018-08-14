package io.sportadvisor.core.system

import io.sportadvisor.core.system.SystemModels.Currency
import io.sportadvisor.util.i18n.I18nModel.Language

/**
  * @author sss3 (Vladimir Alekseev)
  */
object SystemService {
  def supportedLanguage(): Seq[String] = Language.supported.map(_.entryName.toLowerCase)

  def supportedCurrency(): Map[Int, String] = Currency.supported.map(c => (c.id, c.entryName)).toMap

}
