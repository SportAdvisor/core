package io.sportadvisor.http.route.trainer

import io.circe.generic.semiauto._
import io.circe.Decoder
import io.sportadvisor.core.gis.GisModels.Coordinate
import io.sportadvisor.core.trainer.TrainerModels._

/**
  * @author sss3 (Vladimir Alekseev)
  */
object TrainerRouteProtocol {

  import io.circe.java8.time._

  implicit val personalTrainingDecoder: Decoder[PersonalTraining] = deriveDecoder
  implicit val groupTrainingDecoder: Decoder[GroupTraining] = deriveDecoder
  implicit val contactsDecoder: Decoder[Contacts] = deriveDecoder
  implicit val coordinateDecoder: Decoder[Coordinate] = deriveDecoder
  implicit val placeDecoder: Decoder[Place] = deriveDecoder
  implicit val createTrainerDecoder: Decoder[CreateTrainer] = deriveDecoder
}
