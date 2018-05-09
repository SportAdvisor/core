package io.sportadvisor.core.user

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
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.Length(255))
    def email = column[String]("email", O.Length(255), O.Unique)
    def password = column[String]("password", O.Length(255))

    override def * = (id, email, password, name) <> ((UserData.apply _).tupled, UserData.unapply)
  }
  // scalastyle:on

  protected val users = TableQuery[UserScheme]
}
