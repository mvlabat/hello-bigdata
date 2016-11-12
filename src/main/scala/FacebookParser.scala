import akka.actor.Actor
import scala.util.parsing.json.JSON
import spray.http._
import spray.client.pipelining._

import scala.concurrent.Future

case class ParseFacebook(email: String, accessToken: String)

class FacebookParser(database: Database) extends Actor {

  import context.dispatcher

  def receive = {
    case ParseFacebook(email, accessToken) =>
      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
      // Just gets user id.
      val response: Future[HttpResponse] = pipeline(Get(s"https://graph.facebook.com/me?fields=id&access_token=$accessToken"))

      // TODO: handle failures
      response onSuccess {
        case httpResponse =>
          println(httpResponse.entity.asString)
          // TODO: find an alternative to replace the deprecated class
          val id = JSON.parseFull(httpResponse.entity.asString).get.asInstanceOf[Map[String, String]]("id")
          // TODO: seems like there are some problems with concurrency that lead to exceptions
          // TODO: maybe we should refactor database to be an Actor too
          database.setUserId(email, id)
      }
  }

}
