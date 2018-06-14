package io.sportadvisor.http

import io.sportadvisor.BaseE2ETest
import scalaj.http._

/**
  * @author sss3 (Vladimir Alekseev)
  */
class HealthCheckTest extends BaseE2ETest {

  "GET /healthcheck" should "return 200 and OK" in {
    val resp = Http(to("healthcheck")).asString
    resp.code shouldBe 200
    resp.body shouldBe "\"OK\""
  }
}
