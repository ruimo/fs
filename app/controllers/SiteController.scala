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
import play.api.db.Database

@Singleton
class SiteController @Inject() (
  db: Database,
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext,
  authenticated: NeedLogin.Authenticated
) extends AbstractController(cc) with I18nSupport {
  val logger = Logger(getClass)
  val createSiteForm = Form(
    mapping(
      "siteName" -> text(minLength = 8, maxLength = 128)
    )(CreateSite.apply)(CreateSite.unapply)
  )

  def createSite = authenticated(parsers.anyContent) { implicit req =>
    if (req.login.isSuper) {
      createSiteForm.bind(req.body.asJson.get).fold(
        formWithError => {
          logger.error("createSite validation error " + formWithError)
          BadRequest(formWithError.errorsAsJson(req))
        },
        site => db.withConnection { implicit conn =>
          val newSite = Site.create(site.siteName)
          Ok(
            Json.obj(
              "id" -> newSite.id.get.value.toString
            )
          )
        }
      )
    } else {
      Unauthorized("")
    }
  }
}
