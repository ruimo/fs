package controllers

import java.sql.Connection

import javax.inject.Inject
import helpers.PasswordHash
import models.{LoginSession, LoginSessionRepo, User}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._
import play.api.mvc.Results.{Redirect, Unauthorized}

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.db.Database
import play.api.libs.json.Json

object NeedLogin {
  val logger = Logger(getClass)

  def onUnauthorized(request: RequestHeader): Result = Unauthorized(
    Json.obj(
      "errorCode" -> "loginRequired"
    )
  )

  class UserAuthenticatedBuilder (
    parser: BodyParser[AnyContent],
    loginSessionRepo: LoginSessionRepo
  )(
    implicit ec: ExecutionContext
  ) extends AuthenticatedBuilder[LoginSession](
    { req: RequestHeader =>
      loginSessionRepo.fromRequest(req)
    },
    parser,
    onUnauthorized
  ) {
    @Inject()
    def this (
      parser: BodyParsers.Default,
      loginSessionRepo: LoginSessionRepo
    )(
      implicit ec: ExecutionContext
    ) = {
      this (parser: BodyParser[AnyContent], loginSessionRepo)
    }
  }

  class OptUserAuthenticatedBuilder (
    parser: BodyParser[AnyContent],
    loginSessionRepo: LoginSessionRepo
  )(
    implicit ec: ExecutionContext
  ) extends AuthenticatedBuilder[Option[LoginSession]](
    { req: RequestHeader =>
      Some(loginSessionRepo.fromRequest(req))
    },
    parser,
    onUnauthorized
  ) {
    @Inject()
    def this (
      parser: BodyParsers.Default,
      loginSessionRepo: LoginSessionRepo
    )(
      implicit ec: ExecutionContext
    ) = {
      this (parser: BodyParser[AnyContent], loginSessionRepo)
    }
  }

  class Authenticated(
    val parser: BodyParser[AnyContent],
    messagesApi: MessagesApi,
    builder: AuthenticatedBuilder[LoginSession]
  )(
    implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[AuthMessagesRequest, AnyContent] {
    type ResultBlock[A] = (AuthMessagesRequest[A]) => Future[Result]

    @Inject
    def this (
      parser: BodyParsers.Default,
      messagesApi: MessagesApi,
      builder: UserAuthenticatedBuilder
    )(implicit ec: ExecutionContext) =
      this (parser: BodyParser[AnyContent], messagesApi, builder)

    def invokeBlock[A](request: Request[A], block: ResultBlock[A]): Future[Result] =
      builder.authenticate(request, { authRequest: AuthenticatedRequest[A, LoginSession] =>
        block(new AuthMessagesRequest[A](authRequest.user, messagesApi, request))
      })
  }

  class OptAuthenticated(
    val parser: BodyParser[AnyContent],
    messagesApi: MessagesApi,
    builder: AuthenticatedBuilder[Option[LoginSession]]
  )(
    implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[OptAuthMessagesRequest, AnyContent] {
    type ResultBlock[A] = (OptAuthMessagesRequest[A]) => Future[Result]

    @Inject
    def this (
      parser: BodyParsers.Default,
      messagesApi: MessagesApi,
      builder: OptUserAuthenticatedBuilder
    )(implicit ec: ExecutionContext) =
      this (parser: BodyParser[AnyContent], messagesApi, builder)

    def invokeBlock[A](request: Request[A], block: ResultBlock[A]): Future[Result] =
      builder.authenticate(request, { authRequest: AuthenticatedRequest[A, Option[LoginSession]] =>
        block(new OptAuthMessagesRequest[A](authRequest.user, messagesApi, request))
      })
  }
}
