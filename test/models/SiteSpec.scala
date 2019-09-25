package models

import anorm._
import org.specs2.mutable._
import helpers.InjectorSupport
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.db.Database
import java.time.{Instant, ZoneId}
import com.ruimo.scoins.Scoping._

class SiteSpec extends Specification with InjectorSupport {
  "Site" should {
    "can query empty list." in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        inject[SiteRepo].listWithOwner(0, 10, OrderBy("site.site_name"), None).isEmpty === true
      }
    }

    "can query list." in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val userRepo = inject[UserRepo]
        val user01 = userRepo.create("user01", "e@mail", 1L, 2L, UserRole.ADMIN)
        val user02 = userRepo.create("user02", "e@mail", 1L, 2L, UserRole.ADMIN)

        val siteRepo = inject[SiteRepo]
        siteRepo.create("site01", Instant.ofEpochMilli(0), ZoneId.of("UTC"), user01.id.get)
        siteRepo.create("site02", Instant.ofEpochMilli(1), ZoneId.of("UTC"), user01.id.get)
        siteRepo.create("site03", Instant.ofEpochMilli(2), ZoneId.of("UTC"), user01.id.get)
        siteRepo.create("site04", Instant.ofEpochMilli(3), ZoneId.of("UTC"), user01.id.get)
        siteRepo.create("site05", Instant.ofEpochMilli(4), ZoneId.of("UTC"), user01.id.get)
        siteRepo.create("site06", Instant.ofEpochMilli(5), ZoneId.of("UTC"), user02.id.get)

        doWith(siteRepo.listWithOwner(0, 10, OrderBy("site.site_name asc"), user01.id)) { recs =>
          recs.records.size === 5
          recs.records(0)._1.siteName === "site01"
          recs.records(0)._2.id === user01.id

          recs.records(1)._1.siteName === "site02"
          recs.records(1)._2.id === user01.id

          recs.records(2)._1.siteName === "site03"
          recs.records(2)._2.id === user01.id

          recs.records(3)._1.siteName === "site04"
          recs.records(3)._2.id === user01.id

          recs.records(4)._1.siteName === "site05"
          recs.records(4)._2.id === user01.id
        }

        doWith(siteRepo.listWithOwner(0, 10, OrderBy("site.held_on_utc desc"), user01.id)) { recs =>
          recs.records.size === 5
          recs.records(0)._1.siteName === "site05"
          recs.records(0)._2.id === user01.id

          recs.records(1)._1.siteName === "site04"
          recs.records(1)._2.id === user01.id

          recs.records(2)._1.siteName === "site03"
          recs.records(2)._2.id === user01.id

          recs.records(3)._1.siteName === "site02"
          recs.records(3)._2.id === user01.id

          recs.records(4)._1.siteName === "site01"
          recs.records(4)._2.id === user01.id
        }

        doWith(siteRepo.listWithOwner(0, 5, OrderBy("site.site_name asc"), None)) { recs =>
          recs.records.size === 5
          recs.records(0)._1.siteName === "site01"
          recs.records(0)._2.id === user01.id

          recs.records(1)._1.siteName === "site02"
          recs.records(1)._2.id === user01.id

          recs.records(2)._1.siteName === "site03"
          recs.records(2)._2.id === user01.id

          recs.records(3)._1.siteName === "site04"
          recs.records(3)._2.id === user01.id

          recs.records(4)._1.siteName === "site05"
          recs.records(4)._2.id === user01.id
        }

        doWith(siteRepo.listWithOwner(1, 5, OrderBy("site.site_name asc"), None)) { recs =>
          recs.records.size === 1
          recs.records(0)._1.siteName === "site06"
          recs.records(0)._2.id === user02.id
        }
      }
    }
  }
}

