package io.sportadvisor.http.common

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import io.circe.Json
import io.circe.syntax._
import io.sportadvisor.http.Response
import io.sportadvisor.http.Response.FieldFormError
import io.sportadvisor.http.common.Validated.ValidationRule
import io.sportadvisor.util.i18n.I18n

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait Validated[T] {
  def validate(model: T): List[ValidationResult]
}

private final class DefaultValidated[T](rules: Seq[ValidationRule[T]]) extends Validated[T] {
  override def validate(v1: T): List[ValidationResult] = {
    rules.flatMap(rule => rule(v1)).toList
  }
}

final case class ValidationResult(field: String, msgId: String, args: String*) {
  def toFormError(i18n: I18n): FieldFormError = args match {
    case Seq() => FieldFormError(field, i18n.t(msgId))
    case _     => FieldFormError(field, i18n.t(msgId, args))
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
object Validated {

  type ValidationRule[T] = T => List[ValidationResult]

  def build[T]: ValidatedVia[T] = new ValidatedVia[T]

  def apply[T](implicit v: Validated[T]): Validated[T] = v

  class ValidatedVia[T] {
    def apply(rules: ValidationRule[T]*): Validated[T] =
      new DefaultValidated[T](rules)
  }
}

final case class ValidationError(errors: List[FieldFormError]) extends SARejection {
  override def code: StatusCode = StatusCodes.BadRequest

  override def response: Json = Response.error(errors).asJson
}
