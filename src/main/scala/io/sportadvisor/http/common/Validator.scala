package io.sportadvisor.http.common

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import io.circe.Json
import io.circe.syntax._
import io.sportadvisor.http.Response
import io.sportadvisor.http.json._
import io.sportadvisor.http.Response.FormError
import io.sportadvisor.util.i18n.I18n

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait Validator[T] extends (T => List[ValidationResult])

private final class DefaultValidator[T](rules: Seq[T => Option[ValidationResult]]) extends Validator[T] {
  override def apply(v1: T): List[ValidationResult] = {
    rules.flatMap(rule => rule(v1).toList).toList
  }
}

final case class ValidationResult(field: String, msgId: String) {
  def toFormError(i18n: I18n): FormError = {
    FormError(field, i18n.t(msgId))
  }
}

object Validator {
  def apply[T](rules: (T => Option[ValidationResult])*): Validator[T] =
    new DefaultValidator[T](rules)
}

final case class ValidationError(errors: List[FormError]) extends SARejection {
  override def code: StatusCode = StatusCodes.BadRequest

  override def response: Json = Response.errorResponse(errors).asJson
}
