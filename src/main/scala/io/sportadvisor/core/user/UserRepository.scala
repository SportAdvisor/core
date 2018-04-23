package io.sportadvisor.core.user

import java.sql.SQLException

import io.sportadvisor.exception.DuplicateException
import io.sportadvisor.util.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


/**
  * @author sss3 (Vladimir Alekseev)
  */
trait UserRepository {

  def find(email: String) : Future[Option[UserData]]

  def get(userID: UserID) : Future[Option[UserData]]

  def save(user: User) : Future[Either[DuplicateException, UserData]]

  def remove(userID: UserID) : Future[Option[UserData]]

}

class UserRepositorySQL(val connector: DatabaseConnector)
                       (implicit executionContext: ExecutionContext) extends UserTable with UserRepository {

  import connector._
  import connector.profile.api._

  override def find(email: String): Future[Option[UserData]] =
    db.run(users.filter(u => u.email === email).take(1).result.headOption)

  override def get(userID: UserID): Future[Option[UserData]] = Future.successful(None)

  private val insertQuery = users returning users.map(_.id) into((user, id) => user.copy(id = id))

  override def save(user: User): Future[Either[DuplicateException, UserData]] = user match {
    case u@CreateUser(_, _, _) =>
      val action = insertQuery += UserData(0, u.email, u.password, u.name)
      db.run(action.asTry).map {
        case Success(e) => Right(e)
        case Failure(e: SQLException) => if (e.getSQLState == "23505") Left(new DuplicateException) else throw e;
        case Failure(e) => throw e
      }
    case u@UserData(_, _, _, _) => db.run(users.update(u).asTry).map {
      case Success(e) => Right(u)
      case Failure(e: SQLException) => if (e.getSQLState == "23505") Left(new DuplicateException) else throw e;
      case Failure(e) => throw e
    }
  }

  override def remove(userID: UserID): Future[Option[UserData]] = Future.successful(None)


}
