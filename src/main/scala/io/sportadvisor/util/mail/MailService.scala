package io.sportadvisor.util.mail

import io.sportadvisor.util.Config.MailConfig
import io.sportadvisor.util.i18n.I18n

import scala.concurrent.ExecutionContext

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait MailService[I <: I18n, L, R] {
  def mailRender: MailRenderService[I]

  def mailSender: MailSenderService[L, R]
}

private[mail] class MailServiceImpl(config: MailConfig)(implicit executionContext: ExecutionContext)
    extends MailService[I18n, Throwable, Unit] {

  private[this] lazy val sender = CourierMailSenderService(config)
  private[this] lazy val render = ScalateRenderService()

  override def mailRender: MailRenderService[I18n] = render

  override def mailSender: MailSenderService[Throwable, Unit] = sender

}

object MailService {
  def apply(config: MailConfig)(
      implicit executionContext: ExecutionContext): MailService[I18n, Throwable, Unit] =
    new MailServiceImpl(config)
}
