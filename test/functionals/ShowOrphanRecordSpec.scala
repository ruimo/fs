package functionals

import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.{BufferedReader, StringReader}
import java.nio.file.{Files, Path, Paths}

import play.api.i18n.MessagesApi
import models._
import org.specs2.mutable.Specification
import play.api.test.WithServer
import play.api.test.Helpers._
import com.codeborne.selenide.Selenide._
import com.codeborne.selenide.{Browsers, CollectionCondition, Condition, Configuration, Selenide, WebDriverRunner}
import helpers.{Helper, InjectorSupport, TimeZoneInfo, Tsv}
import models.{UserRepo, UserRole}
import org.fluentlenium.core.conditions.Conditions
import play.api.db.Database
import java.time.Instant
import java.time.ZoneId

import controllers.AttendController
import org.openqa.selenium.{By, Cookie, Keys, WebElement}
import play.api.db.Database
import play.api.i18n.Lang

import scala.annotation.tailrec
import scala.collection.JavaConverters._

class ShowOrphanRecordSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Show result." should {
    "One attendee is registered." in new WithServer(app = appl, port = testPort) {
      inject[Database].withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val msg = inject[MessagesApi]
        val siteRepo = inject[SiteRepo]
        val userRepo = inject[UserRepo]
        val agRepo = inject[AgentRecordRepo]

        val user01 = userRepo.create(
          Helper.TestUserName, "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )
        val user02 = userRepo.create(
          "user02", "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )
        val user03 = userRepo.create(
          "user03", "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.SUPER
        )

        val now = Instant.ofEpochMilli(1000L * 60)
        val heldOnUtc = Instant.ofEpochMilli(2000L * 60)
        val zoneId = ZoneId.of("Asia/Tokyo")
        val site01 = siteRepo.create(
          "site01", heldOnUtc, heldOnZoneId = zoneId,
          owner = user01.id.get, now
        )
        val site02 = siteRepo.create(
          "site02", heldOnUtc, heldOnZoneId = zoneId,
          owner = user02.id.get, now
        )

        val recStart0 = agRepo.create(
          site01.id.get, "Enlightened", "agent00", 8, 123L, 343, AgentRecordPhase.START, "tsv00"
        )
        val recEnd0 = agRepo.create(
          site01.id.get, "Enlightened", "agent00", 9, 235L, 400, AgentRecordPhase.END, "tsv01"
        )

        val recStart1 = agRepo.create(
          site01.id.get, "Enlightened", "agent01", 9, 223L, 445, AgentRecordPhase.START, "tsv00"
        )
        val recEnd1 = agRepo.create(
          site01.id.get, "Enlightened", "agent01", 9, 334L, 500, AgentRecordPhase.END, "tsv01"
        )

        val recStart2 = agRepo.create(
          site01.id.get, "Enlightened", "agent02", 1, 423L, 545, AgentRecordPhase.START, "tsv00"
        )

        val recEnd2 = agRepo.create(
          site01.id.get, "Enlightened", "agent03", 10, 134L, 100, AgentRecordPhase.END, "tsv01"
        )

        val recEnd3 = agRepo.create(
          site01.id.get, "Enlightened", "agent04", 11, 1134L, 2100, AgentRecordPhase.START, "tsv01"
        )

        val recStart4 = agRepo.create(
          site02.id.get, "Enlightened", "agent01", 10, 1223L, 1445, AgentRecordPhase.START, "tsv00"
        )
        val recEnd4 = agRepo.create(
          site02.id.get, "Enlightened", "agent01", 11, 1334L, 1500, AgentRecordPhase.END, "tsv01"
        )

        open("/agentRecords/" + site01.id.get.value)

        $$("table.score tbody tr").shouldHaveSize(2)
        $("table.score th.earnedAp.ordered").should(Condition.visible)
        $("table.score th.earnedAp .desc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".rank").text === "1" 
        $$("table.score tbody tr").get(0).find(".agentName").text === "agent00" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".startLevel").text === "8" 
        $$("table.score tbody tr").get(0).find(".endLevel").text === "9" 
        $$("table.score tbody tr").get(0).find(".earnedLevel").text === "1" 
        $$("table.score tbody tr").get(0).find(".startAp").text === "123" 
        $$("table.score tbody tr").get(0).find(".endAp").text === "235" 
        $$("table.score tbody tr").get(0).find(".earnedAp").text === "112" 
        $$("table.score tbody tr").get(0).find(".startWalked").text === "343" 
        $$("table.score tbody tr").get(0).find(".endWalked").text === "400" 
        $$("table.score tbody tr").get(0).find(".earnedWalked").text === "57" 

        $$("table.score tbody tr").get(1).find(".rank").text === "2" 
        $$("table.score tbody tr").get(1).find(".agentName").text === "agent01" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".startLevel").text === "9"
        $$("table.score tbody tr").get(1).find(".endLevel").text === "9"
        $$("table.score tbody tr").get(1).find(".earnedLevel").text === "0"
        $$("table.score tbody tr").get(1).find(".startAp").text === "223"
        $$("table.score tbody tr").get(1).find(".endAp").text === "334"
        $$("table.score tbody tr").get(1).find(".earnedAp").text === "111"
        $$("table.score tbody tr").get(1).find(".startWalked").text === "445" 
        $$("table.score tbody tr").get(1).find(".endWalked").text === "500" 
        $$("table.score tbody tr").get(1).find(".earnedWalked").text === "55" 

        $("table.score th.earnedAp.ordered").click()
        $("table.score th.earnedAp.ordered").should(Condition.visible)
        $("table.score th.earnedAp .asc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".rank").text === "1" 
        $$("table.score tbody tr").get(0).find(".agentName").text === "agent01" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".startLevel").text === "9"
        $$("table.score tbody tr").get(0).find(".endLevel").text === "9"
        $$("table.score tbody tr").get(0).find(".earnedLevel").text === "0"
        $$("table.score tbody tr").get(0).find(".startAp").text === "223"
        $$("table.score tbody tr").get(0).find(".endAp").text === "334"
        $$("table.score tbody tr").get(0).find(".earnedAp").text === "111"
        $$("table.score tbody tr").get(0).find(".startWalked").text === "445" 
        $$("table.score tbody tr").get(0).find(".endWalked").text === "500" 
        $$("table.score tbody tr").get(0).find(".earnedWalked").text === "55" 

        $$("table.score tbody tr").get(1).find(".rank").text === "2" 
        $$("table.score tbody tr").get(1).find(".agentName").text === "agent00" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".startLevel").text === "8" 
        $$("table.score tbody tr").get(1).find(".endLevel").text === "9" 
        $$("table.score tbody tr").get(1).find(".earnedLevel").text === "1" 
        $$("table.score tbody tr").get(1).find(".startAp").text === "123" 
        $$("table.score tbody tr").get(1).find(".endAp").text === "235" 
        $$("table.score tbody tr").get(1).find(".earnedAp").text === "112" 
        $$("table.score tbody tr").get(1).find(".startWalked").text === "343"
        $$("table.score tbody tr").get(1).find(".endWalked").text === "400" 
        $$("table.score tbody tr").get(1).find(".earnedWalked").text === "57" 

        $("table.score th.earnedWalked").click()
        $("table.score th.earnedWalked.ordered").should(Condition.visible)
        $("table.score th.earnedWalked .asc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".rank").text === "1" 
        $$("table.score tbody tr").get(0).find(".agentName").text === "agent01" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".startLevel").text === "9"
        $$("table.score tbody tr").get(0).find(".endLevel").text === "9"
        $$("table.score tbody tr").get(0).find(".earnedLevel").text === "0"
        $$("table.score tbody tr").get(0).find(".startAp").text === "223"
        $$("table.score tbody tr").get(0).find(".endAp").text === "334"
        $$("table.score tbody tr").get(0).find(".earnedAp").text === "111"
        $$("table.score tbody tr").get(0).find(".startWalked").text === "445" 
        $$("table.score tbody tr").get(0).find(".endWalked").text === "500" 
        $$("table.score tbody tr").get(0).find(".earnedWalked").text === "55" 

        $$("table.score tbody tr").get(1).find(".rank").text === "2" 
        $$("table.score tbody tr").get(1).find(".agentName").text === "agent00" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".startLevel").text === "8" 
        $$("table.score tbody tr").get(1).find(".endLevel").text === "9" 
        $$("table.score tbody tr").get(1).find(".earnedLevel").text === "1" 
        $$("table.score tbody tr").get(1).find(".startAp").text === "123" 
        $$("table.score tbody tr").get(1).find(".endAp").text === "235" 
        $$("table.score tbody tr").get(1).find(".earnedAp").text === "112" 
        $$("table.score tbody tr").get(1).find(".startWalked").text === "343"
        $$("table.score tbody tr").get(1).find(".endWalked").text === "400" 
        $$("table.score tbody tr").get(1).find(".earnedWalked").text === "57" 

        $("table.score th.earnedWalked").click()
        $("table.score th.earnedWalked.ordered").should(Condition.visible)
        $("table.score th.earnedWalked .desc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".rank").text === "1" 
        $$("table.score tbody tr").get(0).find(".agentName").text === "agent00" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".startLevel").text === "8" 
        $$("table.score tbody tr").get(0).find(".endLevel").text === "9" 
        $$("table.score tbody tr").get(0).find(".earnedLevel").text === "1" 
        $$("table.score tbody tr").get(0).find(".startAp").text === "123" 
        $$("table.score tbody tr").get(0).find(".endAp").text === "235" 
        $$("table.score tbody tr").get(0).find(".earnedAp").text === "112" 
        $$("table.score tbody tr").get(0).find(".startWalked").text === "343"
        $$("table.score tbody tr").get(0).find(".endWalked").text === "400" 
        $$("table.score tbody tr").get(0).find(".earnedWalked").text === "57" 

        $$("table.score tbody tr").get(1).find(".rank").text === "2" 
        $$("table.score tbody tr").get(1).find(".agentName").text === "agent01" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".startLevel").text === "9"
        $$("table.score tbody tr").get(1).find(".endLevel").text === "9"
        $$("table.score tbody tr").get(1).find(".earnedLevel").text === "0"
        $$("table.score tbody tr").get(1).find(".startAp").text === "223"
        $$("table.score tbody tr").get(1).find(".endAp").text === "334"
        $$("table.score tbody tr").get(1).find(".earnedAp").text === "111"
        $$("table.score tbody tr").get(1).find(".startWalked").text === "445" 
        $$("table.score tbody tr").get(1).find(".endWalked").text === "500" 
        $$("table.score tbody tr").get(1).find(".earnedWalked").text === "55" 

        $("table.score th.agentName").click()
        $("table.score th.agentName.ordered").should(Condition.visible)
        $("table.score th.agentName .asc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".rank").text === "1" 
        $$("table.score tbody tr").get(0).find(".agentName").text === "agent00" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".startLevel").text === "8" 
        $$("table.score tbody tr").get(0).find(".endLevel").text === "9" 
        $$("table.score tbody tr").get(0).find(".earnedLevel").text === "1" 
        $$("table.score tbody tr").get(0).find(".startAp").text === "123" 
        $$("table.score tbody tr").get(0).find(".endAp").text === "235" 
        $$("table.score tbody tr").get(0).find(".earnedAp").text === "112" 
        $$("table.score tbody tr").get(0).find(".startWalked").text === "343"
        $$("table.score tbody tr").get(0).find(".endWalked").text === "400" 
        $$("table.score tbody tr").get(0).find(".earnedWalked").text === "57" 

        $$("table.score tbody tr").get(1).find(".rank").text === "2" 
        $$("table.score tbody tr").get(1).find(".agentName").text === "agent01" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".startLevel").text === "9"
        $$("table.score tbody tr").get(1).find(".endLevel").text === "9"
        $$("table.score tbody tr").get(1).find(".earnedLevel").text === "0"
        $$("table.score tbody tr").get(1).find(".startAp").text === "223"
        $$("table.score tbody tr").get(1).find(".endAp").text === "334"
        $$("table.score tbody tr").get(1).find(".earnedAp").text === "111"
        $$("table.score tbody tr").get(1).find(".startWalked").text === "445" 
        $$("table.score tbody tr").get(1).find(".endWalked").text === "500" 
        $$("table.score tbody tr").get(1).find(".earnedWalked").text === "55" 

        $("table.score th.agentName").click()
        $("table.score th.agentName.ordered").should(Condition.visible)
        $("table.score th.agentName .desc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".rank").text === "1" 
        $$("table.score tbody tr").get(0).find(".agentName").text === "agent01" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".startLevel").text === "9"
        $$("table.score tbody tr").get(0).find(".endLevel").text === "9"
        $$("table.score tbody tr").get(0).find(".earnedLevel").text === "0"
        $$("table.score tbody tr").get(0).find(".startAp").text === "223"
        $$("table.score tbody tr").get(0).find(".endAp").text === "334"
        $$("table.score tbody tr").get(0).find(".earnedAp").text === "111"
        $$("table.score tbody tr").get(0).find(".startWalked").text === "445" 
        $$("table.score tbody tr").get(0).find(".endWalked").text === "500" 
        $$("table.score tbody tr").get(0).find(".earnedWalked").text === "55" 

        $$("table.score tbody tr").get(1).find(".rank").text === "2" 
        $$("table.score tbody tr").get(1).find(".agentName").text === "agent00" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".startLevel").text === "8" 
        $$("table.score tbody tr").get(1).find(".endLevel").text === "9" 
        $$("table.score tbody tr").get(1).find(".earnedLevel").text === "1" 
        $$("table.score tbody tr").get(1).find(".startAp").text === "123" 
        $$("table.score tbody tr").get(1).find(".endAp").text === "235" 
        $$("table.score tbody tr").get(1).find(".earnedAp").text === "112" 
        $$("table.score tbody tr").get(1).find(".startWalked").text === "343"
        $$("table.score tbody tr").get(1).find(".endWalked").text === "400" 
        $$("table.score tbody tr").get(1).find(".earnedWalked").text === "57" 

        $("#onlyOrphanRecord input").isSelected === false
        $("#onlyOrphanRecord").click()
        $("#onlyOrphanRecord input").isSelected === true

        $$("table.score tbody tr").shouldHaveSize(3)

        $("table.score th.agentName.ordered").should(Condition.visible)
        $("table.score th.agentName .asc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".agentName").text === "agent02" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".agentLevel").text === "1" 
        $$("table.score tbody tr").get(0).find(".ap").text === "423" 
        $$("table.score tbody tr").get(0).find(".walked").text === "545" 

        $$("table.score tbody tr").get(1).find(".agentName").text === "agent03" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".agentLevel").text === "10" 
        $$("table.score tbody tr").get(1).find(".ap").text === "134" 
        $$("table.score tbody tr").get(1).find(".walked").text === "100" 

        $$("table.score tbody tr").get(2).find(".agentName").text === "agent04"
        $$("table.score tbody tr").get(2).find(".faction").text === "Enlightened"
        $$("table.score tbody tr").get(2).find(".agentLevel").text === "11"
        $$("table.score tbody tr").get(2).find(".ap").text === "1,134" 
        $$("table.score tbody tr").get(2).find(".walked").text === "2,100" 

        $("table.score th.agentName.ordered").click()
        $("table.score th.agentName.ordered").should(Condition.visible)
        $("table.score th.agentName .desc").should(Condition.visible)

        $$("table.score tbody tr").get(0).find(".agentName").text === "agent04"
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened"
        $$("table.score tbody tr").get(0).find(".agentLevel").text === "11"
        $$("table.score tbody tr").get(0).find(".ap").text === "1,134" 
        $$("table.score tbody tr").get(0).find(".walked").text === "2,100" 

        $$("table.score tbody tr").get(1).find(".agentName").text === "agent03" 
        $$("table.score tbody tr").get(1).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(1).find(".agentLevel").text === "10" 
        $$("table.score tbody tr").get(1).find(".ap").text === "134" 
        $$("table.score tbody tr").get(1).find(".walked").text === "100" 

        $$("table.score tbody tr").get(2).find(".agentName").text === "agent02" 
        $$("table.score tbody tr").get(2).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(2).find(".agentLevel").text === "1" 
        $$("table.score tbody tr").get(2).find(".ap").text === "423" 
        $$("table.score tbody tr").get(2).find(".walked").text === "545" 

        $("#onlyOrphanRecord").click()
        $("#onlyOrphanRecord input").isSelected === false
        $$("table.score tbody tr").shouldHaveSize(2)

        open("/agentRecords/" + site02.id.get.value)
        $$("table.score tbody tr").shouldHaveSize(1)

        $$("table.score tbody tr").get(0).find(".rank").text === "1" 
        $$("table.score tbody tr").get(0).find(".agentName").text === "agent01" 
        $$("table.score tbody tr").get(0).find(".faction").text === "Enlightened" 
        $$("table.score tbody tr").get(0).find(".startLevel").text === "10" 
        $$("table.score tbody tr").get(0).find(".endLevel").text === "11" 
        $$("table.score tbody tr").get(0).find(".earnedLevel").text === "1" 
        $$("table.score tbody tr").get(0).find(".startAp").text === "1,223" 
        $$("table.score tbody tr").get(0).find(".endAp").text === "1,334" 
        $$("table.score tbody tr").get(0).find(".earnedAp").text === "111"
        $$("table.score tbody tr").get(0).find(".startWalked").text === "1,445" 
        $$("table.score tbody tr").get(0).find(".endWalked").text === "1,500" 
        $$("table.score tbody tr").get(0).find(".earnedWalked").text === "55"

        open("/")
        $("#adminLink").click()

        $("#userName").setValue(Helper.TestUserName)
        $("#password").setValue(Helper.TestPassword)
        $("#loginButton").click()
        $("#logoffButton").should(Condition.visible)

        open("/agentRecords/" + site01.id.get.value)
        $$("table.score tbody tr").shouldHaveSize(2)
        $("th.removeAllAgentRecords .button").should(Condition.visible)

        open("/agentRecords/" + site02.id.get.value)
        $$("table.score tbody tr").shouldHaveSize(1)
        $("th.removeAllAgentRecords .button").should(Condition.hidden)
      }
    }
  }
}
