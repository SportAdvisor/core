package io.sportadvisor.util.db

import com.github.tminglei.slickpg.{ExPostgresProfile, PgDate2Support, PgDateSupport}
import enumeratum.SlickEnumSupport
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

/**
  * @author sss3 (Vladimir Alekseev)
  * https://github.com/tminglei/slick-pg
  */
trait SAPostgresProfile
    extends ExPostgresProfile
    with PgDateSupport
    with PgDate2Support
    with SlickEnumSupport {

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api: SAApi.type = SAApi

  object SAApi extends API with DateTimeImplicits {}

}

object SAPostgresProfile extends SAPostgresProfile
