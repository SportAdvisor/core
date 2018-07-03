package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.util.db.DatabaseConnector
import slick.lifted.ProvenShape

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[user] trait MailTokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  // scalastyle:off
  class MailTokenScheme(tag: Tag) extends Table[ChangeMailToken](tag, "MAIL_TOKENS") {
    def token: Rep[String] = column[String]("token", O.PrimaryKey)
    def expireAt: Rep[LocalDateTime] = column[LocalDateTime]("expire_at")

    override def * : ProvenShape[ChangeMailToken] =
      (token, expireAt) <> (ChangeMailToken.tupled, ChangeMailToken.unapply)
  }
  // scalastyle:on

  protected val tokens = TableQuery[MailTokenScheme]

}
