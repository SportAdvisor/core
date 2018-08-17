package io.sportadvisor.http.route.trainer

import java.time.LocalDate

import io.sportadvisor.core.system.SystemModels.Currency
import io.sportadvisor.http.common.Validated.ValidationRule
import io.sportadvisor.http.common.{CommonValidations, Validated, ValidationResult}
import io.sportadvisor.http.route.trainer.TrainerRouteProtocol._
import cats.instances.double._

/**
  * @author sss3 (Vladimir Alekseev)
  */
object TrainerRouteValidators {

  val minSportDisciplines = 1
  val maxSportDisciplines = 5

  val sportDisciplinesLen = "There must be at least %s sport selected but not more than %s"
  val notDefinedContacts = "At least 1 contact must be defined"

  implicit val trainerCreateValidator: Validated[CreateTrainer] = Validated[CreateTrainer](
    (ct: CreateTrainer) => CommonValidations.requiredString("about")(ct.about),
    ct => validateBirthday("birthday")(ct.birthday),
    ct => CommonValidations.required("sex")(ct.sex),
    ct => validateSports("sports")(ct.sports),
    ct => CommonValidations.required("city")(ct.city),
    ct => CommonValidations.required("country")(ct.country),
    ct => CommonValidations.requiredString("alias")(ct.alias),
    ct => contactValidator("contacts")(ct.contacts),
    ct =>
      ct.personalTraining
        .filter(_.support)
        .map(personalTrainingValidator("personalTraining").validate(_))
        .getOrElse(List()),
    ct =>
      ct.groupTraining
        .filter(_.support)
        .map(groupTrainingValidator("groupTraining").validate(_))
        .getOrElse(List())
  )

  private def contactValidator(field: String): ValidationRule[Contacts] =
    c =>
      Contacts.unapply(c) match {
        case None => List(ValidationResult(field, notDefinedContacts))
        case Some(tuple)
            if tuple.productIterator
              .map(_.asInstanceOf[Option[String]])
              .forall(_.map(_.trim).forall(_.isEmpty)) =>
          List(ValidationResult(field, notDefinedContacts))
        case _ => List()
    }

  private def personalTrainingValidator(field: String): Validated[PersonalTraining] =
    Validated[PersonalTraining](
      pt => validateCurrency(s"$field.currency")(pt.currency),
      pt => validatePrice(s"$field.price")(pt.price)
    )

  private def groupTrainingValidator(field: String): Validated[GroupTraining] = Validated[GroupTraining](
    gt => validateCurrency(s"$field.currency")(gt.currency),
    gt => validatePrice(s"$field.price")(gt.price),
    gt => gt.minPeople.map(CommonValidations.validateMin(s"$field.minPeople", 1).apply(_)).getOrElse(List()),
    gt => gt.maxPeople.map(CommonValidations.validateMin(s"$field.maxPeople", 1).apply(_)).getOrElse(List())
  )

  private def validateCurrency(field: String): ValidationRule[Int] =
    c =>
      Option(c) match {
        case None => List(ValidationResult(field, CommonValidations.requiredField))
        case Some(curr) if !Currency.supported.map(_.id).contains(curr) =>
          List(ValidationResult(field, CommonValidations.invalidField))
        case _ => List()
    }

  private def validatePrice(field: String): ValidationRule[Double] =
    p =>
      Option(p) match {
        case None        => List(ValidationResult(field, CommonValidations.requiredField))
        case Some(price) => CommonValidations.validateMin[Double](field, 1D).apply(price)
        case _           => List()
    }

  private def validateBirthday(field: String): ValidationRule[LocalDate] =
    bd =>
      Option(bd) match {
        case None                                  => List(ValidationResult(field, CommonValidations.requiredField))
        case Some(v) if v.isAfter(LocalDate.now()) => List(ValidationResult(field, CommonValidations.invalidField))
        case _                                     => List()
    }

  private def validateSports(field: String): ValidationRule[List[Int]] =
    l =>
      Option(l) match {
        case None => List(ValidationResult(field, CommonValidations.requiredField))
        case Some(list) if list.length < minSportDisciplines || list.length > maxSportDisciplines =>
          List(ValidationResult(field, sportDisciplinesLen, minSportDisciplines, maxSportDisciplines))
        case _ => List()
    }
}
