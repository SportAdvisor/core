package io.sportadvisor.util.mail

import io.sportadvisor.util.i18n.I18n
import org.fusesource.scalate.TemplateEngine

/**
  * @author sss3 (Vladimir Alekseev)
  */
@SuppressWarnings(Array("org.wartremover.warts.Any"))
trait MailRenderService[I <: I18n] {

  def render(templateName: String, data: Map[String, Any]): String

  def renderI18n(templateName: String, data: Map[String, Any], i18n: I): String

}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
class ScalateRenderService[I <: I18n](engine: TemplateEngine) extends MailRenderService[I] {

  override def render(templateName: String, data: Map[String, Any]): String = {
    engine.layout(templateName, data)
  }

  override def renderI18n(templateName: String, data: Map[String, Any], i18n: I): String = {
    engine.layout(templateName, data ++ Map("i18n" -> i18n))
  }

}

object ScalateRenderService {
  private[this] lazy val engine = {
    val engine = new TemplateEngine
    engine.allowCaching = false
    engine
  }

  def apply(): ScalateRenderService[I18n] = new ScalateRenderService[I18n](engine)
}
