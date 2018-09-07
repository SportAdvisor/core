package io.sportadvisor.core.trainer

import java.time.LocalDate

import io.sportadvisor.core.gis.GisModels.Coordinate
import io.sportadvisor.core.system.SystemModels.Sex
import io.sportadvisor.exception.ApiError

/**
  * @author sss3 (Vladimir Alekseev)
  */
object TrainerModels {

  final case class CreateTrainer(about: String,
                                 alias: String,
                                 birthday: LocalDate,
                                 sex: Int,
                                 sports: List[Int],
                                 groupTraining: Option[GroupTraining],
                                 personalTraining: Option[PersonalTraining],
                                 contacts: Contacts,
                                 workWithChild: Boolean,
                                 workWithInjured: Boolean,
                                 workWithAdults: Boolean,
                                 country: Place,
                                 city: Place,
                                 places: List[Place])

  final case class Place(name: String, coordinate: Coordinate)

  final case class PersonalTraining(support: Boolean, price: Double, currency: Int)
  final case class GroupTraining(support: Boolean,
                                 price: Double,
                                 currency: Int,
                                 minPeople: Option[Int],
                                 maxPeople: Option[Int])
  final case class Contacts(phone: Option[String],
                            vk: Option[String],
                            facebook: Option[String],
                            instagram: Option[String])

  final case class Trainer(id: Long,
                           about: String,
                           alias: String,
                           birthday: LocalDate,
                           sex: Sex,
                           sports: List[Int],
                           group: Option[GroupTraining],
                           personal: Option[PersonalTraining],
                           contacts: Contacts,
                           workWithChild: Boolean,
                           workWithInjured: Boolean,
                           workWithAdults: Boolean,
                           countryId: Long,
                           cityId: Long,
                           places: List[Place])

  final case class NotUniqueSports(sportAndPageAliases: Map[Long, String])
      extends ApiError(s"Not unique sports ${sportAndPageAliases.keySet}", None)

  final case class LimitPagesOnUser() extends ApiError("Limit pages on user", None)
}
