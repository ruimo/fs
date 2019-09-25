package controllers

import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsString, Json}
import javax.inject._
import play.api._
import play.api.mvc._
import helpers.{PasswordHash, TimeZoneInfo}

import scala.concurrent.ExecutionContext

@Singleton
class TimeZoneController @Inject() (
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) with I18nSupport {
  val logger = Logger(getClass)

  def table = Action { implicit req =>
    Ok(
      Json.obj(
        "table" -> TimeZoneInfo.table.map(e => JsString(e.view)).toSeq
      )
    )
  }
}
