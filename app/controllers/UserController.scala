package controllers

import play.api.i18n.Messages
import java.net.URLDecoder

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsString, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.ZoneOffset.UTC

import helpers.PasswordHash
import helpers.Sanitizer
import models._

import scala.concurrent.ExecutionContext

case class Login(userName: String, password: String)
case class ChangePassword(currentPassword: String, newPasswords: (String, String))

@Singleton
class UserController @Inject() (
  parsers: PlayBodyParsers,
  implicit val ec: ExecutionContext,
  dbApi: DBApi,
  val userRepo: UserRepo,
  passwordHash: PasswordHash,
  cc: ControllerComponents
) extends AbstractController(cc) with I18nSupport with AuthenticatedSupport {
  val db = dbApi.database("default")
  val logger = Logger(getClass)

  val loginForm = Form(
    mapping(
      "userName" -> text(minLength = 8, maxLength = 24),
      "password" -> text(minLength = 8, maxLength = 24)
    )(Login.apply)(Login.unapply)
  )

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

  // def startLogin(url: String) = Action { implicit req =>
  //   logger.info("StartLogin(" + url + ")")
  //   db.withConnection { implicit conn =>
  //     val count = userRepo.count()
  //     if (count == 0) {
  //       logger.info("No users found. Creating first user.")
  //       val password = passwordHash.password()
  //       val (salt, hash) = passwordHash.generateWithSalt(password)
  //       val user = userRepo.create(
  //         name = userRepo.AdminName, "admin", None, "manager", "set@your.email", hash, salt
  //       )

  //       logger.info("--------------------")
  //       logger.info("Your first user '" + user.name + "' has password '" + password + "'")
  //       logger.info("--------------------")

  //       Ok(
  //         views.html.login(
  //           loginForm.fill(
  //             Login(userRepo.AdminName, "")
  //           ).discardingErrors.withGlobalError(Messages("checkLogFileForAdminPassword")),
  //           url
  //         )
  //       )
  //     }
  //     else {
  //       Ok(views.html.login(loginForm, url))
  //     }
  //   }
  // }

  def startLogin() = Action { implicit req =>
    logger.info("startLogin called.")
    db.withConnection { implicit conn =>
      val count = userRepo.count()
      if (count == 0) {
        logger.info("No users found. Creating first user.")
        val password = passwordHash.password()
        val (salt, hash) = passwordHash.generateWithSalt(password)
        val user = userRepo.create(
          name = userRepo.AdminName, "set@your.email", hash, salt
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

  def login() = Action { implicit req =>
    logger.info("login called.")
    loginForm.bind(req.body.asJson.get).fold(
      formWithError => {
        println("stdout error." + formWithError)
        logger.error("UserController.login validation error " + formWithError)
        BadRequest(formWithError.errorsAsJson)
      },
      login => db.withConnection { implicit conn =>
        userRepo.login(login.userName, login.password) match {
          case None => BadRequest(
            loginForm.fill(login).withGlobalError(Messages("nameAndPasswordNotMatched")).errorsAsJson
          )
          case Some(user) =>
            Ok("").withSession(req.session + LoginSession.loginSessionString(user.id.get.value))
        }
      }
    )
  }

  def logoff = authenticated(parsers.anyContent) { implicit req =>
    Ok("").withSession(req.session - LoginSession.LoginSessionKey)
  }

  def changePassword = authenticated(parsers.anyContent) { implicit req =>
    changePasswordForm.bind(req.body.asJson.get).fold(
      formWithError => {
        logger.error("UserController.changePassword validation error: " + formWithError)
        BadRequest(formWithError.errorsAsJson)
      },
      newPassword => db.withConnection { implicit conn =>
        if (
          userRepo.changePassword(
            UserId(req.user.session.userId),
            newPassword.currentPassword, newPassword.newPasswords._1
          )
        ) Ok("")
        else BadRequest(
          changePasswordForm.withGlobalError("passwordDoesnotMatch").errorsAsJson
        )
      }
    )
  }
}
