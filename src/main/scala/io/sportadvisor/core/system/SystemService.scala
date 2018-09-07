package io.sportadvisor.core.system

import io.sportadvisor.core.system.SystemModels.{Currency, Sex}
import io.sportadvisor.util.i18n.I18nModel.Language

/**
  * @author sss3 (Vladimir Alekseev)
  */
object SystemService {
  def supportedLanguage(): Map[String, String] =
    Language.supported.map(l => (l.entryName.toLowerCase, l.name)).toMap

  def supportedCurrency(): Map[Int, String] = Currency.supported.map(c => (c.id, c.entryName)).toMap

  def supportedSex(): Map[Int, String] = Sex.values.map(s => (s.id, s.entryName)).toMap
}
