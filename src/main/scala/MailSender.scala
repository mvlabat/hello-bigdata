import akka.actor.Actor
import org.slf4j.LoggerFactory

case class SendMail()

class MailSender extends Actor {

  val logger = LoggerFactory.getLogger(classOf[MailSender])

  def receive = {

    case SendMail =>
      // TODO: send email
      logger.debug("Imagine E-mail being sent") // I'm not sure I've got this working
      println("Imagine E-mail being sent")

      /*send a Mail (
        from = ("agent.smith@matrix.com", "Agent Smith"),
        to = "mvlabat@gmail.com",
        subject = "Import stuff",
        message = "Mister Anderson..."
      )*/

  }

}
