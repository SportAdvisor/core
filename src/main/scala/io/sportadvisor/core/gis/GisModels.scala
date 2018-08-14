package io.sportadvisor.core.gis

import io.sportadvisor.util.i18n.I18nModel.I18nMap

/**
  * @author sss3 (Vladimir Alekseev)
  */
object GisModels {

  final case class Coordinate(latitude: Double, longitude: Double)
  final case class Country(id: Long, name: I18nMap, coordinate: Coordinate)
  final case class City(id: Long, name: String, coordinate: Coordinate, countryId: Long)

}
