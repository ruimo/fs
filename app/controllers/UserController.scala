package controllers

import play.api.i18n.Messages
import java.net.URLDecoder

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsString, JsValue, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.ZoneOffset.UTC

import helpers.PasswordHash
import helpers.Sanitizer
import models._
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._

import scala.concurrent.ExecutionContext

case class Login(userName: String, password: String)
case class ChangePassword(currentPassword: String, newPasswords: (String, String))

@Singleton
class UserController @Inject() (
  parsers: PlayBodyParsers,
  dbApi: DBApi,
  val userRepo: UserRepo,
  passwordHash: PasswordHash,
  authenticated: NeedLogin.Authenticated,
  optAuthenticated: NeedLogin.OptAuthenticated,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) with I18nSupport {
  val db = dbApi.database("default")
  val logger = Logger(getClass)

  val createUserForm = Form(
    mapping(
      "userName" -> text(minLength = 8, maxLength = 32),
      "email" -> email.verifying(nonEmpty),
      "password" -> text(minLength = 8, maxLength = 32)
    )(CreateUser.apply)(CreateUser.unapply)
  )

  val loginForm = Form(
    mapping(
      "userName" -> text(minLength = 8, maxLength = 24),
      "password" -> text(minLength = 8, maxLength = 24)
    )(Login.apply)(Login.unapply)
  )

  def createUser = authenticated(parsers.anyContent) { implicit req =>
    if (req.login.isSuper) {
      createUserForm.bind(req.body.asJson.get).fold(
        formWithError => {
          logger.error("createUser validation error " + formWithError)
          BadRequest(formWithError.errorsAsJson(req))
        },
        user => db.withConnection { implicit conn =>
          try {
            val (salt, hash) = passwordHash.generateWithSalt(user.password)
            val newUser = userRepo.create(
              user.userName, user.email, hash, salt, UserRole.ADMIN
            )

            Ok(Json.obj("id" -> newUser.id.get.value.toString))
          } catch {
            case e: UniqueConstraintException =>
              logger.error("createUser unique constraint exception: " + user)
              Conflict(
                Json.obj(
                  "errorCode" -> "recordWithSameNameExists"
                )
              )
          }
        }
      )
    } else {
      Forbidden("")
    }
  }

  def changePasswordForm = Form(
    mapping(
      "currentPassword" -> text(minLength = 8, maxLength = 24),
      "newPasswords" -> tuple(
        "main" -> text(minLength = 8, maxLength = 24),
        "confirm" -> text
      ).verifying(
        "confirmPasswordDoesNotMatch", passwords => passwords._1 == passwords._2
      )
    )(ChangePassword.apply)(ChangePassword.unapply)
  )

  def startLogin() = Action { implicit req =>
    logger.info("startLogin called.")
    db.withConnection { implicit conn =>
      val count = userRepo.count()
      if (count == 0) {
        logger.info("No users found. Creating first user.")
        val password = passwordHash.password()
        val (salt, hash) = passwordHash.generateWithSalt(password)
        val user = userRepo.create(
          name = userRepo.AdminName, "set@your.email", hash, salt, UserRole.SUPER
        )

        logger.info("--------------------")
        logger.info("Your first user '" + user.name + "' has password '" + password + "'")
        logger.info("--------------------")

        Ok(Json.obj("message" -> Messages("initialApplicationStart")))
      } else {
        Ok(Json.obj())
      }
    }
  }

  def userToJson(user: User): JsValue = Json.obj(
    "id" -> user.id.get.value.toString,
    "name" -> user.name,
    "role" ->  user.role.ordinal
  )

  def login() = Action { implicit req =>
    logger.info("login called.")
    loginForm.bind(req.body.asJson.get).fold(
      formWithError => {
        logger.error("login validation error " + formWithError)
        BadRequest(formWithError.errorsAsJson)
      },
      login => db.withConnection { implicit conn =>
        userRepo.login(login.userName, login.password) match {
          case None => BadRequest(
            loginForm.fill(login).withGlobalError(Messages("nameAndPasswordNotMatched")).errorsAsJson
          )
          case Some(user) =>
            Ok(userToJson(user)).withSession(req.session + LoginSession.loginSessionString(user))
        }
      }
    )
  }

  def logoff = Action { implicit req =>
    logger.info("logoff called.")
    Ok("").withSession(req.session - LoginSession.LoginSessionKey)
  }

  def changePassword = authenticated(parsers.anyContent) { implicit req =>
    changePasswordForm.bind(req.body.asJson.get).fold(
      formWithError => {
        logger.error("UserController.changePassword validation error: " + formWithError)
        BadRequest(formWithError.errorsAsJson(req))
      },
      newPassword => db.withConnection { implicit conn =>
        if (
          userRepo.changePassword(
            req.login.user.id.get,
            newPassword.currentPassword, newPassword.newPasswords._1
          )
        ) Ok("")
        else BadRequest(
          changePasswordForm.withGlobalError("passwordDoesnotMatch").errorsAsJson(req)
        )
      }
    )
  }

  def loginInfo = optAuthenticated (parsers.anyContent) { implicit req =>
    req.login match {
      case None => NotFound("")
      case Some(us) => Ok(userToJson(us.user))
    }
  }
}
