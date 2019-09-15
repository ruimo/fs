package models

import java.sql.Connection
import scala.collection.{immutable => imm}
import java.time.Instant

import anorm._
import scala.language.postfixOps

case class SiteId(value: Long) extends AnyVal

case class Site(
  id: Option[SiteId],
  siteName: String,
  createdAt: Instant
)

object Site {
  val simple = {
    SqlParser.get[Option[Long]]("site.site_id") ~
    SqlParser.get[String]("site.site_name") ~
    SqlParser.get[Instant]("site.created_at") map {
      case id~siteName~createdAt =>
        Site(id.map(SiteId.apply), siteName, createdAt)
    }
  }

  def create(
    siteName: String, now: Instant = Instant.now()
  )(implicit conn: Connection): Site = {
    SQL(
      """
      insert into site (
        site_id, site_name, created_at
      ) values (
        (select nextval('site_seq')),
        {siteName}, {createdAt}
      )
      """
    ).on(
      'siteName -> siteName,
      'createdAt -> now
    ).executeUpdate()

    val id: Long = SQL("select currval('site_seq')").as(SqlParser.scalar[Long].single)

    Site(Some(SiteId(id)), siteName, now)
  }
}
