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

class AgentRecordSpec extends Specification with InjectorSupport {
  "Agent record" should {
    "can query empty list." in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)
        repo.getByAgentName(site.id.get, "shanai") === Map()
      }
    }

    "create record." in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)

        val now = Instant.now()
        val rec = repo.create(site.id.get, "agentName", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val map = repo.getByAgentNameWithSite(site.id.get, "agentName")
        map.size === 1
        doWith(map(AgentRecordPhase.START)) { case (ar, site) =>
          ar.agentName === "agentName"
          ar.agentLevel === 1
          ar.lifetimeAp === 2
          ar.distanceWalked === 3
          ar.phase === AgentRecordPhase.START
          ar.tsv === "tsv"
          ar.createdAt === now

          site.siteName === "site0"
        }
      }
    }

    "can delete." in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val site2 = inject[SiteRepo].create("site1", Instant.now(), ZoneId.systemDefault(), user.id.get)

        val now = Instant.now()
        val rec = repo.create(site.id.get, "agentName", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        repo.delete(site.id.get, "agentName", AgentRecordPhase.END)
        repo.getByAgentNameWithSite(site.id.get, "agentName").size === 1
        repo.getByAgentNameWithSite(site2.id.get, "agentName").size === 0
        repo.delete(site.id.get, "agentName", AgentRecordPhase.START)
        repo.getByAgentNameWithSite(site.id.get, "agentName").size === 0
      }
    }
  }
}
