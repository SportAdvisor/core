package io.sportadvisor.http.route.trainer

import java.time.ZonedDateTime

import io.sportadvisor.core.system.SystemModels.Currency
import io.sportadvisor.http.common.Validated.ValidationRule
import io.sportadvisor.http.common.{CommonValidations, Validated, ValidationResult}
import io.sportadvisor.http.route.trainer.TrainerRouteProtocol._

import cats.instances.double._
import cats.instances.int._

/**
  * @author sss3 (Vladimir Alekseev)
  */
object TrainerRouteValidators {

  val minSportDisciplines = 1
  val maxSportDisciplines = 5

  val birthdayLtNow = "???"
  val sportDisciplinesLen = "???"
  val notSupportedCurrency = "???"

  implicit val trainerCreateValidator: Validated[CreateTrainer] = Validated[CreateTrainer](
    (ct: CreateTrainer) => CommonValidations.requiredString("about")(ct.about),
    ct => CommonValidations.required("birthday")(ct.birthday),
    ct => CommonValidations.required("sex")(ct.sex),
    ct => CommonValidations.required("sports")(ct.sports),
    ct => CommonValidations.required("city")(ct.city),
    ct => CommonValidations.required("country")(ct.country),
    ct => CommonValidations.requiredString("alias")(ct.alias),
    ct => {
      if (ct.birthday.isBefore(ZonedDateTime.now())) {
        Some(ValidationResult("birthday", birthdayLtNow))
      } else {
        None
      }
    },
    ct => {
      if (ct.sports.length < minSportDisciplines || ct.sports.length > maxSportDisciplines) {
        Some(ValidationResult("sports", sportDisciplinesLen, minSportDisciplines, maxSportDisciplines))
      } else {
        None
      }
    }
//    ct => contactValidator("contacts")(ct.contacts),
//    ct => personalTrainingValidator("personalTraining")(ct.personalTraining),
//    ct => groupTrainingValidator("groupTraining")(ct.groupTraining)
  )

  private def contactValidator(field: String): Validated[Contacts] = ???

  private def personalTrainingValidator(field: String): Validated[PersonalTraining] = Validated[PersonalTraining](
    pt => CommonValidations.required(s"$field.currency")(pt.currency),
    pt => supportCurrency(s"$field.currency")(pt.currency),
    pt => CommonValidations.required(s"$field.price")(pt.price),
    pt => CommonValidations.validateMin[Double](s"$field.price", 1D).apply(pt.price)
  )

  private def groupTrainingValidator(field: String): Validated[GroupTraining] =  Validated[GroupTraining](
    gt => CommonValidations.required(s"$field.currency")(gt.currency),
    gt => supportCurrency(s"$field.currency")(gt.currency),
    gt => CommonValidations.required(s"$field.price")(gt.price),
    gt => CommonValidations.validateMin[Double](s"$field.price", 1D).apply(gt.price),
    gt => gt.minPeople.flatMap(CommonValidations.validateMin(s"$field.minPeople", 1).apply(_)),
    gt => gt.maxPeople.flatMap(CommonValidations.validateMin(s"$field.maxPeople", 1).apply(_))
  )

  private def supportCurrency(field: String): ValidationRule[Int] = c => {
    if (!Currency.supported.map(_.id).contains(c)) {
      Some(ValidationResult(field, notSupportedCurrency))
    } else {
      None
    }
  }
}
