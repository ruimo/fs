package models

import javax.inject.{Singleton, Inject}
import java.sql.Connection
import scala.collection.{immutable => imm}
import java.time.{Instant, ZoneId}

import anorm._
import scala.language.postfixOps

case class SiteId(value: Long) extends AnyVal

case class Site(
  id: Option[SiteId],
  siteName: String,
  heldOnUtc: Instant,
  heldOnZoneId: ZoneId,
  owner: UserId,
  createdAt: Instant
)

@Singleton
class SiteRepo @Inject() (
  userRepo: UserRepo
) {
  val simple = {
    SqlParser.get[Option[Long]]("site.site_id") ~
    SqlParser.get[String]("site.site_name") ~
    SqlParser.get[Instant]("site.held_on_utc") ~
    SqlParser.get[String]("site.held_on_zone_id") ~
    SqlParser.get[Long]("site.owner") ~
    SqlParser.get[Instant]("site.created_at") map {
      case id~siteName~heldOnUtc~heldOnZoneId~owner~createdAt =>
        Site(id.map(SiteId.apply), siteName, heldOnUtc, ZoneId.of(heldOnZoneId), UserId(owner), createdAt)
    }
  }

  val withUser = simple ~ userRepo.simple map {
    case site ~ user => (site, user)
  }

  def create(
    siteName: String, heldOnUtc: Instant, heldOnZoneId: ZoneId, owner: UserId, now: Instant = Instant.now()
  )(implicit conn: Connection): Site = ExceptionMapper.mapException {
    SQL(
      """
      insert into site (
        site_id, site_name, held_on_utc, held_on_zone_id, owner, created_at
      ) values (
        (select nextval('site_seq')),
        {siteName}, {heldOnUtc}, {heldOnZoneId}, {owner}, {createdAt}
      )
      """
    ).on(
      'siteName -> siteName,
      'heldOnUtc -> heldOnUtc,
      'heldOnZoneId -> heldOnZoneId.getId,
      'owner -> owner.value,
      'createdAt -> now
    ).executeUpdate()

    val id: Long = SQL("select currval('site_seq')").as(SqlParser.scalar[Long].single)

    Site(Some(SiteId(id)), siteName, heldOnUtc, heldOnZoneId, owner, now)
  }

  def update(
    id: SiteId, siteName: String, heldOnUtc: Instant, heldOnZoneId: ZoneId, owner: UserId
  )(implicit conn: Connection): Long = SQL(
    """
    update site set
       site_name = {siteName},
       held_on_utc = {heldOnUtc},
       held_on_zone_id = {heldOnZoneId},
       owner = {owner}
    where site_id = {id}
    """
  ).on(
    'id -> id.value,
    'siteName -> siteName,
    'heldOnUtc -> heldOnUtc,
    'heldOnZoneId -> heldOnZoneId.getId,
    'owner -> owner.value
  ).executeUpdate()

  def get(siteId: SiteId)(implicit conn: Connection): Option[Site] = SQL(
    "select * from site where site_id = {siteId}"
  ).on(
    'siteId -> siteId.value
  ).as(simple.singleOpt)

  def getWithUser(siteId: SiteId)(implicit conn: Connection): Option[(Site, User)] = SQL(
    "select * from site inner join users on site.owner = users.user_id where site_id = {siteId}"
  ).on(
    'siteId -> siteId.value
  ).as(withUser.singleOpt)

  def delete(siteId: Long)(implicit conn: Connection): Long = SQL(
    "delete from site where site_id = {siteId}"
  ).on(
    'siteId -> siteId
  ).executeUpdate()

  def listWithOwner(page: Int = 0, pageSize: Int = 10, orderBy: OrderBy, owner: Option[UserId])(
    implicit conn: Connection
  ): PagedRecords[(Site, User)] = {
    import scala.language.postfixOps
    
    val offset: Int = pageSize * page
    val baseSql = 
      s"""
      from site
      inner join users on site.owner = users.user_id
      """ + (owner.map(o => s"where site.owner = ${o.value}").getOrElse(""))

    val records: Seq[(Site, User)] = SQL(
      "select * " + baseSql +
      s"""
      order by $orderBy limit {pageSize} offset {offset}
      """
    ).on(
      'pageSize -> pageSize,
      'offset -> offset
    ).as(
      withUser *
    )

    val count = SQL(
      "select count(*) " + baseSql
    ).as(SqlParser.scalar[Long].single)

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }
}
