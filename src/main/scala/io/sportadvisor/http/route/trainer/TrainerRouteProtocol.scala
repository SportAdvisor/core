package io.sportadvisor.http.route.trainer


import java.time.LocalDate

import io.circe.generic.semiauto._
import io.circe.Decoder
import io.sportadvisor.core.gis.GisModels.Coordinate

/**
  * @author sss3 (Vladimir Alekseev)
  */
object TrainerRouteProtocol {

  final case class CreateTrainer(about: String, alias: String, birthday: LocalDate,
                                 sex: Int, sports: List[Int], groupTraining: Option[GroupTraining],
                                 personalTraining: Option[PersonalTraining], contacts: Contacts, workWithChild: Boolean,
                                 workWithInjured: Boolean, workWithAdults: Boolean, country: Place, city: Place,
                                 places: List[Place])


  final case class PersonalTraining(support: Boolean, price: Double, currency: Int)
  final case class GroupTraining(support: Boolean, price: Double, currency: Int, minPeople: Option[Int], maxPeople: Option[Int])
  final case class Contacts(phone: Option[String], vk: Option[String], facebook: Option[String], instagram: Option[String])
  final case class Place(name: String, coordinate: Coordinate)

  implicit val personalTrainingDecoder: Decoder[PersonalTraining] = deriveDecoder
  implicit val groupTrainingDecoder: Decoder[GroupTraining] = deriveDecoder
  implicit val contactsDecoder: Decoder[Contacts] = deriveDecoder
  implicit val createTrainerDecoder: Decoder[CreateTrainer] = deriveDecoder
}
