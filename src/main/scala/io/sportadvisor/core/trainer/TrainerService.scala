package io.sportadvisor.core.trainer

import io.sportadvisor.core.trainer.TrainerModels.{CreateTrainer, Trainer}
import io.sportadvisor.exception.ApiError

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TrainerService {

  def create(trainer: CreateTrainer, ownerId: Long): Future[Either[ApiError, Trainer]]

}

object StubTrainerService extends TrainerService {
  override def create(trainer: CreateTrainer, ownerId: Long): Future[Either[ApiError, Trainer]] = ???
}
