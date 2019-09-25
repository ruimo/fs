package models

import helpers.TimeZoneInfo
import java.time.{LocalDateTime, ZonedDateTime, Instant}

case class CreateSite(siteName: String, localDateTime: LocalDateTime, timeZoneIndex: Int) {
  val timeZone: TimeZoneInfo = TimeZoneInfo.table(timeZoneIndex)
  val dateTime: ZonedDateTime = localDateTime.atZone(timeZone.zoneId)
  val utc: Instant = dateTime.toInstant
}

