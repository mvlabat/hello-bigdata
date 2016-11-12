import scala.collection.mutable.ListBuffer

class UserController(database: Database) {

  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

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
        else if (!database.emailDoesNotExist(email))
          errors += "Such E-mail already exists"

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
    database.createUser(formInput("email"), formInput("password"))
  }

  def userExists(email: String, password: String) = database.userExists(email, password)

}
