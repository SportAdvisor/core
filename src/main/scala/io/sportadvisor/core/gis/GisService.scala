package io.sportadvisor.core.gis

import io.sportadvisor.core.gis.GisModels.{City, Coordinate, Country}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.util.i18n.I18nModel.Language

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait GisService {

  def createCountryIfAbsent(name: String,
                            lang: Language,
                            coordinate: Coordinate): Future[Either[ApiError, Country]]

  def createCityIfAbsent(name: String,
                         lang: Language,
                         coordinate: Coordinate,
                         countryId: Long): Future[Either[ApiError, City]]
}

object StubGisService extends GisService {
  override def createCountryIfAbsent(name: String,
                                     lang: Language,
                                     coordinate: Coordinate): Future[Either[ApiError, Country]] = ???

  override def createCityIfAbsent(name: String,
                                  lang: Language,
                                  coordinate: Coordinate,
                                  countryId: Long): Future[Either[ApiError, City]] = ???
}
