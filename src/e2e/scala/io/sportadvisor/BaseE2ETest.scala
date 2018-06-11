package io.sportadvisor

import scala.collection.JavaConverters._
import com.dimafeng.testcontainers._
import com.github.dockerjava.api.command.InspectContainerResponse
import io.circe.{Decoder, HCursor}
import io.circe.parser.parse
import org.scalatest.{FlatSpec, Matchers}
import org.testcontainers.containers.wait.strategy.Wait
import scalaj.http.{Http, HttpRequest}

import scala.util.Random

/**
  * @author sss3 (Vladimir Alekseev)
  * https://github.com/scalaj/scalaj-http
  */
trait BaseE2ETest extends FlatSpec with Matchers with ForAllTestContainer with Decoders {

  private val port = 5553
  private val smtp = ""
  protected val smtpPort = 1025
  protected val smtpUser = "no-reply@sportadvisor.io"
  protected val smtpPass = ""

  lazy val pgContainer = PostgreSQLContainer()

  lazy val app = GenericContainer("io.sportadvisor/sportadvisor-core:it",
                                  exposedPorts = List(port),
                                  env = env(),
                                  waitStrategy = Wait.forHttp("/healthcheck"))

  override val container = MultipleContainers(appContainers():_*)

  protected def server: String = s"http://${getIp(app.container.getContainerInfo)}:$port"

  protected def to(s: String) = s"$server/$s"

  protected def r[A](body: String)(implicit decoder: Decoder[A]): A = {
    decoder(HCursor.fromJson(parse(body).toOption.get)) match {
      case Left(e) => throw e
      case Right(r) => r
    }
  }

  protected def post(url: String, data: String): HttpRequest = {
    Http(url).postData(data).header("content-type", "application/json")
  }

  protected def additionalContainer(): Seq[LazyContainer[Container]] = List()

  private def env(): Map[String, String] = Map(
    "SECRET_KEY" -> ("IntegrationTest" + Random.nextString(2)),
    "JDBC_URL" -> dockerJdbcUrl(pgContainer),
    "JDBC_USER" -> pgContainer.username,
    "JDBC_PASSWORD" -> pgContainer.password,
    "MAIL_SMTP" -> smtp,
    "MAIL_SMPT_PORT" -> smtpPort.toString,
    "MAIL_USER" -> smtpUser,
    "MAIL_PASS" -> smtpPass
  )

  private def dockerJdbcUrl(pgContainer: PostgreSQLContainer): String = {
    import org.testcontainers.containers.{PostgreSQLContainer => OTCPostgreSQLContainer}

    val ip = getIp(pgContainer.container.getContainerInfo)
    s"jdbc:postgresql://$ip:${OTCPostgreSQLContainer.POSTGRESQL_PORT}/test"
  }

  private def getIp(c: InspectContainerResponse): String =
    c.getNetworkSettings.getNetworks.asScala.values.head.getIpAddress

  private def containers(): Seq[LazyContainer[Container]] = List(LazyContainer(pgContainer), LazyContainer(app))

  private def appContainers(): Seq[LazyContainer[Container]] = additionalContainer() ++ containers()

}
