package controllers

import play.api.db.Database
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import models._
import play.api.mvc.Security.AuthenticatedRequest

case class LoginUser(
  session: LoginSession,
  user: User
)

class MyAuthenticatedBuilder[U, P](
  userinfo: RequestHeader => Option[U],
  defaultParser: BodyParser[P],
  onUnauthorized: RequestHeader => Result= implicit request =>
    Unauthorized(
      views.html.defaultpages.unauthorized()
    )
  )(
    implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[({ type R[A] = AuthenticatedRequest[A, U] })#R, P] {

  lazy val parser = defaultParser

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) =
    authenticate(request, block)

  /**
    * Authenticate the given block.
    */
  def authenticate[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) = {
    userinfo(request).map { user =>
      block(new AuthenticatedRequest(user, request))
    } getOrElse {
      Future.successful(onUnauthorized(request))
    }
  }
}

trait AuthenticatedSupport {
  def db: Database
  def userRepo: UserRepo
  implicit val ec: ExecutionContext

  def onUnauthorized(request: RequestHeader) = Unauthorized("")

  implicit def loginUser(implicit request: RequestHeader): Option[LoginUser] =
    LoginSession.retrieveLogin(request) match {
      case None => None
      case Some(loginSession) => {
        if (loginSession.isExpired()) None
        else db.withConnection { implicit conn =>
          userRepo.get(UserId(loginSession.userId)).map { user =>
            LoginUser(loginSession, user)
          }
        }
      }
    }

  def authenticated[P](parser: BodyParser[P]) = new MyAuthenticatedBuilder[LoginUser, P](
    req => loginUser(req),
    parser,
    onUnauthorized
  )
}

