package io.sportadvisor.http.user

import io.sportadvisor.BaseE2ETest

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait UserMappings { self: BaseE2ETest =>
  def req(s: String): String = to("api/users/" + s)
}
