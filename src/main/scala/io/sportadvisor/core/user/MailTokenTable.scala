package io.sportadvisor.core.user

import java.sql.Timestamp
import java.time.LocalDateTime

import io.sportadvisor.util.db.DatabaseConnector
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.ProvenShape

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[user] trait MailTokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  implicit val localDateTime2Date: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, Timestamp](
      l => Timestamp.valueOf(l),
      d => d.toLocalDateTime
    )

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
