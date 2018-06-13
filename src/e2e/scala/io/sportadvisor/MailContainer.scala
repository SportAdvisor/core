package io.sportadvisor
import com.dimafeng.testcontainers._

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait MailContainer extends BaseE2ETest {

  lazy val mailContainer: Container =
    GenericContainer("mailhog/mailhog:latest", exposedPorts = Seq(smtpPort), env = smtpEnv())

  override def additionalContainer(
      initial: Seq[LazyContainer[Container]]): Seq[LazyContainer[Container]] = {
    val seq = Seq(LazyContainer(mailContainer)) ++ initial
    super.additionalContainer(seq)
  }

  private def smtpEnv(): Map[String, String] = Map(
    "MH_HOSTNAME" -> "sportadvisor.io"
  )
}
