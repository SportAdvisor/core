package io.sportadvisor.core.user

import java.time.LocalDateTime
import io.sportadvisor.util.db.DatabaseConnector
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait TokenRepository {

  def save(token: RefreshToken): Future[RefreshToken]

  def removeByUser(id: UserID): Future[Unit]

  def removeByDate(dateRemember: LocalDateTime, dateNotRemember: LocalDateTime): Future[Unit]

  def getByUserId(userID: UserID): Future[Seq[RefreshToken]]

  def removeById(refreshTokenId: Long): Future[Unit]
}

class TokenRepositorySQL(val connector: DatabaseConnector)(
    implicit executionContext: ExecutionContext)
    extends TokenTable
    with TokenRepository {

  import connector._
  import connector.profile.api._

  override def save(token: RefreshToken): Future[RefreshToken] = {
    db.run(tokens += token).map(_ => token)
  }

  override def removeByUser(id: UserID): Future[Unit] = {
    val query = tokens.filter(t => t.userId === id)
    db.run(query.delete).map(_ => ())
  }

  override def removeByDate(dateRemember: LocalDateTime,
                            dateNotRemember: LocalDateTime): Future[Unit] = {
    val query = tokens.filter(t =>
      (t.remember === true && t.lastTouch < dateRemember) || (!t.remember && t.lastTouch < dateNotRemember))
    db.run(query.delete).map(_ => { () })
  }

  override def getByUserId(userID: UserID): Future[Seq[RefreshToken]] = {
    val query = tokens.filter(_.userId === userID)
    db.run(query.result)
  }

  override def removeById(refreshTokenId: Long): Future[Unit] = {
    val query = tokens.filter(_.id === refreshTokenId)
    db.run(query.delete).map(_ => ())
  }
}
