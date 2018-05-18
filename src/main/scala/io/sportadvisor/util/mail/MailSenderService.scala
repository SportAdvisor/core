package io.sportadvisor.util.mail

import courier.{Envelope, Mailer, Text}
import io.sportadvisor.util.Config.MailConfig
import javax.mail.internet.InternetAddress
import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait MailSenderService[L, R] {

  def send(mail: MailMessage): Future[Either[L, R]]

}

class CourierMailSenderService(val smtp: String,
                               val smtpPort: Int,
                               val user: String,
                               val pass: String)(implicit executionContext: ExecutionContext)
    extends MailSenderService[Throwable, Unit]
    with Logging {

  private lazy val mailer: Mailer = Mailer(smtp, smtpPort)
    .auth(true)
    .as(user, pass)
    .startTtls(true)()

  override def send(mail: MailMessage): Future[Either[Throwable, Unit]] = {
    val envelope = Envelope
      .from(new InternetAddress(user))
      .to(mapAddresses(mail.to): _*)
      .cc(mapAddresses(mail.cc): _*)
      .bcc(mapAddresses(mail.bcc): _*)
      .subject(mail.subject)
      .content(Text(mail.content.content))
    mailer(envelope).transform(expand())
  }

  private def mapAddresses(addresses: Seq[String]): Seq[InternetAddress] = {
    addresses.map(new InternetAddress(_))
  }

  private def expand(): Try[Unit] => Try[Either[Throwable, Unit]] = {
    case Failure(e) =>
      log.error("Failed send message", e)
      Success(Left(e))
    case Success(v) => Success(Right(v))
  }

}

object CourierMailSenderService {
  def apply(config: MailConfig)(
      implicit executionContext: ExecutionContext): CourierMailSenderService =
    new CourierMailSenderService(config.smtp, config.smtpPort, config.user, config.pass)
}
