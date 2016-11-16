import java.util.Calendar

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import Mail._

object Main extends App {
  implicit val system = ActorSystem("test_task")

  val database = system.actorOf(Props[Database])
  val userController = new UserController(database)
  val facebookParser = system.actorOf(Props(new FacebookParser(database)))
  val service = system.actorOf(Props(new Router(userController, facebookParser)), "router-service")

  import system.dispatcher
  val mailSender = system.actorOf(Props[MailSender])
  scheduleMails()

  // IO requires an implicit ActorSystem, and ? requires an implicit timeout
  // Bind HTTP to the specified service.
  implicit val timeout = Timeout(5.seconds)
  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)

  def scheduleMails() = {
    val scheduler = system.scheduler

    val desiredTime = 16.hours

    val calendar = Calendar.getInstance
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val difference = desiredTime.toMillis - (System.currentTimeMillis() - calendar.getTimeInMillis)
    val scheduleTime = if (difference < 0) 1.day.toMillis - difference else difference
    scheduler.schedule(scheduleTime.millis, 1.day) {
      mailSender ! SendMail
    }
  }
}
