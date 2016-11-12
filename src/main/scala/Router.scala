import akka.actor.Actor
import akka.actor._
import spray.routing._
import spray.http._
import MediaTypes._
import spray.routing.authentication.ContextAuthenticator

import spray.routing.AuthenticationFailedRejection

import scala.concurrent.Future

class Router(userController: UserController, facebookParser: ActorRef) extends Actor with HttpService {

  import context.dispatcher

  def actorRefFactory = context

  def receive = runRoute(registerRoute ~ loginRoute ~ oauthRoute)

  private def authenticateUser(email: String, password: String): ContextAuthenticator[String] = {
    type Authentication[T] = Either[Rejection, T]
    ctx =>
    {
      Future {
        Either.cond(userController.userExists(email, password),
          email,
          AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
      }
    }
  }

  private val registerRoute = {
    path("register") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            HttpResponse(StatusCodes.OK, HttpEntity(html.register().toString))
          }
        }
      } ~
      post {
        entity(as[FormData]) { formData: FormData =>
          val formInput = formData.fields.toMap
          userController.validate(formInput) match {
            case None =>
              userController.createUser(formInput)
              redirect("/login", StatusCodes.Found)

            case Some(errors) =>
              respondWithMediaType(`text/html`) {
                complete {
                  HttpResponse(StatusCodes.OK, HttpEntity(html.register(errors).toString))
                }
              }
          }
        }
      }
    }

  }

  private val loginErrorRoute = respondWithMediaType(`text/html`) {
    complete {
      HttpResponse(StatusCodes.OK, HttpEntity(html.login(List("Entered E-mail and password don't match")).toString))
    }
  }

  private val loginRoute = {
    path("login") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            HttpResponse(StatusCodes.OK, HttpEntity(html.login().toString))
          }
        }
      } ~
      post {
        entity(as[FormData]) { formData: FormData =>
          val formInput = formData.fields.toMap

          (formInput.get("email"), formInput.get("password")) match {
            case (Some(email), Some(password)) if userController.userExists(email, password) =>
              // TODO: yeah, such security sucks
              setCookie(
                HttpCookie("email", email, httpOnly = true),
                HttpCookie("password", password, httpOnly = true)
              ) {
                authenticate(authenticateUser(email, password)) { _ =>
                  redirect("/oauth", StatusCodes.Found)
                } ~ loginErrorRoute
              }

            case _ =>
              loginErrorRoute
          }

        }
      }
    }
  }

  private val oauthRoute = {
    path("oauth") {
      cookie("email") { email =>
        cookie("password") { password =>
          authenticate(authenticateUser(email.content, password.content)) { email =>
            get {
              respondWithMediaType(`text/html`) {
                complete {
                  HttpResponse(StatusCodes.OK, HttpEntity(html.oauth(email).toString))
                }
              }
            } ~
            post {
              entity(as[FormData]) { formData: FormData =>
                val accessToken = formData.fields.toMap.getOrElse("access-token", "")
                facebookParser ! ParseFacebook(email, accessToken)

                respondWithMediaType(`text/html`) {
                  complete {
                    HttpResponse(StatusCodes.OK, HttpEntity(html.oauth(email, accessToken).toString))
                  }
                }
              }
            }
          } ~ redirect("/login", StatusCodes.Found)
        } ~ redirect("/login", StatusCodes.Found)
      } ~ redirect("/login", StatusCodes.Found)

    }
  }

}
