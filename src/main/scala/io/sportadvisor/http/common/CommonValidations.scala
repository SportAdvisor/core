package io.sportadvisor.http.common

import cats.{Order, Show}
import io.sportadvisor.http.common.Validated.ValidationRule

/**
  * @author sss3 (Vladimir Alekseev)
  */
object CommonValidations {

  val requiredField = "Field is required"
  val invalidField = "Field is invalid"
  val minValueError = "Min value is %s"
  val maxValueError = "Max value is %s"

  def required[T](field: String): ValidationRule[T] =
    t =>
      if (isNull(t)) {
        List(ValidationResult(field, requiredField))
      } else {
        List()
    }

  def requiredString(field: String): ValidationRule[String] = t => {
    if (isNull(t) || t.trim.isEmpty) {
      List(ValidationResult(field, requiredField))
    } else {
      List()
    }
  }

  def validateMin[T: Order: Show](field: String, minVal: T): ValidationRule[T] = t => {
    if (Order[T].lt(t, minVal)) {
      List(ValidationResult(field, minValueError, Show[T].show(minVal)))
    } else {
      List()
    }
  }

  def validateMax[T: Order: Show](field: String, maxVal: T): ValidationRule[T] = t => {
    if (Order[T].gt(t, maxVal)) {
      List(ValidationResult(field, minValueError, Show[T].show(maxVal)))
    } else {
      List()
    }
  }

  private def isNull[T](t: T) = Option(t) match {
    case None => true
    case _    => false
  }
}
