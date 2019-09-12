package controllers

import play.api.i18n.Messages
import java.net.URLDecoder

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsString, Json, JsObject}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.ZoneOffset.UTC

import helpers.PasswordHash
import helpers.Sanitizer

@Singleton
class MessageController @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) with I18nSupport {
  val logger = Logger(getClass)

  def messages() = Action { implicit req =>
    val keyArgs = (req.body.asJson.get \ "keyArgs").as[Seq[JsObject]]
    Ok(
      new JsObject(
        keyArgs.map { ka =>
          val key: String = (ka \ "key").as[String]
          val args: Seq[String] = (ka \ "args").asOpt[Seq[String]].getOrElse(Seq[String]())
          val msg: String = Messages(key, args: _*)
          key -> JsString(msg)
        }.toMap
      )
    )
  }
}


