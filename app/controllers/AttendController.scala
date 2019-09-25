package controllers

import java.time.{Instant, LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.libs.json.{JsString, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import helpers.{PasswordHash, TimeZoneInfo}
import models._
import play.api.data.validation.Constraints

import scala.concurrent.ExecutionContext
import play.api.db.Database
import play.api.data.validation.Constraints

@Singleton
class AttendController @Inject() (
  db: Database,
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  siteRepo: SiteRepo,
  implicit val ec: ExecutionContext,
  authenticated: NeedLogin.Authenticated
) extends AbstractController(cc) with I18nSupport {
  val logger = Logger(getClass)
  val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

  def index(siteId: Long) = Action { implicit req =>
    db.withConnection { implicit conn =>
      siteRepo.get(SiteId(siteId)) match {
        case None =>
          NotFound(
            Json.obj(
              "errorCode" -> "siteNotFound",
              "errorMessage" -> Messages("siteNotFound")
            )
          )
        case Some(site) =>
          val tzInfo = TimeZoneInfo.tableByZoneId(site.heldOnZoneId);

          Ok(
            Json.obj(
              "siteId" -> site.id.get.value,
              "siteName" -> site.siteName,
              "heldOn" -> formatter.withZone(tzInfo.zoneId).format(site.heldOnUtc),
              "timezone" -> tzInfo.view
            )
          )
      }
    }
  }
}
