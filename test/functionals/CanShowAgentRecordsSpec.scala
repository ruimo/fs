package functionals

import java.nio.file.{Files, Paths}

import models._
import org.specs2.mutable.Specification
import play.api.test.WithServer
import play.api.test.Helpers._
import com.codeborne.selenide.Selenide._
import com.codeborne.selenide.{Browsers, CollectionCondition, Condition, Configuration, Selenide, WebDriverRunner}
import helpers.{Helper, InjectorSupport, Tsv}
import models.{UserRepo, UserRole}
import org.fluentlenium.core.conditions.Conditions
import play.api.db.Database
import java.time.Instant
import java.time.ZoneId

import org.openqa.selenium.Keys

class CanShowAgentRecordsSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Can show agent record" should {
    "Many records" in new WithServer(app = appl, port = testPort) {
      inject[Database].withConnection { implicit conn =>
        val siteRepo = inject[SiteRepo]
        val userRepo = inject[UserRepo]
        val agentRecordRepo = inject[AgentRecordRepo]

        val user00 = userRepo.create(
          "user0000", "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )

        val site00 = siteRepo.create(
          "site00", heldOnUtc = Instant.ofEpochMilli(3L), heldOnZoneId = ZoneId.systemDefault,
          owner = user00.id.get, now = Instant.ofEpochMilli(2L)
        )

        (0 until 55).foreach { i =>
          agentRecordRepo.create(
            site00.id.get, "Enl", "agent" + i, 10, 100L, 10, AgentRecordPhase.START, "tsv"
          )

          agentRecordRepo.create(
            site00.id.get, "Enl", "agent" + i, 10, 101L, 12, AgentRecordPhase.END, "tsv"
          )
        }

        open("/agentRecords/1")
        $(".pagination-link.top").should(Condition.hidden)
        $$(".pagination-link.middle").shouldHaveSize(5)
        $$(".pagination-link.middle").get(0).text === "1"
        $$(".pagination-link.middle").get(1).text === "2"
        $$(".pagination-link.middle").get(2).text === "3"
        $$(".pagination-link.middle").get(3).text === "4"
        $$(".pagination-link.middle").get(4).text === "5"
        $(".pagination-link.bottom").should(Condition.appear)
        $(".pagination-link.bottom").text === "6"

        $(".pagination-link.bottom").click()
        $(".pagination-link.top").should(Condition.appear)
        $(".pagination-link.top").text === "1"
        $$(".pagination-link.middle").get(0).text === "2"
        $$(".pagination-link.middle").get(1).text === "3"
        $$(".pagination-link.middle").get(2).text === "4"
        $$(".pagination-link.middle").get(3).text === "5"
        $$(".pagination-link.middle").get(4).text === "6"
        $(".pagination-link.bottom").should(Condition.hidden)

        $(".pagination-link.top").click()
        $(".pagination-link.top").should(Condition.hidden)
        $$(".pagination-link.middle").shouldHaveSize(5)
        $$(".pagination-link.middle").get(0).text === "1"
        $$(".pagination-link.middle").get(1).text === "2"
        $$(".pagination-link.middle").get(2).text === "3"
        $$(".pagination-link.middle").get(3).text === "4"
        $$(".pagination-link.middle").get(4).text === "5"
        $(".pagination-link.bottom").should(Condition.appear)
        $(".pagination-link.bottom").text === "6"
      }
    }
  }
}

