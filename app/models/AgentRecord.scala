package models

import javax.inject.{Inject, Singleton}
import java.sql.Connection

import scala.collection.{immutable => imm}
import java.time.{Instant, ZoneId}

import anorm._

import scala.language.postfixOps

case class AgentRecordId(value: Long) extends AnyVal

case class AgentRecord(
  id: Option[AgentRecordId],
  siteId: SiteId,
  faction: String,
  agentName: String,
  agentLevel: Int,
  lifetimeAp: Long,
  distanceWalked: Int,
  phase: AgentRecordPhase,
  tsv: String,
  createdAt: Instant
)

case class AgentRecordSumEntry(
  faction: String,
  agentName: String,
  startAgentLevel: Int,
  endAgentLevel: Int,
  earnedAgentLevel: Int,
  startLifetimeAp: Long,
  endLifetimeAp: Long,
  earnedLifetimeAp: Long,
  startDistanceWalked: Int,
  endDistanceWalked: Int,
  earnedDistanceWalked: Int,
  createdAt: Instant
)

@Singleton
class AgentRecordRepo @Inject() (
  siteRepo: SiteRepo
) {
  val simple = {
    SqlParser.get[Option[Long]]("agent_record.agent_record_id") ~
    SqlParser.get[Long]("agent_record.site_id") ~
    SqlParser.get[String]("agent_record.faction") ~
    SqlParser.get[String]("agent_record.agent_name") ~
    SqlParser.get[Int]("agent_record.agent_level") ~
    SqlParser.get[Long]("agent_record.lifetime_ap") ~
    SqlParser.get[Int]("agent_record.distance_walked") ~
    SqlParser.get[Int]("agent_record.phase") ~
    SqlParser.get[String]("agent_record.tsv") ~
    SqlParser.get[Instant]("agent_record.created_at") map {
      case id~siteId~faction~agentName~agentLevel~lifetimeAp~distanceWalked~phase~tsv~createdAt =>
        AgentRecord(
          id.map(AgentRecordId.apply), SiteId(siteId), faction, agentName, agentLevel, lifetimeAp,
          distanceWalked, AgentRecordPhase.byIndex(phase), tsv, createdAt
        )
    }
  }

  val withSite = simple ~ siteRepo.simple map {
    case agentRecord ~ site => (agentRecord, site)
  }

  def create(
    siteId: SiteId, faction: String, agentName: String, agentLevel: Int, lifetimeAp: Long, distanceWalked: Int,
    phase: AgentRecordPhase, tsv: String, createdAt: Instant = Instant.now()
  )(implicit conn: Connection): AgentRecord = ExceptionMapper.mapException {
    SQL(
      """
      insert into agent_record (
        agent_record_id, site_id, faction, agent_name, agent_level, lifetime_ap, distance_walked, phase, tsv, created_at
      ) values (
        (select nextval('agent_record_seq')),
        {siteId}, {faction}, {agentName}, {agentLevel}, {lifetimeAp}, {distanceWalked}, {phase}, {tsv}, {createdAt}
      )
      """
    ).on(
      'siteId -> siteId.value,
      'faction -> faction,
      'agentName -> agentName,
      'agentLevel -> agentLevel,
      'lifetimeAp -> lifetimeAp,
      'distanceWalked -> distanceWalked,
      'phase -> phase.ordinal(),
      'tsv -> tsv,
      'createdAt -> createdAt
    ).executeUpdate()

    val id: Long = SQL("select currval('agent_record_seq')").as(SqlParser.scalar[Long].single)

    AgentRecord(
      Some(AgentRecordId(id)), siteId, faction, agentName, agentLevel, lifetimeAp, distanceWalked, phase, tsv, createdAt
    )
  }

  def get(agentRecordId: AgentRecordId)(implicit conn: Connection): Option[AgentRecord] = SQL(
    "select * from agent_record where agent_record_id = {agentRecordId}"
  ).on(
    'agentRecordId -> agentRecordId.value
  ).as(simple.singleOpt)

  def getByAgentName(
    siteId: SiteId, agentName: String
  )(
    implicit conn: Connection
  ): imm.Map[AgentRecordPhase, AgentRecord] = SQL(
    "select * from agent_record where site_id = {siteId} and agent_name = {agentName}"
  ).on(
    'siteId -> siteId.value,
    'agentName -> agentName
  ).as(
    simple *
  ).map { rec =>
    rec.phase -> rec
  }.toMap

  def getByAgentNameWithSite(
    siteId: SiteId, agentName: String
  )(
    implicit conn: Connection
  ): imm.Map[AgentRecordPhase, (AgentRecord, Site)] = SQL(
    """
    select * from agent_record
    inner join site on site.site_id = agent_record.site_id
    where agent_record.site_id = {siteId} and agent_name = {agentName}
    """
  ).on(
    'siteId -> siteId.value,
    'agentName -> agentName
  ).as(
    withSite *
  ).map { rec =>
    rec._1.phase -> rec
  }.toMap

  def delete(agentRecordId: AgentRecordId)(implicit conn: Connection): Long = SQL(
    "delete from agent_record where agent_record_id = {agentRecordId}"
  ).on(
    'agentRecordId -> agentRecordId.value
  ).executeUpdate()

  def delete(siteId: SiteId, agentName: String, phase: AgentRecordPhase)(implicit conn: Connection): Long = SQL(
    "delete from agent_record where site_id = {siteId} and agent_name = {agentName} and phase = {phase}"
  ).on(
    'siteId -> siteId.value,
    'agentName -> agentName,
    'phase -> phase.ordinal
  ).executeUpdate()

  val listParser = {
    SqlParser.get[String]("faction") ~
    SqlParser.get[String]("agent_name") ~
    SqlParser.get[Int]("start_agent_level") ~
    SqlParser.get[Int]("end_agent_level") ~
    SqlParser.get[Int]("agent_level_earned") ~
    SqlParser.get[Long]("start_lifetime_ap") ~
    SqlParser.get[Long]("end_lifetime_ap") ~
    SqlParser.get[Long]("lifetime_ap_earned") ~
    SqlParser.get[Int]("start_distance_walked") ~
    SqlParser.get[Int]("end_distance_walked") ~
    SqlParser.get[Int]("distance_walked_earned") ~
    SqlParser.get[Instant]("created_at") map {
      case faction~agentName~agentLevel0~agentLevel1~agentLevelEarned~lifetimeAp0~lifetimeAp1~lifetimeApEarned~distanceWalked0~distanceWalked1~distanceWalkedEarned~createdAt =>
        AgentRecordSumEntry(
          faction, agentName, agentLevel0, agentLevel1, agentLevelEarned, lifetimeAp0, lifetimeAp1, lifetimeApEarned,
          distanceWalked0, distanceWalked1, distanceWalkedEarned, createdAt
        )
    }
  }

  def list(siteId: SiteId, page: Int = 0, pageSize: Int = 10, orderBy: OrderBy)(
    implicit conn: Connection
  ): PagedRecords[AgentRecordSumEntry] = {
    import scala.language.postfixOps

    val offset: Int = pageSize * page
    
    val baseSql = s"""
      from agent_record r0, agent_record r1
      where r0.site_id = {siteId} and r1.site_id = {siteId} and r0.agent_name = r1.agent_name and r0.phase = 0 and r1.phase = 1
    """

    val records: Seq[AgentRecordSumEntry] = SQL(
      """
      select
        r0.faction faction,
        r0.agent_name agent_name,
        r0.agent_level start_agent_level,
        r1.agent_level end_agent_level,
        r1.agent_level - r0.agent_level agent_level_earned,
        r0.lifetime_ap start_lifetime_ap,
        r1.lifetime_ap end_lifetime_ap,
        r1.lifetime_ap - r0.lifetime_ap lifetime_ap_earned,
        r0.distance_walked start_distance_walked,
        r1.distance_walked end_distance_walked,
        r1.distance_walked - r0.distance_walked distance_walked_earned,
        r1.created_at created_at
      """ + baseSql + s"""
      order by $orderBy limit {pageSize} offset {offset}
      """
    ).on(
      'siteId -> siteId.value,
      'pageSize -> pageSize,
      'offset -> offset
    ).as(
      listParser *
    )

    val count = SQL(
      "select count(r0.agent_name) " + baseSql
    ).on(
      'siteId -> siteId.value
    ).as(SqlParser.scalar[Long].single)

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }
}
