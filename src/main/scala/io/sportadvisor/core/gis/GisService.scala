package io.sportadvisor.core.gis

import io.sportadvisor.core.gis.GisModels.{City, Coordinate, Country}
import io.sportadvisor.util.i18n.I18nModel.Language

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
class GisService {

  def createCountryIfAbsent(name: String, lang: Language, coordinate: Coordinate): Future[Country] = ???

  def createCityIfAbsent(name: String, lang: Language, coordinate: Coordinate, countryId: Long): Future[City] = ???
}
