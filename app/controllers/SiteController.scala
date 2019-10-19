package controllers

import java.time.{Instant, LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter

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
import play.api.data.validation.Constraints

import scala.concurrent.ExecutionContext
import play.api.db.Database
import play.api.data.validation.Constraints

@Singleton
class SiteController @Inject() (
  db: Database,
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  siteRepo: SiteRepo,
  authenticated: NeedLogin.Authenticated,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) with I18nSupport {
  val logger = Logger(getClass)
  val createSiteForm = Form(
    mapping(
      "siteName" -> text(minLength = 8, maxLength = 64),
      "dateTime" -> localDateTime("yyyy/MM/dd HH:mm").verifying("error.datetime", _ != null),
      "timeZoneIndex" -> number
    )(CreateSite.apply)(CreateSite.unapply)
  )
  val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

  def createSite = authenticated(parsers.anyContent) { implicit req =>
    createSiteForm.bind(req.body.asJson.get).fold(
      formWithError => {
        logger.error("createSite validation error " + formWithError)
        BadRequest(formWithError.errorsAsJson(req))
      },
      site => db.withConnection { implicit conn =>
        try {
          val newSite = siteRepo.create(site.siteName, site.utc, site.timeZone.zoneId, req.login.user.id.get)

          Ok(
            Json.obj(
              "id" -> newSite.id.get.value.toString
            )
          )
        } catch {
          case e: UniqueConstraintException =>
            Conflict(
              Json.obj(
                "errorCode" -> "recordWithSameNameExists"
              )
            )
        }
      }
    )
  }

  def updateSite(siteId: Long) = authenticated(parsers.anyContent) { implicit req =>
    createSiteForm.bind(req.body.asJson.get).fold(
      formWithError => {
        logger.error("updateSite validation error " + formWithError)
        BadRequest(formWithError.errorsAsJson(req))
      },
      newSite => db.withConnection { implicit conn =>
        try {
          val id = SiteId(siteId)
          val userId: Option[UserId] = req.login.user.id
          siteRepo.get(id) match {
            case None =>
              NotFound("")
            case Some(site) =>
              if (site.owner == userId.get || req.login.isSuper) {
                siteRepo.update(
                  SiteId(siteId), newSite.siteName, newSite.utc, newSite.timeZone.zoneId, site.owner
                )

                Ok(
                  Json.obj(
                    "id" -> siteId.toString
                  )
                )
              } else {
                Forbidden("")
              }
          }
        } catch {
          case e: UniqueConstraintException =>
            Conflict(
              Json.obj(
                "errorCode" -> "recordWithSameNameExists"
              )
            )
        }
      }
    )
  }

  def listSiteToUpdate(
    page: Int, pageSize: Int, orderBySpec: String
  ) = authenticated(parsers.anyContent) { implicit req =>
    db.withConnection { implicit conn =>
      val owner = if (req.login.isSuper) None else req.login.user.id
      val list: PagedRecords[(Site, User)] = siteRepo.listWithOwner(page, pageSize, OrderBy(orderBySpec), owner)
      Ok(
        Json.obj(
          "page" -> list.currentPage,
          "pageSize" -> list.pageSize,
          "table" -> list.records.map { case (site, user) =>
              Json.obj(
                "siteId" -> site.id.get.value,
                "siteName" -> site.siteName,
                "dateTime" -> formatter.format(ZonedDateTime.ofInstant(site.heldOnUtc, site.heldOnZoneId)),
                "timeZone" -> (site.heldOnZoneId + "(" + site.heldOnZoneId.getRules.getOffset(Instant.EPOCH) + ")"),
                "owner" -> user.name
              )
          }
        )
      )
    }
  }

  def listSite(
    page: Int, pageSize: Int, orderBySpec: String
  ) = Action { implicit req =>
    db.withConnection { implicit conn =>
      val list: PagedRecords[(Site, User)] = siteRepo.listWithOwner(page, pageSize, OrderBy(orderBySpec), None)
      Ok(
        Json.obj(
          "page" -> list.currentPage,
          "pageSize" -> list.pageSize,
          "table" -> list.records.map { case (site, user) =>
              Json.obj(
                "siteId" -> site.id.get.value,
                "siteName" -> site.siteName,
                "dateTime" -> formatter.format(ZonedDateTime.ofInstant(site.heldOnUtc, site.heldOnZoneId)),
                "timeZone" -> (site.heldOnZoneId + "(" + site.heldOnZoneId.getRules.getOffset(Instant.EPOCH) + ")"),
                "owner" -> user.name
              )
          }
        )
      )
    }
  }

  def deleteSite(siteId: Long) = authenticated(parsers.anyContent) { implicit req =>
    val userId: Option[UserId] = req.login.user.id
    db.withConnection { implicit conn =>
      siteRepo.get(SiteId(siteId)) match {
        case None => Ok("")
        case Some(site) =>
          if (site.owner == userId.get || req.login.isSuper) {
            siteRepo.delete(siteId)
            Ok("")
          } else {
            Forbidden("")
          }
      }
    }
  }
}
