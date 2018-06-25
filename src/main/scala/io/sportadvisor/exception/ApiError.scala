package io.sportadvisor.exception

import scala.util.control.NoStackTrace

/**
  * @author sss3 (Vladimir Alekseev)
  */
abstract class ApiError(val msg: String, val error: Option[Throwable])
    extends RuntimeException(msg)
    with NoStackTrace {}
