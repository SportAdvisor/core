package io.sportadvisor

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
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

  val unitVal: Unit = ()

}
