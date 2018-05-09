package io.sportadvisor.core.user

import java.sql.Timestamp
import java.time.LocalDateTime

import io.sportadvisor.util.db.DatabaseConnector
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[user] trait TokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  implicit val localDateTime2Date: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, Timestamp](
      l => Timestamp.valueOf(l),
      d => d.toLocalDateTime
    )

  // scalastyle:off
  class TokenScheme(tag: Tag) extends Table[RefreshToken](tag, "REFRESH_TOKENS") {
    def userId = column[Long]("user_id")
    def token = column[String]("token", O.Length(255))
    def remember = column[Boolean]("remember")
    def lastTouch = column[LocalDateTime]("last_touch")

    override def * =
      (userId, token, remember, lastTouch) <> (RefreshToken.tupled, RefreshToken.unapply)

    def pk = primaryKey("pk_refresh_tokens", (userId, token))
  }
  // scalastyle:on

  protected val tokens = TableQuery[TokenScheme]

}
