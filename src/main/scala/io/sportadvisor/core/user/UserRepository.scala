package io.sportadvisor.core.user

import io.sportadvisor.core.user.UserModels.{CreateUser, User, UserData, UserID}
import io.sportadvisor.exception.ApiError
import io.sportadvisor.util.db.DatabaseConnector
import io.sportadvisor.util.db.Extractors.handleDuplicate

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait UserRepository {

  def find(email: String): Future[Option[UserData]]

  def get(userID: UserID): Future[Option[UserData]]

  def save(user: User): Future[Either[ApiError, UserData]]

  def remove(userID: UserID): Future[Int]

}

class UserRepositorySQL(val connector: DatabaseConnector)(implicit executionContext: ExecutionContext)
    extends UserTable
    with UserRepository {

  import connector._
  import connector.profile.api._

  override def find(email: String): Future[Option[UserData]] =
    db.run(users.filter(u => u.email === email).take(1).result.headOption)

  override def get(userID: UserID): Future[Option[UserData]] =
    db.run(users.filter(u => u.id === userID).take(1).result.headOption)

  private val insertQuery = users returning users.map(_.id) into ((user, id) => user.copy(id = id))

  override def save(user: User): Future[Either[ApiError, UserData]] = user match {
    case u @ CreateUser(_, _, _) => createUser(u)
    case u: UserData             => updateUser(u)
  }

  override def remove(userID: UserID): Future[Int] = db.run(users.filter(_.id === userID).delete)щ

  private def createUser(u: CreateUser): Future[Either[ApiError, UserData]] = {
    val action = insertQuery += UserData(0, u.email, u.password, u.name, None)
    db.run(action.asTry).map(handleDuplicate(_, identity[UserData]))
  }

  private def updateUser(u: UserData): Future[Either[ApiError, UserData]] = {
    db.run(users.filter(user => user.id === u.id).update(u).asTry)
      .map(handleDuplicate(_, (_: Int) => u))
  }

}
