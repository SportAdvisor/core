package io.sportadvisor.core.user.token

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.ChangeMailToken
import io.sportadvisor.util.db.DatabaseConnector
import slick.lifted.ProvenShape

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[token] trait MailTokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  // scalastyle:off
  class MailTokenScheme(tag: Tag) extends Table[ChangeMailToken](tag, "MAIL_TOKENS") {
    def token: Rep[String] = column[String]("token", O.PrimaryKey)
    def expireAt: Rep[LocalDateTime] = column[LocalDateTime]("expire_at")
    def userId: Rep[Long] = column[Long]("user_id")

    override def * : ProvenShape[ChangeMailToken] =
      (token, expireAt, userId) <> (ChangeMailToken.tupled, ChangeMailToken.unapply)
  }
  // scalastyle:on

  protected val tokens = TableQuery[MailTokenScheme]

}
