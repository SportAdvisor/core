package io.sportadvisor.util.mail

/**
  * @author sss3 (Vladimir Alekseev)
  */
object MailModel {

  trait MailContent {
    def content: String
  }

  final case class HtmlContent(content: String) extends MailContent
  final case class RawContent(content: String) extends MailContent

  final case class MailMessage(to: Seq[String],
                               cc: Seq[String],
                               bcc: Seq[String],
                               subject: String,
                               content: MailContent)

}
