package models

import scala.math.{min, max}

case class Pagination(
  topButtonExists: Boolean,
  lastButtonExists: Boolean,
  startPage: Long,
  showPageCount: Long
)

object Pagination {
  def get(pagedRecords: PagedRecords[_], topLastShowThreshold: Int = 5): Option[Pagination] = {
    if (pagedRecords.pageCount == 1) None
    else if (pagedRecords.pageCount <= topLastShowThreshold) {
      Some(
        Pagination(
          topButtonExists = false, lastButtonExists = false,
          startPage = 0, showPageCount = pagedRecords.pageCount
        )
      )
    } else {
      var start: Long = pagedRecords.currentPage - (topLastShowThreshold / 2)
      var end: Long = pagedRecords.currentPage + (topLastShowThreshold / 2)

      if (start < 0) {
        start = 0
        end = topLastShowThreshold - 1
      } else if (pagedRecords.pageCount <= end ) {
        end = pagedRecords.pageCount - 1
        start = end - topLastShowThreshold + 1
      }

      Some(
        Pagination(
          topButtonExists = 0 < start, lastButtonExists = end < pagedRecords.pageCount - 1,
          start, topLastShowThreshold
        )
      )
    }
  }
}
