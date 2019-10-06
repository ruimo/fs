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

    "list empty." in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val now = Instant.now()
        repo.list(site.id.get, 0, 10, OrderBy("agent_name")).records.isEmpty === true

        val rec0 = repo.create(site.id.get, "agentName", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        repo.list(site.id.get, 0, 10, OrderBy("agent_name")).records.isEmpty === true

        val rec1 = repo.create(site.id.get, "agentName1", 1, 2L, 3, AgentRecordPhase.END, "tsv", now)
        repo.list(site.id.get, 0, 10, OrderBy("agent_name")).records.isEmpty === true

        val site2 = inject[SiteRepo].create("site1", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val rec2 = repo.create(site2.id.get, "agentName1", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val rec4 = repo.create(site2.id.get, "agentName1", 2, 4L, 6, AgentRecordPhase.END, "tsv", now.plusMillis(100L))

        repo.list(site.id.get, 0, 10, OrderBy("agent_name")).records.isEmpty === true

        val list = repo.list(site2.id.get, 0, 10, OrderBy("agent_name")).records
        list.size === 1
        doWith(list(0)) { r =>
          r.agentName === "agentName1"
          r.startAgentLevel === 1
          r.endAgentLevel === 2
          r.earnedAgentLevel === 1
          r.startLifetimeAp === 2L
          r.endLifetimeAp === 4L
          r.earnedLifetimeAp === 2L
          r.startDistanceWalked === 3
          r.endDistanceWalked === 6
          r.earnedDistanceWalked === 3
          r.createdAt === now.plusMillis(100L)
        }
      }
    }

    "list" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site0 = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val site1 = inject[SiteRepo].create("site1", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val now = Instant.now()

        val rec0 = repo.create(site0.id.get, "agentName0", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val rec1 = repo.create(site0.id.get, "agentName3", 1, 2L, 3, AgentRecordPhase.END, "tsv", now)

        val rec2 = repo.create(site0.id.get, "agentName1", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val rec3 = repo.create(site0.id.get, "agentName1", 11, 32L, 53, AgentRecordPhase.END, "tsv", now.plusMillis(100L))

        val rec5 = repo.create(site0.id.get, "agentName2", 10, 20L, 30, AgentRecordPhase.START, "tsv", now.plusMillis(123L))
        val rec6 = repo.create(site0.id.get, "agentName2", 100, 49L, 90, AgentRecordPhase.END, "tsv", now.plusMillis(50L))

        val rec7 = repo.create(site1.id.get, "agentName2", 110, 120L, 130, AgentRecordPhase.START, "tsv", now.plusMillis(12L))
        val rec8 = repo.create(site1.id.get, "agentName2", 200, 190L, 170, AgentRecordPhase.END, "tsv", now.plusMillis(5L))

        // site0
        // agent      startLv endLv earnedLv startAp endAp earnedAp startDistWlk endDistWlk earnedDistWlk createAt
        // agentName1 1       11    10       2       32    30       3            53         50            now + 100
        // agentName2 10      100   90       20      49    29       30           90         60            now + 50

        // site1
        // agent      startLv endLv earnedLv startAp endAp earnedAp startDistWlk endDistWlk earnedDistWlk createAt
        // agentName2 110     200   90       120     190   70       130          170        40            now + 5

        val e0 = AgentRecordSumEntry(
          "agentName1",
          1, 11, 10,
          2, 32, 30,
          3, 53, 50,
          now.plusMillis(100L)
        )

        val e1 = AgentRecordSumEntry(
          "agentName2",
          10, 100, 90,
          20, 49, 29,
          30, 90, 60,
          now.plusMillis(50L)
        )

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("agent_name")).records) { list =>
          list.size === 2
          list(0) === e0
          list(1) === e1
        }
       
        doWith(repo.list(site0.id.get, 0, 10, OrderBy("agent_name desc")).records) { list =>
          list.size === 2
          list(0) === e1
          list(1) === e0
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("start_agent_level")).records) { list =>
          list.size === 2
          list(0) === e0
          list(1) === e1
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("start_agent_level desc")).records) { list =>
          list.size === 2
          list(0) === e1
          list(1) === e0
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("agent_level_earned")).records) { list =>
          list.size === 2
          list(0) === e0
          list(1) === e1
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("agent_level_earned desc")).records) { list =>
          list.size === 2
          list(0) === e1
          list(1) === e0
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("start_distance_walked")).records) { list =>
          list.size === 2
          list(0) === e0
          list(1) === e1
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("start_distance_walked desc")).records) { list =>
          list.size === 2
          list(0) === e1
          list(1) === e0
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("distance_walked_earned")).records) { list =>
          list.size === 2
          list(0) === e0
          list(1) === e1
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("distance_walked_earned desc")).records) { list =>
          list.size === 2
          list(0) === e1
          list(1) === e0
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("created_at")).records) { list =>
          list.size === 2
          list(0) === e1
          list(1) === e0
        }

        doWith(repo.list(site0.id.get, 0, 10, OrderBy("created_at desc")).records) { list =>
          list.size === 2
          list(0) === e0
          list(1) === e1
        }
      }
    }
  }
}
