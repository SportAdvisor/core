package io.sportadvisor
import com.dimafeng.testcontainers._

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait MailContainer extends BaseE2ETest {

  lazy val mailContainer =
    GenericContainer("mailhog/mailhog:latest", exposedPorts = List(smtpPort), env = smtpEnv())

  override def additionalContainer(): Seq[LazyContainer[Container]] = List(LazyContainer(mailContainer))

  private def smtpEnv(): Map[String, String] = Map(
    "MH_HOSTNAME" -> "sportadvisor.io"
  )
}
