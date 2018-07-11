package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.{RefreshTokenData, UserID}
import io.sportadvisor.util.db.DatabaseConnector
import slick.lifted.ProvenShape

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[user] trait AuthTokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  // scalastyle:off
  class TokenScheme(tag: Tag) extends Table[RefreshTokenData](tag, "REFRESH_TOKENS") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId: Rep[UserID] = column[Long]("user_id")
    def token: Rep[String] = column[String]("token", O.Length(255))
    def remember: Rep[Boolean] = column[Boolean]("remember")
    def lastTouch: Rep[LocalDateTime] = column[LocalDateTime]("last_touch")

    override def * : ProvenShape[RefreshTokenData] =
      (id, userId, token, remember, lastTouch) <> (RefreshTokenData.tupled, RefreshTokenData.unapply)

  }
  // scalastyle:on

  protected val tokens = TableQuery[TokenScheme]

}