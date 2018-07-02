package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.core.user.UserModels.{ResetPasswordToken, UserID}
import io.sportadvisor.util.db.DatabaseConnector
import slick.lifted.{PrimaryKey, ProvenShape}

private[user] trait ResetPasswordTokenTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  // scalastyle:off
  class ResetPwdTokenScheme(tag: Tag) extends Table[ResetPasswordToken](tag, "RESET_PWD_TOKENS") {
    def userId: Rep[UserID] = column[Long]("user_id")
    def token: Rep[String] = column[String]("token")
    def expireAt: Rep[LocalDateTime] = column[LocalDateTime]("expire_at")

    override def * : ProvenShape[ResetPasswordToken] =
      (userId, token, expireAt) <> (ResetPasswordToken.tupled, ResetPasswordToken.unapply)

    def pk: PrimaryKey = primaryKey("pk_reset_pwd_tokens", token)

  }
  // scalastyle:on

  protected val tokens = TableQuery[ResetPwdTokenScheme]

}
