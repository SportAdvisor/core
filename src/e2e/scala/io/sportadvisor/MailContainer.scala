package io.sportadvisor

import com.dimafeng.testcontainers._
import io.circe.generic.semiauto._
import io.circe.Decoder
import io.circe.parser._
import scalaj.http.Http

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait MailContainer extends BaseE2ETest {

  private val smptPort = 1025

  lazy val mailContainer: GenericContainer =
    GenericContainer("mailhog/mailhog:latest", env = smtpEnv())
  lazy val env: Map[String, String] = Map(
    SMTP_IP -> getIp(mailContainer.container.getContainerInfo),
    SMTP_PORT -> smptPort.toString,
    MAIL_HOG_API_PORT -> 8025.toString
  )

  override def additionalContainer(
      initial: Seq[LazyContainer[Container]]): Seq[LazyContainer[Container]] = {
    val seq = Seq(LazyContainer(mailContainer: Container)) ++ initial
    super.additionalContainer(seq)
  }

  private def smtpEnv(): Map[String, String] = Map(
    "MH_HOSTNAME" -> "sportadvisor.io"
  )

  override protected def mailEnv: Map[String, String] = env

  def messages(): Seq[Message] = {
    mhMessages map (Message(_))
  }

  final case class Message(from: String, to: Seq[String], body: String, subject: String)

  private object Message {
    def apply(mh: MailhogMessage): Message = {
      val from = mapPath(mh.From)
      val to = mh.To.map(mapPath)
      val body = mh.Content.Body
      new Message(from, to, body, mh.Content.Headers.Subject.head)
    }

    def mapPath: Path => String = p => s"${p.Mailbox}@${p.Domain}"
  }

  private def mhMessages(): Seq[MailhogMessage] = {
    val resp = Http(s"http://${mailEnv(SMTP_IP)}:${mailEnv(MAIL_HOG_API_PORT)}/api/v2/messages").asString
    parse(resp.body) match {
      case Left(e) => throw new IllegalStateException(e)
      case Right(root) =>
        respDecoder.decodeJson(root) match {
          case Left(e)  => throw new IllegalStateException(e)
          case Right(r) => r.items
        }
    }
  }

  private final case class ApiResponse(items: Seq[MailhogMessage])
  private final case class MailhogMessage(From: Path, To: Seq[Path], Content: ContentValue)

  private final case class Path(Mailbox: String, Domain: String)
  private final case class ContentValue(Body: String, Headers: HeaderValue)
  private final case class HeaderValue(Subject: Seq[String])

  private implicit val pathDecoder: Decoder[Path] = deriveDecoder
  private implicit val contentDecoder: Decoder[ContentValue] = deriveDecoder
  private implicit val headerDecoder: Decoder[HeaderValue] = deriveDecoder
  private implicit val messageDecoder: Decoder[MailhogMessage] = deriveDecoder
  private implicit val respDecoder: Decoder[ApiResponse] = deriveDecoder

}
