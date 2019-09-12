package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsString, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import helpers.PasswordHash

import models._
import scala.concurrent.ExecutionContext

@Singleton
class AdminController @Inject() (
  dbApi: DBApi,
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  passwordHash: PasswordHash,
  val userRepo: UserRepo,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) with I18nSupport with AuthenticatedSupport {
  val logger = Logger(getClass)
  val db = dbApi.database("default")

  def top = authenticated(parsers.anyContent) { implicit req =>
    Ok("")
  }
}
