package io.sportadvisor.core.user

import java.time.LocalDateTime
import java.util.Date

import io.circe.generic.auto._
import com.roundeights.hasher.Implicits._
import io.sportadvisor.util.JwtUtil

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import io.sportadvisor.util.MonadTransformers._

/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserService(userRepository: UserRepository,
                  tokenRepository: TokenRepository,
                  secretKey: String)(implicit executionContext: ExecutionContext) {

  private[this] val expPeriod = 2.hour.toMinutes

  def signUp(email: String,
             password: String,
             name: String): Future[Either[UserAlreadyExists, AuthToken]] =
    userRepository
      .save(CreateUser(email, password.sha256.hex, name))
      .flatMap {
        case Left(e)  => Future.successful(Left(new UserAlreadyExists))
        case Right(u) => createAndSaveToken(u, Boolean.box(true)).map(t => Right(t))
      }

  def signIn(email: String, password: String, remember: Boolean): Future[Option[AuthToken]] =
    userRepository
      .find(email)
      .filterT(u => password.sha256.hex == u.password)
      .flatMapTOuter(u => createAndSaveToken(u, remember))

  private def createAndSaveToken(user: UserData, remember: Boolean): Future[AuthToken] = {
    val token = createToken(user, remember)
    saveToken(user, token.refreshToken, remember, LocalDateTime.now())
      .map(_ => token)
  }

  private def createToken(user: UserData, remember: Boolean): AuthToken = {
    val time = LocalDateTime.now()
    val expTime = time.plusMinutes(expPeriod)
    val token = JwtUtil.encode(AuthTokenContent(user.id), secretKey, Option(expTime))
    val refreshToken = JwtUtil.encode(RefreshTokenContent(user.id, new Date().getTime), secretKey)
    AuthToken(token, refreshToken, expTime)
  }

  private def saveToken(user: UserData,
                        refreshToken: Token,
                        remember: Boolean,
                        creation: LocalDateTime): Future[RefreshToken] = {
    tokenRepository.save(RefreshToken(user.id, refreshToken, remember, creation))
  }

}
