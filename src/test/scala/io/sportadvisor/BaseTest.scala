package io.sportadvisor

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import io.circe.Encoder
import io.circe.syntax._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

/**
  * @author sss3 (Vladimir Alekseev)
  * https://doc.akka.io/docs/akka-http/current/routing-dsl/testkit.html
  * http://www.scalatest.org/user_guide/using_matchers
  */
trait BaseTest extends WordSpec with Matchers with ScalatestRouteTest with MockitoSugar {

  def awaitForResult[T](futureResult: Future[T]): T =
    Await.result(futureResult, 5.seconds)

  def r[T: FromResponseUnmarshaller: ClassTag]: T = responseAs[T]

  def sleep(t: Duration): Unit = Thread.sleep(t.toMillis)

  def requestBody[T: Encoder](body: T): HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, body.asJson.noSpaces)

  val unitVal: Unit = ()

}
