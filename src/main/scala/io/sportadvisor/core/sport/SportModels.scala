package io.sportadvisor.core.sport

import io.sportadvisor.util.i18n.I18nModel.I18nMap

/**
  * @author sss3 (Vladimir Alekseev)
  */
object SportModels {

  final case class Sport(id: Long, value: I18nMap)
}
