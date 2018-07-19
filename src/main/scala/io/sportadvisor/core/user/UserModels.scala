package io.sportadvisor.core.user

import java.time.LocalDateTime

import io.sportadvisor.exception.ApiError
import io.sportadvisor.util.i18n.I18nModel.Language

/**
  * @author sss3 (Vladimir Alekseev)
  */
object UserModels {

  type UserID = Long

  sealed trait User {
    def email: String
    def password: String
  }

  final case class CreateUser(email: String, password: String, name: String) extends User
  final case class UserData(id: UserID,
                            email: String,
                            password: String,
                            name: String,
                            language: Option[String])
      extends User {
    def lang: Language = language.flatMap(Language.find).getOrElse(Language.default)
  }

  final case class ChangeMailToken(userID: UserID, token: String, expireAt: LocalDateTime)
  final case class ResetPasswordToken(userId: UserID, token: String, expireAt: LocalDateTime)

  final case class PasswordMismatch() extends ApiError("Password mismatch", None)

}
