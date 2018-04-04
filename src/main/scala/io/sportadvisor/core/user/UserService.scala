package io.sportadvisor.core.user

import java.time.{LocalDateTime, ZoneId}

import io.sportadvisor.util.MonadTransformers._
import com.roundeights.hasher.Implicits._
import pdi.jwt._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContext, Future}



/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserService(userRepository: UserRepository, tokenRepository: TokenRepository, secretKey: String)
                 (implicit executionContext: ExecutionContext) {

  private[this] val expPeriod = 30

  private[this] val rememberExp = 10
  private[this] val notRememberExp = 1

  def signIn(email: String, password: String, remember: Boolean) : Future[Option[AuthToken]] =
    userRepository
        .find(email)
        .filterT(_.password == password.sha256.hex)
        .mapT(createToken)
        .flatMapT(t => saveToken(t._1, t._2, remember))

  def signUp(email: String, password: String, name: String) : Future[AuthToken] =
    userRepository
        .save(UserData(null.asInstanceOf[UserID], email, password.sha256.hex, name))
        .map(createToken)
        .flatMap(t => saveToken(t._1, t._2, remember = false))
        .flatMapT()

  def signOut(token: Token) : Future[Boolean] =
    tokenRepository.find(token)
      .flatMapTOuter(t => tokenRepository.remove(t))
      .map(_.nonEmpty)

  def refreshToken(token: Token) : Future[Option[AuthToken]] =
    tokenRepository.find(token)
      .mapT(t => decodeRefreshToken(t, secretKey))
      .flatMapT {
        case Left(tuple) =>
          updateRefreshToken(tuple._2)
              .map(t => Option(tuple._1))
        case Right(t) =>
          tokenRepository.remove(t)
            .map(b => None)
      }.flatMapT(u => userRepository.get(u))
      .mapT(u => createToken(u)._2.updateRefreshToken(token))


  private def createToken(user: UserData) : (UserData, AuthToken) = {
    val expTime = LocalDateTime.now().plusMinutes(expPeriod)
    val token = Jwt.encode(AuthTokenContent(user.id, expTime.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli)
      .asJson.noSpaces, secretKey, JwtAlgorithm.HS256)
    val refreshToken = Jwt.encode(RefreshTokenContent(user.id, LocalDateTime.now()).asJson.noSpaces, secretKey, JwtAlgorithm.HS256)
    (user, AuthToken(token, refreshToken, expTime))
  }

  private def saveToken(userData: UserData, authToken: AuthToken, remember: Boolean) : Future[Option[AuthToken]] = {
    val exp = calculateExp(remember)
    val refreshToken = RefreshToken(userData.id, authToken.refreshToken, exp, remember)
    tokenRepository.save(refreshToken)
        .map(b => Option(authToken))
  }

  private def updateRefreshToken(token: RefreshToken): Future[RefreshToken] = tokenRepository.save(token.updateExp(calculateExp(token.remember)))

  private def decodeRefreshToken(token: RefreshToken, secret: String) : Either[(UserID, RefreshToken), RefreshToken] =
    Jwt.decodeRaw(token.refreshToken, secret, Seq(JwtAlgorithm.HS256))
      .toOption.flatMap(decode[RefreshTokenContent](_).toOption)
      .map(_.userID)
      .map(u => (u, token))
      .toLeft(token)

  private def calculateExp(remember: Boolean) : LocalDateTime = LocalDateTime.now().plusDays(if (remember) rememberExp else notRememberExp)

}

