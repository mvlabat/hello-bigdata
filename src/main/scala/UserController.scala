import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

class UserController(database: ActorRef) {

  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  private val timeoutDuration = 5.seconds
  private implicit val timeout = Timeout(timeoutDuration)

  private def checkEmail(e: String): Boolean = e match {
    case str: String if str.trim.isEmpty => false
    case str: String if emailRegex.findFirstMatchIn(str).isDefined => true
    case _ => false
  }

  def validate(formInput: Map[String, String]): Option[List[String]] = {
    val errors = new ListBuffer[String]()

    formInput.get("email") match {
      case Some(email) =>
        if (!checkEmail(email))
          errors += "E-mail is invalid"
        else {
          val future = database ? CheckEmailDoesNotExistMessage(email)
          val emailExists = !Await.result(future.mapTo[Boolean], timeoutDuration)
          if (emailExists)
            errors += "Such E-mail already exists"
        }

      case None =>
        errors += "E-mail is not entered"
    }

    (formInput.get("password"), formInput.get("password-repeat")) match {
      case (Some(password), Some(passwordRepeat)) if password != passwordRepeat =>
        errors += "Passwords do not match"

      case (Some(_), None) | (None, Some(_)) | (None, None) =>
        errors += "Password is not entered"

      case _ =>
    }

    if (errors.isEmpty)
      None
    else
      Some(errors.toList)
  }

  def createUser(formInput: Map[String, String]) = {
    // We expect formInput being already validated.
    database ! CreateUserMessage(formInput("email"), formInput("password"))
  }

  def userExists(email: String, password: String) = {
    val future = database ? CheckUserExistsMessage(email, password)
    Await.result(future.mapTo[Boolean], timeoutDuration)
  }

}
