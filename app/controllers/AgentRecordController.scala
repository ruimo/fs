package controllers

import java.nio.file.Files

import scala.util.{Failure, Success, Try}
import java.time.{Instant, LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter

import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import javax.inject._
import play.api._
import play.api.mvc._
import helpers.{PasswordHash, Tsv}
import models._

import scala.concurrent.ExecutionContext
import play.api.db.Database

import scala.collection.{immutable => imm}

@Singleton
class AgentRecordController @Inject() (
  db: Database,
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  agentRecordRepo: AgentRecordRepo,
  siteRepo: SiteRepo,
  authenticated: NeedLogin.Authenticated,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) with I18nSupport {
  val logger = Logger(getClass)
  val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

  def toJson(map: imm.Map[AgentRecordPhase, (AgentRecord, Site)]): JsObject = JsObject {
    map.toSeq.map { case (key: AgentRecordPhase, value: (AgentRecord, Site)) =>
      val (ar, site) = value

      key.toString -> Json.obj(
        "id" -> ar.id.get.value,
        "faction" -> ar.faction,
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
            if (agentRecordRepo.getByAgentNameWithSite(siteId, tsv.agentName).get(phase).isDefined)
              Conflict(Json.obj("agentName" -> tsv.agentName))
            else {
              agentRecordRepo.create(
                siteId, tsv.faction, tsv.agentName, tsv.agentLevel, tsv.lifetimeAp, tsv.distanceWalked, phase, tsvStr
              )

              Ok(toJson(agentRecordRepo.getByAgentNameWithSite(siteId, tsv.agentName)))
            }
          } else {
            agentRecordRepo.delete(siteId, tsv.agentName, phase)
            agentRecordRepo.create(
              siteId, tsv.faction, tsv.agentName, tsv.agentLevel, tsv.lifetimeAp, tsv.distanceWalked, phase, tsvStr
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

  def list(siteId: Long, page: Int, pageSize: Int, orderBySpec: String) = Action { implicit req =>
    db.withConnection { implicit conn =>
      val site = siteRepo(SiteId(siteId))

      val records: PagedRecords[AgentRecordSumEntry] = agentRecordRepo.list(SiteId(siteId), page, pageSize, OrderBy(orderBySpec))
      val pagination: Option[Pagination] = Pagination.get(records)
      Ok(
        pagination.map { p =>
          Json.obj(
            "pagination" -> Json.obj(
              "topButtonExists" -> p.topButtonExists,
              "lastButtonExists" -> p.lastButtonExists,
              "startPage" -> p.startPage,
              "showPageCount" -> p.showPageCount
            )
          )
        }.getOrElse(Json.obj()) ++
        Json.obj(
          "pageControl" -> Json.obj(
            "currentPage" -> records.currentPage,
            "pageSize" -> records.pageSize,
            "pageCount" -> records.pageCount,
            "nextPageExists" -> records.nextPageExists,
            "prevPageExists" -> records.prevPageExists,
            "orderByCol" -> records.orderBy.columnName,
            "orderBySort" -> records.orderBy.order.toString
          ),
          "site" -> Json.obj(
            "siteName" -> site.siteName
          ),
          "table" -> JsArray(
            records.records.zipWithIndex.map { case (r, i) =>
              Json.obj(
                "rank" -> (records.offset + i),
                "faction" -> r.faction,
                "agentName" -> r.agentName,
                "faction" -> r.faction,
                "startLevel" -> r.startAgentLevel,
                "endLevel" -> r.endAgentLevel,
                "earnedLevel" -> r.earnedAgentLevel,
                "startAp" -> r.startLifetimeAp,
                "endAp" -> r.endLifetimeAp,
                "earnedAp" -> r.earnedLifetimeAp,
                "startWalked" -> r.startDistanceWalked,
                "endWalked" -> r.endDistanceWalked,
                "earnedWalked" -> r.earnedDistanceWalked,
                "createdAt" -> formatter.format(ZonedDateTime.ofInstant(r.createdAt, site.heldOnZoneId))
              )
            }.toSeq
          )
        )
      )
    }
  }

  def download(siteId: Long, orderBySpec: String) = Action { implicit req =>
    db.withConnection { implicit conn =>
      val orderBy = OrderBy(orderBySpec)
      val file = Files.createTempFile(null, null)
      agentRecordRepo.downloadTsv(SiteId(siteId), orderBy, file)
      Ok.sendPath(
        file, fileName = _ => orderBy.columnName + "_" + orderBy.order + ".tsv", onClose = () => Files.delete(file)
      )
    }
  }

  def deleteRecords(sid: Long) = authenticated(parsers.anyContent) { implicit req =>
    val userId: Option[UserId] = req.login.user.id
    val siteId = SiteId(sid)
    db.withConnection { implicit conn =>
      siteRepo.get(siteId) match {
        case None => Ok("")
        case Some(site) =>
          if (site.owner == userId.get || req.login.isSuper) {
            agentRecordRepo.deleteRecords(siteId)
            Ok("")
          } else {
            Forbidden("")
          }
      }
    }
  }

  def deleteAgentRecord(sid: Long, agentName: String) = authenticated(parsers.anyContent) { implicit req =>
    val userId: Option[UserId] = req.login.user.id
    val siteId = SiteId(sid)
    db.withConnection { implicit conn =>
      siteRepo.get(siteId) match {
        case None => Ok("")
        case Some(site) =>
          if (site.owner == userId.get || req.login.isSuper) {
            agentRecordRepo.deleteRecordsByAgentName(agentName)
            Ok("")
          } else {
            Forbidden("")
          }
      }
    }
  }
}
