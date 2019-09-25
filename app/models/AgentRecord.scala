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
  site_id: SiteId,
  agentName: String,
  agentLevel: Int,
  lifetimeAp: Long,
  distanceWalked: Int,
  phase: AgentRecordPhase,
  tsv: String,
  createdAt: Instant
)

@Singleton
class AgentRecordRepo @Inject() (
  siteRepo: SiteRepo
) {
  val simple = {
    SqlParser.get[Option[Long]]("agent_record.agent_record_id") ~
    SqlParser.get[Long]("agent_record.site_id") ~
    SqlParser.get[String]("agent_record.agent_name") ~
    SqlParser.get[Int]("agent_record.agent_level") ~
    SqlParser.get[Long]("agent_record.lifetime_ap") ~
    SqlParser.get[Int]("agent_record.distance_walked") ~
    SqlParser.get[Int]("agent_record.phase") ~
    SqlParser.get[String]("agent_record.tsv") ~
    SqlParser.get[Instant]("agent_record.created_at") map {
      case id~siteId~agentName~agentLevel~lifetimeAp~distanceWalked~phase~tsv~createdAt =>
        AgentRecord(
          id.map(AgentRecordId.apply), SiteId(siteId), agentName, agentLevel, lifetimeAp,
          distanceWalked, AgentRecordPhase.byIndex(phase), tsv, createdAt
        )
    }
  }

  val withSite = simple ~ siteRepo.simple map {
    case agentRecord ~ site => (agentRecord, site)
  }

  def create(
    siteId: SiteId, agentName: String, agentLevel: Int, lifetimeAp: Long, distanceWalked: Int,
    phase: AgentRecordPhase, tsv: String, createdAt: Instant = Instant.now()
  )(implicit conn: Connection): AgentRecord = ExceptionMapper.mapException {
    SQL(
      """
      insert into agent_record (
        agent_record_id, site_id, agent_name, agent_level, lifetime_ap, distance_walked, phase, tsv, created_at
      ) values (
        (select nextval('agent_record_seq')),
        {siteId}, {agentName}, {agentLevel}, {lifetimeAp}, {distanceWalked}, {phase}, {tsv}, {createdAt}
      )
      """
    ).on(
      'siteId -> siteId.value,
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
      Some(AgentRecordId(id)), siteId, agentName, agentLevel, lifetimeAp, distanceWalked, phase, tsv, createdAt
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
}
