package io.sportadvisor.core.sport

import io.sportadvisor.core.sport.SportModels.Sport

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait SportService {

  def find(id: Long): Option[Sport]

}

object StubSportService extends SportService {
  override def find(id: Long): Option[Sport] = ???
}
