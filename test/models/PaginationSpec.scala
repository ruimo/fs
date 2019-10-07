package models

import org.specs2.mutable._
import com.ruimo.scoins.Scoping._

class PaginationSpec extends Specification {
  "Pagination" should {
    "If page count is one, no pagination is needed." in {
      val pr = PagedRecords[Int](
        currentPage = 0,
        pageSize = 10,
        pageCount = 1,
        orderBy = OrderBy("col"),
        records = Seq()
      )

      Pagination.get(pr) === None
    }

    "Threshold - 1 pages." in {
      val threshold = 5
      val pr = PagedRecords[Int](
        currentPage = 0,
        pageSize = 10,
        pageCount = threshold - 1,
        orderBy = OrderBy("col"),
        records = Seq()
      )

      (0 until threshold).foreach { i =>
        Pagination.get(pr.copy(currentPage = i)) === Some(Pagination(false, false, 0, threshold - 1))
      }
      1 === 1
    }

    "Threshold pages." in {
      val threshold = 5
      val pr = PagedRecords[Int](
        currentPage = 0,
        pageSize = 10,
        pageCount = threshold,
        orderBy = OrderBy("col"),
        records = Seq()
      )

      (0 until threshold).foreach { i =>
        Pagination.get(pr.copy(currentPage = i)) === Some(Pagination(false, false, 0, threshold))
      }
      1 === 1
    }

    "Threshold + 1 pages." in {
      val threshold = 5
      val pr = PagedRecords[Int](
        currentPage = 0,
        pageSize = 10,
        pageCount = threshold + 1,
        orderBy = OrderBy("col"),
        records = Seq()
      )

      // [0] 1 2 3 4 ... 5
      Pagination.get(pr) === Some(Pagination(false, true, 0, threshold))

      // 0 [1] 2 3 4 ... 5
      Pagination.get(pr.copy(currentPage = 1)) === Some(Pagination(false, true, 0, threshold))

      // 0 1 [2] 3 4 ... 5
      Pagination.get(pr.copy(currentPage = 2)) === Some(Pagination(false, true, 0, threshold))

      // 0 .. 1 2 [3] 4 5
      Pagination.get(pr.copy(currentPage = 3)) === Some(Pagination(true, false, 1, threshold))

      // 0 .. 1 2 3 [4] 5
      Pagination.get(pr.copy(currentPage = 4)) === Some(Pagination(true, false, 1, threshold))

      // 0 .. 1 2 3 4 [5]
      Pagination.get(pr.copy(currentPage = 5)) === Some(Pagination(true, false, 1, threshold))
    }

    "Threshold + 2 pages." in {
      val threshold = 5
      val pr = PagedRecords[Int](
        currentPage = 0,
        pageSize = 10,
        pageCount = threshold + 2,
        orderBy = OrderBy("col"),
        records = Seq()
      )

      // [0] 1 2 3 4 ... 6
      Pagination.get(pr) === Some(Pagination(false, true, 0, threshold))

      // 0 [1] 2 3 4 ... 6
      Pagination.get(pr.copy(currentPage = 1)) === Some(Pagination(false, true, 0, threshold))

      // 0 1 [2] 3 4 ... 6
      Pagination.get(pr.copy(currentPage = 2)) === Some(Pagination(false, true, 0, threshold))

      // 0 .. 1 2 [3] 4 5 ... 6
      Pagination.get(pr.copy(currentPage = 3)) === Some(Pagination(true, true, 1, threshold))

      // 0 .. 2 3 [4] 5 6
      Pagination.get(pr.copy(currentPage = 4)) === Some(Pagination(true, false, 2, threshold))

      // 0 .. 2 3 4 [5] 6
      Pagination.get(pr.copy(currentPage = 5)) === Some(Pagination(true, false, 2, threshold))

      // 0 .. 2 3 4 5 [6]
      Pagination.get(pr.copy(currentPage = 6)) === Some(Pagination(true, false, 2, threshold))
    }

    "Threshold + 3 pages." in {
      val threshold = 5
      val pr = PagedRecords[Int](
        currentPage = 0,
        pageSize = 10,
        pageCount = threshold + 3,
        orderBy = OrderBy("col"),
        records = Seq()
      )

      // [0] 1 2 3 4 ... 7
      Pagination.get(pr) === Some(Pagination(false, true, 0, threshold))

      // 0 [1] 2 3 4 ... 7
      Pagination.get(pr.copy(currentPage = 1)) === Some(Pagination(false, true, 0, threshold))

      // 0 1 [2] 3 4 ... 7
      Pagination.get(pr.copy(currentPage = 2)) === Some(Pagination(false, true, 0, threshold))

      // 0 .. 1 2 [3] 4 5 ... 7
      Pagination.get(pr.copy(currentPage = 3)) === Some(Pagination(true, true, 1, threshold))

      // 0 .. 2 3 [4] 5 6 ... 7
      Pagination.get(pr.copy(currentPage = 4)) === Some(Pagination(true, true, 2, threshold))

      // 0 .. 3 4 [5] 6 7
      Pagination.get(pr.copy(currentPage = 5)) === Some(Pagination(true, false, 3, threshold))

      // 0 .. 3 4 5 [6] 7
      Pagination.get(pr.copy(currentPage = 6)) === Some(Pagination(true, false, 3, threshold))

      // 0 .. 3 4 5 6 [7]
      Pagination.get(pr.copy(currentPage = 6)) === Some(Pagination(true, false, 3, threshold))
    }
  }
}
