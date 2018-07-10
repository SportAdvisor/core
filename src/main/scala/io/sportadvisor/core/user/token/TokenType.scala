package io.sportadvisor.core.user.token

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
  * @author sss3 (Vladimir Alekseev)
  */
sealed trait TokenType extends EnumEntry

object TokenType extends Enum[TokenType] {
  override def values: immutable.IndexedSeq[TokenType] = findValues

  case object MailChange extends TokenType
  case object ResetPassword extends TokenType
}
