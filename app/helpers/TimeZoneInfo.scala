package helpers

import scala.collection.JavaConverters._
import java.time.{ZoneId, ZoneOffset, Instant}
import scala.collection.immutable.Vector

case class TimeZoneInfo(
  zoneId: ZoneId, zoneOffset: ZoneOffset, view: String
)

object TimeZoneInfo {
  def apply(sZoneId: String): TimeZoneInfo = {
    val zoneId = ZoneId.of(sZoneId)
    val zoneOffset = zoneId.getRules.getOffset(Instant.EPOCH)
    TimeZoneInfo(zoneId, zoneOffset, zoneId + "(" + zoneOffset + ")")
  }

  implicit val orderingByZoneOffset: Ordering[TimeZoneInfo] = new Ordering[TimeZoneInfo] {
    override def compare(x: TimeZoneInfo, y: TimeZoneInfo): Int = x.zoneOffset.compareTo(y.zoneOffset)
  }

  val table: Vector[TimeZoneInfo] =
    ZoneId.getAvailableZoneIds().asScala.toVector.map(TimeZoneInfo.apply).sortBy(_.zoneOffset)

  val tableByZoneId: Map[ZoneId, TimeZoneInfo] = table.map { info => info.zoneId -> info }.toMap
}
