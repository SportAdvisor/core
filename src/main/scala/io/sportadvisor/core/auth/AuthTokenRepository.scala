package io.sportadvisor.core.auth

import java.time.LocalDateTime

import io.sportadvisor.core.auth.AuthModels._
import io.sportadvisor.core.user.UserModels.UserID
import io.sportadvisor.util.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait AuthTokenRepository {

  def find(token: String): Future[Option[RefreshTokenData]]

  def save(token: RefreshToken): Future[RefreshTokenData]

  def removeByUser(id: UserID): Future[Unit]

  def removeByDate(dateRemember: LocalDateTime, dateNotRemember: LocalDateTime): Future[Int]

  def findByUserId(userID: UserID): Future[Seq[RefreshToken]]

  def removeById(refreshTokenId: Long): Future[Unit]
}

class AuthTokenRepositorySQL(val connector: DatabaseConnector)(implicit executionContext: ExecutionContext)
    extends TokenTable
    with AuthTokenRepository {

  import connector._
  import connector.profile.api._

  private val insertQuery = tokens returning tokens.map(_.id) into ((token, id) => token.copy(id = id))

  override def save(token: RefreshToken): Future[RefreshTokenData] = token match {
    case t: CreateRefreshToken => createToken(t)
    case t: RefreshTokenData   => updateToken(t)
  }

  private def createToken(token: CreateRefreshToken): Future[RefreshTokenData] = {
    val action = insertQuery += RefreshTokenData(0,
                                                 token.userId,
                                                 token.token,
                                                 token.remember,
                                                 token.lastTouch)
    db.run(action)
  }

  private def updateToken(t: RefreshTokenData): Future[RefreshTokenData] = {
    db.run(tokens.filter(token => token.id === t.id).update(t)).map(_ => t)
  }

  override def removeByUser(id: UserID): Future[Unit] = {
    val query = tokens.filter(t => t.userId === id)
    db.run(query.delete).map(_ => ())
  }

  override def removeByDate(dateRemember: LocalDateTime, dateNotRemember: LocalDateTime): Future[Int] = {
    val query = tokens.filter(t =>
      (t.remember === true && t.lastTouch < dateRemember) || (!t.remember && t.lastTouch < dateNotRemember))
    db.run(query.delete)
  }

  override def findByUserId(userID: UserID): Future[Seq[RefreshToken]] = {
    val query = tokens.filter(_.userId === userID)
    db.run(query.result)
  }

  override def removeById(refreshTokenId: Long): Future[Unit] = {
    val query = tokens.filter(_.id === refreshTokenId)
    db.run(query.delete).map(_ => ())
  }

  override def find(token: String): Future[Option[RefreshTokenData]] = {
    db.run(tokens.filter(t => t.token === token).take(1).result.headOption)
  }
}
