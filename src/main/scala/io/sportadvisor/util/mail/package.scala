package io.sportadvisor.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object mail {

  def dateToString(date: LocalDateTime): String = DateTimeFormatter.ISO_DATE.format(date)

}
