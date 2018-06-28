package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.{RefreshToken, UserID}
import io.sportadvisor.util.db.DatabaseConnector
import slick.lifted.{PrimaryKey, ProvenShape}

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[user] trait TokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  // scalastyle:off
  class TokenScheme(tag: Tag) extends Table[RefreshToken](tag, "REFRESH_TOKENS") {
    def userId: Rep[UserID] = column[Long]("user_id")
    def token: Rep[String] = column[String]("token", O.Length(255))
    def remember: Rep[Boolean] = column[Boolean]("remember")
    def lastTouch: Rep[LocalDateTime] = column[LocalDateTime]("last_touch")

    override def * : ProvenShape[RefreshToken] =
      (userId, token, remember, lastTouch) <> (RefreshToken.tupled, RefreshToken.unapply)

    def pk: PrimaryKey = primaryKey("pk_refresh_tokens", (userId, token))
  }
  // scalastyle:on

  protected val tokens = TableQuery[TokenScheme]

}
