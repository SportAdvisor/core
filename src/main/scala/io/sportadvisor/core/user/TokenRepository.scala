package io.sportadvisor.core.user

import java.time.LocalDateTime
import io.sportadvisor.util.db.DatabaseConnector
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TokenRepository {

  def save(token: RefreshToken): Future[RefreshToken]

  def removeByDate(dateRemember: LocalDateTime, dateNotRemember: LocalDateTime) : Future[Unit]

  def getByUserId(userID: UserID) : Future[Seq[RefreshToken]]
}

class TokenRepositorySQL(val connector: DatabaseConnector)
                        (implicit executionContext: ExecutionContext) extends TokenTable with TokenRepository {

  import connector._
  import connector.profile.api._

  override def save(token: RefreshToken): Future[RefreshToken] = {
    db.run(tokens += token).map(_ => token)
  }

  override def removeByDate(dateRemember: LocalDateTime, dateNotRemember: LocalDateTime): Future[Unit] = {
    val query1 = tokens.filter(t => t.remember && t.lastTouch < dateRemember)
    val query2 = tokens.filter(t => !t.remember &&  t.lastTouch < dateNotRemember)
    db.run(query1.delete).map(_ => {})
    db.run(query2.delete).map(_ => {})
  }

  override def getByUserId(userID: UserID): Future[Seq[RefreshToken]] = {
    val query = tokens.filter(_.userId === userID)
    db.run(query.result)
  }
}
