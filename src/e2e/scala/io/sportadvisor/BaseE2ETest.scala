package io.sportadvisor

import java.time.Duration
import java.util.Base64

import scala.collection.JavaConverters._
import com.dimafeng.testcontainers._
import com.github.dockerjava.api.command.InspectContainerResponse
import io.circe._
import io.circe.parser._
import io.sportadvisor.core.auth.AuthModels.AuthToken
import io.sportadvisor.http.Response.{DataResponse, ObjectData}
import org.scalatest.{FlatSpec, Matchers}
import org.slf4s.Logging
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType
import org.testcontainers.containers.wait.strategy.Wait
import scalaj.http.{Http, HttpRequest}

import scala.util.Random

/**
  * @author sss3 (Vladimir Alekseev)
  * https://github.com/scalaj/scalaj-http
  */
trait BaseE2ETest
    extends FlatSpec
    with Matchers
    with ForAllTestContainer
    with Decoders
    with Preconditions
    with Logging {

  private val port = 5553
  protected val smtpUser = "no-reply@sportadvisor.io"
  protected val smtpPass = ""
  protected val SMTP_PORT = "smtp-port"
  protected val SMTP_IP = "smtp-ip"
  protected val MAIL_HOG_API_PORT = "mail-hog-api-port"

  lazy val pgContainer = PostgreSQLContainer()

  lazy val app: GenericContainer =
    GenericContainer(
      "io.sportadvisor/sportadvisor-core:it",
      exposedPorts = Seq(port),
      env = env(),
      waitStrategy = Wait.forHttp("/healthcheck").withStartupTimeout(Duration.ofSeconds(10))
    ).configure(c => {
      c.withLogConsumer(dockerLog)
      ()
    })

  override val container = MultipleContainers(appContainers(): _*)

  override def afterStart(): Unit = setup()

  protected def server: String = s"http://${getIp(app.container.getContainerInfo)}:$port"

  protected def to(s: String) = s"$server/$s"

  protected def r[A](body: String)(implicit decoder: Decoder[A]): A = {
    decoder(HCursor.fromJson(parse(body).toOption.get)) match {
      case Left(e)  => throw e
      case Right(r) => r
    }
  }

  protected final def auth(email: String, pass: String, remember: Boolean): AuthToken =
    r[DataResponse[AuthToken, ObjectData[AuthToken]]](post(
      to("api/users/sign-in"),
      s"""{"email": "$email", "password": "$pass", "remember":$remember}""").asString.body).data.data

  protected final def post(url: String, data: String, token: String = ""): HttpRequest =
    Http(url).postData(data).defaultHeaders(token).defaultTimeout

  protected final def put(url: String, data: String, token: String = ""): HttpRequest =
    Http(url)
      .postData(data)
      .method("PUT")
      .defaultHeaders(token)
      .defaultTimeout

  protected final def get(url: String, token: String = ""): HttpRequest =
    Http(url)
      .defaultHeaders(token)
      .defaultTimeout

  protected final def delete(url: String, token: String = ""): HttpRequest =
    Http(url).defaultHeaders(token).defaultTimeout

  protected def userId(token: String): Long = {
    val payloadBase64 = token.substring(token.indexOf('.') + 1, token.lastIndexOf('.'))
    val payload = new String(Base64.getDecoder.decode(payloadBase64))
    parse(payload).getOrElse(Json.Null).hcursor.downField("userID").as[Long].toOption.get
  }

  protected def additionalContainer(initial: Seq[LazyContainer[Container]]): Seq[LazyContainer[Container]] =
    initial

  private def env(): Map[String, String] = Map(
    "SECRET_KEY" -> ("IntegrationTest" + Random.nextString(2)),
    "JDBC_URL" -> dockerJdbcUrl(pgContainer),
    "JDBC_USER" -> pgContainer.username,
    "JDBC_PASSWORD" -> pgContainer.password,
    "MAIL_SMTP" -> mailEnv(SMTP_IP),
    "MAIL_SMPT_PORT" -> mailEnv(SMTP_PORT),
    "MAIL_USER" -> smtpUser,
    "MAIL_PASS" -> smtpPass
  )

  protected def mailEnv: Map[String, String] = Map(
    SMTP_PORT -> "1234",
    SMTP_IP -> "1234"
  )

  private def dockerJdbcUrl(pgContainer: PostgreSQLContainer): String = {
    import org.testcontainers.containers.{PostgreSQLContainer => OTCPostgreSQLContainer}

    val ip = getIp(pgContainer.container.getContainerInfo)
    s"jdbc:postgresql://$ip:${OTCPostgreSQLContainer.POSTGRESQL_PORT}/test"
  }

  protected def getIp(c: InspectContainerResponse): String =
    c.getNetworkSettings.getNetworks.asScala.values.head.getIpAddress

  protected def sleep(timeOut: scala.concurrent.duration.Duration): Unit = Thread.sleep(timeOut.toMillis)

  private def containers(): Seq[LazyContainer[Container]] = {
    Seq(LazyContainer(pgContainer), LazyContainer(app))
  }

  private def appContainers(): Seq[LazyContainer[Container]] = additionalContainer(containers())

  implicit class HttpExtension(value: HttpRequest) {
    protected val authHeader = "Authorization"
    import scala.concurrent.duration._

    def defaultHeaders(token: String): HttpRequest =
      value
        .header("content-type", "application/json")
        .header("Accept-Language", "en-US, en;q=0.9")
        .header(authHeader, token)

    def defaultTimeout: HttpRequest = timeout(5.second)

    def timeout(timeOut: scala.concurrent.duration.Duration): HttpRequest =
      value.timeout(timeOut.toMillis.toInt, timeOut.toMillis.toInt)
  }

  private def dockerLog(out: OutputFrame): Unit = {
    out.getType match {
      case OutputType.STDERR => log.error(out.getUtf8String)
      case _                 => log.info(out.getUtf8String)
    }
  }

}
