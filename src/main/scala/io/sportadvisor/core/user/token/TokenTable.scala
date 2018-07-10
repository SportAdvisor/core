package io.sportadvisor.core.user.token

import java.time.LocalDateTime

import io.sportadvisor.util.db.DatabaseConnector
import slick.jdbc.JdbcType
import slick.lifted.ProvenShape

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[token] trait TokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  implicit lazy val typeMapper: JdbcType[TokenType] =
    connector.profile.mappedColumnTypeForEnum(TokenType)

  // scalastyle:off
  class TokenScheme(tag: Tag) extends Table[ExpiredToken](tag, "TOKENS") {
    def userId: Rep[Long] = column[Long]("user_id")
    def token: Rep[String] = column[String]("token", O.PrimaryKey)
    def expireAt: Rep[LocalDateTime] = column[LocalDateTime]("expire_at")
    def tokenType: Rep[TokenType] = column[TokenType]("type")

    override def * : ProvenShape[ExpiredToken] =
      (userId, token, expireAt, tokenType) <> (ExpiredToken.tupled, ExpiredToken.unapply)
  }
  // scalastyle:on

  protected val tokens = TableQuery[TokenScheme]
}
