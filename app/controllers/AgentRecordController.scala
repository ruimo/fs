package controllers

import scala.util.{Try, Success, Failure}
import java.time.{Instant, LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsObject, JsString, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import helpers.{PasswordHash, Tsv}
import models._
import play.api.data.validation.Constraints

import scala.concurrent.ExecutionContext
import play.api.db.Database
import play.api.data.validation.Constraints

import scala.collection.{immutable => imm}

@Singleton
class AgentRecordController @Inject() (
  db: Database,
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  agentRecordRepo: AgentRecordRepo,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) with I18nSupport {
  val logger = Logger(getClass)
  val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

  def toJson(map: imm.Map[AgentRecordPhase, (AgentRecord, Site)]): JsObject = JsObject {
    map.toSeq.map { case (key: AgentRecordPhase, value: (AgentRecord, Site)) =>
      val (ar, site) = value

      key.toString -> Json.obj(
        "id" -> ar.id.get.value,
        "agentName" -> ar.agentName,
        "agentLevel" -> ar.agentLevel,
        "lifetimeAp" -> ar.lifetimeAp.toString,
        "distanceWalked" -> ar.distanceWalked,
        "createdAt" -> formatter.withZone(site.heldOnZoneId).format(ar.createdAt)
      )
    }
  }

  def createAgentRecord = Action { implicit req =>
    val json = req.body.asJson.get
    val siteId: SiteId = SiteId((json \ "siteId").as[String].toLong)
    val phase: AgentRecordPhase = (json \ "phase").as[String] match {
      case "END" => AgentRecordPhase.END
      case _ => AgentRecordPhase.START
    }
    val tsvStr: String = (json \ "tsv").as[String]
    val overwrite: Boolean = (json \ "overwrite").as[Boolean]
    Try(Tsv.parse(tsvStr)) match {
      case Failure(e) =>
        logger.error("Tsv parse error.", e)
        BadRequest(
          Json.obj(
            "errorCode" -> "csvFormatError"
          )
        )

      case Success(tsv) =>
        db.withConnection { implicit conn =>
          if (! overwrite) {
            if (agentRecordRepo.getByAgentNameWithSite(siteId, tsv.agentName).get(phase).isDefined) Conflict("")
            else {
              agentRecordRepo.create(
                siteId, tsv.agentName, tsv.agentLevel, tsv.lifetimeAp, tsv.distanceWalked, phase, tsvStr
              )

              Ok(toJson(agentRecordRepo.getByAgentNameWithSite(siteId, tsv.agentName)))
            }
          } else {
            agentRecordRepo.delete(siteId, tsv.agentName, phase)
            agentRecordRepo.create(
              siteId, tsv.agentName, tsv.agentLevel, tsv.lifetimeAp, tsv.distanceWalked, phase, tsvStr
            )

            Ok(toJson(agentRecordRepo.getByAgentNameWithSite(siteId, tsv.agentName)))
          }
        }
    }
  }

  def registeredRecords(siteId: Long, agentName: String) = Action { implicit req =>
    db.withConnection { implicit conn =>
      Ok(toJson(agentRecordRepo.getByAgentNameWithSite(SiteId(siteId), agentName)))
    }
  }
}
