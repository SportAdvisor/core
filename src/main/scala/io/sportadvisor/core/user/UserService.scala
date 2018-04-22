package io.sportadvisor.core.user

import java.time.LocalDateTime
import java.util.Date

import io.circe.generic.auto._
import io.circe.syntax._
import com.roundeights.hasher.Implicits._
import io.sportadvisor.util.JwtUtil

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}



/**
  * @author sss3 (Vladimir Alekseev)
  */
class UserService(userRepository: UserRepository, secretKey: String)
                 (implicit executionContext: ExecutionContext) {

  private[this] val expPeriod = 2.hour.toMinutes

  private[this] val rememberExp = 10
  private[this] val notRememberExp = 1

  def signUp(email: String, password: String, name: String) : Future[Either[UserAlreadyExists, AuthToken]] =
    userRepository
      .save(CreateUser(email, password.sha256.hex, name))
      .map {
        case Left(e) => Left(new UserAlreadyExists)
        case Right(u) => Right(createToken(u))
      }


  private def createToken(user: UserData) : AuthToken = {
    val expTime = LocalDateTime.now().plusMinutes(expPeriod)
    val token = JwtUtil.encode(AuthTokenContent(user.id), secretKey, Option(expTime))
    val refreshToken = JwtUtil.encode(RefreshTokenContent(user.id, new Date().getTime), secretKey)
    AuthToken(token, refreshToken, expTime)
  }

}

