package io.sportadvisor.core.user

import io.sportadvisor.core.user.UserModels.{UserData, UserID}
import io.sportadvisor.util.db.DatabaseConnector

/**
  * @author sss3 (Vladimir Alekseev)
  */
private[user] trait UserTable {
  protected val connector: DatabaseConnector
  import connector.profile.api._

  /**
    * id: long,
      password: varchar(255),
      name: varchar(255),
      email: varchar(255) UNIQUE,
    */
  // scalastyle:off
  class UserScheme(tag: Tag) extends Table[UserData](tag, "ACCOUNTS") {
    def id: Rep[UserID] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column[String]("name", O.Length(255))
    def email: Rep[String] = column[String]("email", O.Length(255), O.Unique)
    def password: Rep[String] = column[String]("password", O.Length(255))
    def language: Rep[Option[String]] = column[Option[String]]("language")

    override def * =
      (id, email, password, name, language) <> ((UserData.apply _).tupled, UserData.unapply)
  }
  // scalastyle:on

  protected val users = TableQuery[UserScheme]
}
