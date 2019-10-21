package models

import java.io.{BufferedReader, FileReader}

import anorm._
import org.specs2.mutable._
import helpers.InjectorSupport
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.db.Database
import java.time.{Instant, ZoneId}

import com.ruimo.csv.Parser
import com.ruimo.scoins.{LoanPattern, PathUtil}
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
        val rec = repo.create(site.id.get, "enl", "agentName", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val map = repo.getByAgentNameWithSite(site.id.get, "agentName")
        map.size === 1
        doWith(map(AgentRecordPhase.START)) { case (ar, site) =>
          ar.faction === "enl"
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
        val rec = repo.create(site.id.get, "enl", "agentName", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
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

        val rec0 = repo.create(site.id.get, "enl", "agentName", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        repo.list(site.id.get, 0, 10, OrderBy("agent_name")).records.isEmpty === true

        val rec1 = repo.create(site.id.get, "enl", "agentName1", 1, 2L, 3, AgentRecordPhase.END, "tsv", now)
        repo.list(site.id.get, 0, 10, OrderBy("agent_name")).records.isEmpty === true

        val site2 = inject[SiteRepo].create("site1", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val rec2 = repo.create(site2.id.get, "enl", "agentName1", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val rec4 = repo.create(site2.id.get, "enl", "agentName1", 2, 4L, 6, AgentRecordPhase.END, "tsv", now.plusMillis(100L))

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

        val rec0 = repo.create(site0.id.get, "enl", "agentName0", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val rec1 = repo.create(site0.id.get, "enl", "agentName3", 1, 2L, 3, AgentRecordPhase.END, "tsv", now)

        val rec2 = repo.create(site0.id.get, "enl", "agentName1", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val rec3 = repo.create(site0.id.get, "enl", "agentName1", 11, 32L, 53, AgentRecordPhase.END, "tsv", now.plusMillis(100L))

        val rec5 = repo.create(site0.id.get, "enl", "agentName2", 10, 20L, 30, AgentRecordPhase.START, "tsv", now.plusMillis(123L))
        val rec6 = repo.create(site0.id.get, "enl", "agentName2", 100, 49L, 90, AgentRecordPhase.END, "tsv", now.plusMillis(50L))

        val rec7 = repo.create(site1.id.get, "enl", "agentName2", 110, 120L, 130, AgentRecordPhase.START, "tsv", now.plusMillis(12L))
        val rec8 = repo.create(site1.id.get, "enl", "agentName2", 200, 190L, 170, AgentRecordPhase.END, "tsv", now.plusMillis(5L))

        // site0
        // agent      startLv endLv earnedLv startAp endAp earnedAp startDistWlk endDistWlk earnedDistWlk createAt
        // agentName1 1       11    10       2       32    30       3            53         50            now + 100
        // agentName2 10      100   90       20      49    29       30           90         60            now + 50

        // site1
        // agent      startLv endLv earnedLv startAp endAp earnedAp startDistWlk endDistWlk earnedDistWlk createAt
        // agentName2 110     200   90       120     190   70       130          170        40            now + 5

        val e0 = AgentRecordSumEntry(
          "enl",
          "agentName1",
          1, 11, 10,
          2, 32, 30,
          3, 53, 50,
          now.plusMillis(100L)
        )

        val e1 = AgentRecordSumEntry(
          "enl",
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

    "list orphan" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site0 = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val site1 = inject[SiteRepo].create("site1", Instant.now(), ZoneId.systemDefault(), user.id.get)
        val now = Instant.now()

        val rec0 = repo.create(site0.id.get, "enl", "agentName0", 1, 4L, 3, AgentRecordPhase.START, "tsv", now)
        val rec1 = repo.create(site0.id.get, "enl", "agentName3", 2, 2L, 6, AgentRecordPhase.END, "tsv", now.plusMillis(5L))

        val rec2 = repo.create(site0.id.get, "enl", "agentName1", 1, 2L, 3, AgentRecordPhase.START, "tsv", now)
        val rec3 = repo.create(site0.id.get, "enl", "agentName1", 11, 32L, 53, AgentRecordPhase.END, "tsv", now.plusMillis(100L))

        val rec5 = repo.create(site0.id.get, "enl", "agentName2", 10, 20L, 30, AgentRecordPhase.START, "tsv", now.plusMillis(123L))
        val rec6 = repo.create(site0.id.get, "enl", "agentName2", 100, 49L, 90, AgentRecordPhase.END, "tsv", now.plusMillis(50L))

        val rec7 = repo.create(site1.id.get, "enl", "agentName2", 110, 120L, 130, AgentRecordPhase.START, "tsv", now.plusMillis(12L))
        val rec8 = repo.create(site1.id.get, "enl", "agentName2", 200, 190L, 170, AgentRecordPhase.END, "tsv", now.plusMillis(5L))

        val rec9 = repo.create(site1.id.get, "enl", "agentName0", 120, 220L, 330, AgentRecordPhase.START, "tsv", now.plusMillis(22L))
        val recA = repo.create(site1.id.get, "enl", "agentName5", 210, 290L, 370, AgentRecordPhase.END, "tsv", now.plusMillis(15L))

        // site0
        // agent      lv  ap  distWlk phase createAt
        // agentName0 1    2        3     0      now
        // agentName3 2    4        6     1      now + 5

        // site1
        // agent      lv   ap  distWlk phase createAt
        // agentName0 120 220      330     0 now + 22
        // agentName5 210 290      370     1 now + 15

        doWith(repo.listOrphan(site0.id.get, 0, 10, OrderBy("agent_name")).records) { list =>
          list.size === 2
          list(0) === rec0
          list(1) === rec1
        }
       
        doWith(repo.listOrphan(site0.id.get, 0, 10, OrderBy("agent_name desc")).records) { list =>
          list.size === 2
          list(0) === rec1
          list(1) === rec0
        }

        doWith(repo.listOrphan(site0.id.get, 0, 10, OrderBy("agent_level")).records) { list =>
          list.size === 2
          list(0) === rec0
          list(1) === rec1
        }

        doWith(repo.listOrphan(site0.id.get, 0, 10, OrderBy("agent_level desc")).records) { list =>
          list.size === 2
          list(0) === rec1
          list(1) === rec0
        }

        doWith(repo.listOrphan(site0.id.get, 0, 10, OrderBy("distance_walked")).records) { list =>
          list.size === 2
          list(0) === rec0
          list(1) === rec1
        }

        doWith(repo.listOrphan(site0.id.get, 0, 10, OrderBy("distance_walked desc")).records) { list =>
          list.size === 2
          list(0) === rec1
          list(1) === rec0
        }

        doWith(repo.listOrphan(site1.id.get, 0, 10, OrderBy("agent_name")).records) { list =>
          list.size === 2
          list(0) === rec9
          list(1) === recA
        }
       
        doWith(repo.listOrphan(site1.id.get, 0, 10, OrderBy("agent_name desc")).records) { list =>
          list.size === 2
          list(0) === recA
          list(1) === rec9
        }

        doWith(repo.listOrphan(site1.id.get, 0, 10, OrderBy("agent_level")).records) { list =>
          list.size === 2
          list(0) === rec9
          list(1) === recA
        }

        doWith(repo.listOrphan(site1.id.get, 0, 10, OrderBy("agent_level desc")).records) { list =>
          list.size === 2
          list(0) === recA
          list(1) === rec9
        }

        doWith(repo.listOrphan(site1.id.get, 0, 10, OrderBy("distance_walked")).records) { list =>
          list.size === 2
          list(0) === rec9
          list(1) === recA
        }

        doWith(repo.listOrphan(site1.id.get, 0, 10, OrderBy("distance_walked desc")).records) { list =>
          list.size === 2
          list(0) === recA
          list(1) === rec9
        }
      }
    }

    "Can download tsv" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)

        val recStart0 = repo.create(
          site.id.get, "Enlightened", "agent00", 8, 123L, 345, AgentRecordPhase.START, "tsv00"
        )
        val recEnd0 = repo.create(
          site.id.get, "Enlightened", "agent00", 9, 234L, 400, AgentRecordPhase.END, "tsv01"
        )

        val recStart1 = repo.create(
          site.id.get, "Enlightened", "agent01", 10, 123L, 245, AgentRecordPhase.START, "tsv02"
        )
        val recEnd1 = repo.create(
          site.id.get, "Enlightened", "agent01", 11, 1000L, 1400, AgentRecordPhase.END, "tsv03"
        )

        val recStart2 = repo.create(
          site.id.get, "Enlightened", "agent02", 10, 1123L, 1245, AgentRecordPhase.START, "tsv04"
        )
        val recEnd2 = repo.create(
          site.id.get, "Enlightened", "agent02", 12, 2000L, 2400, AgentRecordPhase.END, "tsv05"
        )

        import com.ruimo.scoins.LoanPattern.autoCloseableCloser
        PathUtil.withTempFile(None, None) { path =>
          repo.downloadTsv(site.id.get, OrderBy("agent_name asc"), path)
          LoanPattern.using(new BufferedReader(new FileReader(path.toFile))) { br =>
            val headers = Parser.parseOneLine(br.readLine, '\t').get
            headers === Seq(
              "Agent Faction", "Agent Name",
              "Start Level", "End Level", "Earned Level",
              "Start Lifetime AP", "End Lifetime AP", "Earned Lifetime AP",
              "Start Distance Walked", "End Distance Walked", "Earned Distance Walked"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent00",
              "8", "9", "1",
              "123", "234", (234 - 123).toString,
              "345", "400", (400 - 345).toString
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent01",
              "10", "11", "1",
              "123", "1000", (1000 - 123).toString,
              "245", "1400", (1400 - 245).toString
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent02",
              "10", "12", "2",
              "1124", "2000", (2000 - 1123).toString,
              "1245", "2401", (2400 - 1245).toString
            )

            br.readLine === null
          }
        }.get

        PathUtil.withTempFile(None, None) { path =>
          repo.downloadTsv(site.id.get, OrderBy("lifetime_ap_earned desc"), path)
          LoanPattern.using(new BufferedReader(new FileReader(path.toFile))) { br =>
            val headers = Parser.parseOneLine(br.readLine, '\t').get
            headers === Seq(
              "Agent Faction", "Agent Name",
              "Start Level", "End Level", "Earned Level",
              "Start Lifetime AP", "End Lifetime AP", "Earned Lifetime AP",
              "Start Distance Walked", "End Distance Walked", "Earned Distance Walked"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent01",
              "10", "11", "1",
              "123", "1000", (1000 - 123).toString, // 877
              "245", "1400", (1400 - 245).toString // 1155
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent02",
              "10", "12", "2",
              "1124", "2000", (2000 - 1124).toString, // 876
              "1245", "2401", (2401 - 1245).toString // 1156
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent00",
              "8", "9", "1",
              "123", "234", (234 - 123).toString, // 111
              "345", "400", (400 - 345).toString // 55
            )

            br.readLine === null
          }
        }.get

        PathUtil.withTempFile(None, None) { path =>
          repo.downloadTsv(site.id.get, OrderBy("distance_walked_earned asc"), path)
          LoanPattern.using(new BufferedReader(new FileReader(path.toFile))) { br =>
            val headers = Parser.parseOneLine(br.readLine, '\t').get
            headers === Seq(
              "Agent Faction", "Agent Name",
              "Start Level", "End Level", "Earned Level",
              "Start Lifetime AP", "End Lifetime AP", "Earned Lifetime AP",
              "Start Distance Walked", "End Distance Walked", "Earned Distance Walked"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent00",
              "8", "9", "1",
              "123", "234", (234 - 123).toString, // 111
              "345", "400", (400 - 345).toString // 55
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent01",
              "10", "11", "1",
              "123", "1000", (1000 - 123).toString, // 877
              "245", "1400", (1400 - 245).toString // 1155
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent02",
              "10", "12", "2",
              "1124", "2000", (2000 - 1124).toString, // 876
              "1245", "2401", (2401 - 1245).toString // 1156
            )

            br.readLine === null
          }
        }.get

        1 === 1
      }
    }

    "Can download orphan tsv" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      inject[Database].withConnection { implicit conn =>
        val repo = inject[AgentRecordRepo]
        val user = inject[UserRepo].create("user", "email", 0, 0, UserRole.ADMIN)
        val site = inject[SiteRepo].create("site0", Instant.now(), ZoneId.systemDefault(), user.id.get)

        val recStart0 = repo.create(
          site.id.get, "Enlightened", "agent00", 8, 123L, 345, AgentRecordPhase.START, "tsv00"
        )
        val recEnd0 = repo.create(
          site.id.get, "Enlightened", "agent00", 9, 234L, 400, AgentRecordPhase.END, "tsv01"
        )

        val recStart1 = repo.create(
          site.id.get, "Enlightened", "agent01", 10, 123L, 245, AgentRecordPhase.START, "tsv02"
        )
        val recEnd1 = repo.create(
          site.id.get, "Enlightened", "agent01", 11, 1000L, 1400, AgentRecordPhase.END, "tsv03"
        )

        val recStart2 = repo.create(
          site.id.get, "Enlightened", "agent02", 10, 1123L, 1245, AgentRecordPhase.START, "tsv04"
        )
        val recEnd2 = repo.create(
          site.id.get, "Enlightened", "agent02", 12, 2000L, 2400, AgentRecordPhase.END, "tsv05"
        )

        val recStart3 = repo.create(
          site.id.get, "Enlightened", "agent03", 10, 1123L, 1245, AgentRecordPhase.START, "tsv04"
        )
        val recEnd4 = repo.create(
          site.id.get, "Enlightened", "agent04", 12, 2000L, 2400, AgentRecordPhase.END, "tsv05"
        )
        val recStart5 = repo.create(
          site.id.get, "Enlightened", "agent05", 11, 2123L, 2245, AgentRecordPhase.START, "tsv04"
        )

        import com.ruimo.scoins.LoanPattern.autoCloseableCloser
        PathUtil.withTempFile(None, None) { path =>
          repo.downloadTsv(site.id.get, OrderBy("agent_name asc"), path)
          LoanPattern.using(new BufferedReader(new FileReader(path.toFile))) { br =>
            val headers = Parser.parseOneLine(br.readLine, '\t').get
            headers === Seq(
              "Agent Faction", "Agent Name", "Level", "Lifetime AP", "Distance Walked"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent03", "10", "1123", "1245"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent04", "12", "2000", "2400"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent05", "11", "2123", "2245"
            )

            br.readLine === null
          }
        }.get

        PathUtil.withTempFile(None, None) { path =>
          repo.downloadTsv(site.id.get, OrderBy("lifetime_ap_earned desc"), path)
          LoanPattern.using(new BufferedReader(new FileReader(path.toFile))) { br =>
            val headers = Parser.parseOneLine(br.readLine, '\t').get
            headers === Seq(
              "Agent Faction", "Agent Name", "Level", "Lifetime AP", "Distance Walked"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent05", "11", "2123", "2245"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent04", "12", "2000", "2400"
            )

            Parser.parseOneLine(br.readLine, '\t').get === Seq(
              "Enlightened", "agent03", "10", "1123", "1245"
            )

            br.readLine === null
          }
        }.get

        1 === 1
      }
    }
  }
}
