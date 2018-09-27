package io.sportadvisor.http.route

import java.time.LocalDate

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{handleExceptions, handleRejections, pathPrefix}
import akka.http.scaladsl.server.Route
import io.sportadvisor.BaseTest
import io.sportadvisor.core.auth.AuthService
import io.sportadvisor.core.gis.GisModels.Coordinate
import io.sportadvisor.core.sport.SportModels.Sport
import io.sportadvisor.core.sport.SportService
import io.sportadvisor.core.system.SystemModels.Sex
import io.sportadvisor.core.system.SystemModels.Sex.MALE
import io.sportadvisor.core.trainer.TrainerModels._
import io.sportadvisor.core.trainer.TrainerService
import io.sportadvisor.http.Codecs._
import io.sportadvisor.http.HttpTestUtils._
import io.sportadvisor.http.Response.{EmptyResponse, ErrorResponse, FieldFormError, FormError}
import io.sportadvisor.http.common.CommonValidations
import io.sportadvisor.http.route.trainer.TrainerRoute
import io.sportadvisor.http.{exceptionHandler, rejectionHandler, I18nStub}
import io.sportadvisor.util.I18nService
import io.sportadvisor.util.i18n.I18nModel.Language
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.concurrent.Future

/**
  * @author sss3 (Vladimir Alekseev)
  */
class TrainerRouteTest extends BaseTest {

  "TrainerRoute" when {
    "POST /api/trainers" should {
      "return 201 and url to new trainer" in new Context {
        val entity: HttpEntity.Strict = requestBody(validRequestModel)
        when(trainerService.create(any[CreateTrainer](), anyLong()))
          .thenReturn(Future.successful(Right(trainerValid)))
        Post("/api/trainers", entity).withHeaders(authHeader(userId)) ~> trainerRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe StatusCodes.Created.intValue
          response.status shouldBe StatusCodes.Created

          val location = header("Location")
          location.isDefined shouldBe true
        }
      }

      "return 401 if user unauthorized" in new Context {
        Post("/api/trainers", requestBody(validRequestModel)) ~> trainerRoute ~> check {
          val resp = r[EmptyResponse]
          resp.code shouldBe StatusCodes.Unauthorized.intValue
          response.status shouldBe StatusCodes.Unauthorized
        }
      }

      "return 400 if limit exceeded" in new Context {
        when(trainerService.create(any[CreateTrainer](), anyLong()))
          .thenReturn(Future.successful(Left(LimitPagesOnUser())))
        val req: HttpEntity.Strict = requestBody(validRequestModel)
        Post("/api/trainers", req).withHeaders(authHeader(userId)) ~> trainerRoute ~> check {
          val resp = r[ErrorResponse[FormError]]
          resp.code shouldBe StatusCodes.BadRequest.intValue
          response.status shouldBe StatusCodes.BadRequest
          resp.errors should (contain(FormError(TrainerRoute.pagesLimit)) and have size 1)
        }
      }

      "return 400 if sports not unique" in new Context {
        when(trainerService.create(any[CreateTrainer](), anyLong()))
          .thenReturn(Future.successful(Left(NotUniqueSports(Map(1L -> "PageAlias")))))
        when(sportService.find(1L)).thenReturn(Some(Sport(1L, Map(Language.EN -> "Football"))))
        val req: HttpEntity.Strict = requestBody(validRequestModel)
        Post("/api/trainers", req).withHeaders(authHeader(userId)) ~> trainerRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe StatusCodes.BadRequest.intValue
          response.status shouldBe StatusCodes.BadRequest
          resp.errors should (contain(FieldFormError(
            "sports",
            TrainerRoute.notUniqueSports.format("Football", "PageAlias"))) and have size 1)
        }
      }

      "return 400 if about is empty" in new Context {
        val req: HttpEntity.Strict = requestBody(validRequestModel.copy(about = "   "))
        Post("/api/trainers", req).withHeaders(authHeader(userId)) ~> trainerRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe StatusCodes.BadRequest.intValue
          response.status shouldBe StatusCodes.BadRequest
          resp.errors should (contain(FieldFormError("about", CommonValidations.requiredField)) and have size 1)
        }
      }

      "return 400 if birthday invalid" in new Context {
        val req: HttpEntity.Strict =
          requestBody(validRequestModel.copy(birthday = LocalDate.now().plusDays(1)))
        Post("/api/trainers", req).withHeaders(authHeader(userId)) ~> trainerRoute ~> check {
          val resp = r[ErrorResponse[FieldFormError]]
          resp.code shouldBe StatusCodes.BadRequest.intValue
          response.status shouldBe StatusCodes.BadRequest
          resp.errors should (contain(FieldFormError("birthday", CommonValidations.invalidField)) and have size 1)
        }
      }


    }
  }

  val validRequestModel: CreateTrainer = CreateTrainer(
    about = "about",
    alias = "alias",
    birthday = LocalDate.now().minusYears(19),
    sex = Sex.MALE.id,
    sports = List(1, 2),
    groupTraining = None,
    personalTraining = None,
    contacts = Contacts(Option("123123123"), None, None, None),
    workWithChild = true,
    workWithInjured = true,
    workWithAdults = true,
    country = Place("Russia", Coordinate(12D, 17D)),
    city = Place("Kazan", Coordinate(12D, 17D)),
    places = List()
  )

  val trainerValid: Trainer = Trainer(
    id = 1L,
    about = "about",
    alias = "alias",
    birthday = LocalDate.now().minusYears(19),
    sex = MALE,
    sports = List(1, 2),
    group = None,
    personal = None,
    contacts = Contacts(Option("123123123"), None, None, None),
    workWithChild = true,
    workWithInjured = true,
    workWithAdults = true,
    countryId = 1L,
    cityId = 1L,
    places = List()
  )

  trait Context {

    val userId = 1L
    val trainerService: TrainerService = mock[TrainerService]
    val sportService: SportService = mock[SportService]

    implicit val auth: AuthService = mock[AuthService]
    implicit val i18n: I18nService = I18nStub

    val trainerRoute: Route = handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        pathPrefix("api") {
          new TrainerRoute(trainerService, sportService).route
        }
      }
    }
  }

}
