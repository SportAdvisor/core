package io.sportadvisor.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
  * @author sss3 (Vladimir Alekseev)
  */
package object mail {

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

  def dateToString(date: LocalDateTime): String = DateTimeFormatter.ISO_DATE.format(date)

}
