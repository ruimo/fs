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

class ShowResultWithOneAttendeeSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Show result." should {
    "One attendee is registered." in new WithServer(app = appl, port = testPort) {
      inject[Database].withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val msg = inject[MessagesApi]
        val siteRepo = inject[SiteRepo]
        val userRepo = inject[UserRepo]

        val user = userRepo.create(
          Helper.TestUserName, "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )

        val now = Instant.ofEpochMilli(1000L * 60)
        val heldOnUtc = Instant.ofEpochMilli(2000L * 60)
        val zoneId = ZoneId.of("Asia/Tokyo")
        val site = siteRepo.create(
          "site", heldOnUtc, heldOnZoneId = zoneId,
          owner = user.id.get, now
        )

        val attendCtr = inject[AttendController]
        val dateTime = attendCtr.formatter.withZone(zoneId).format(heldOnUtc)

        open("/agentRecords/" + site.id.get.value)
        $(".siteName").text === "site"
        $(".emptyMessage").text === msg("recordEmpty")

        WebDriverRunner.getWebDriver.manage().deleteAllCookies()
        open("/attend/" + site.id.get.value)
        $(".termOfUseConfirm").should(Condition.visible)
        $(".termOfUseConfirm .confirm.button").click()

        $(".dateTime").shouldHave(Condition.text(dateTime))
        $(".timezone").text === TimeZoneInfo.tableByZoneId(zoneId).view
        $(".notification").text === msg("registerBeforeRecordGuide")
        $(".startRecord").getAttribute("disabled") === "true"
        $$(".endRecord.is-hidden").shouldHaveSize(1)

        val tsvStr = new String(Files.readAllBytes(Paths.get("testdata/tsv/case02.tsv")), "utf-8")
        val tsv = Tsv.parse(tsvStr)

        TsvTool.setTsvTo(tsvStr, $("#tsv"))
        $("#tsv").sendKeys(" ")
        $("#tsv").sendKeys(Keys.BACK_SPACE)

        $(".startRecord").getAttribute("disabled") === null
        $(".startRecord").click()

        $$(".records tbody tr").shouldHaveSize(1)
        $(".records tbody .phase").text === msg("registerBeforeRecord")
        $(".records tbody .faction").text === "Enlightened"
        $(".records tbody .agentName").text === "shanai"
        $(".records tbody .agentLevel").text === "14"
        $(".records tbody .lifetimeAp").text === "312,897,982"
        $(".records tbody .distanceWalked").text === "8,506"

        $(".notification").text === msg("registerAfterRecordGuide")
        $(".endRecord").getAttribute("disabled") === "true"

        val tsvStrAft = tsvStr.replace(
          "\t14\t", "\t16\t",
        ).replace(
          "312897982", "312897992"
        ).replace(
          "8506", "8507"
        )
        TsvTool.setTsvTo(tsvStrAft, $("#tsv"))
        $("#tsv").sendKeys(" ")
        $("#tsv").sendKeys(Keys.BACK_SPACE)

        $(".endRecord").getAttribute("disabled") === null
        $(".endRecord").click()

        $$(".records tbody tr").shouldHaveSize(2)

        $$(".records tbody .phase").get(0).text === msg("registerBeforeRecord")
        $$(".records tbody .faction").get(0).text === "Enlightened"
        $$(".records tbody .agentName").get(0).text === "shanai"
        $$(".records tbody .agentLevel").get(0).text === "14"
        $$(".records tbody .lifetimeAp").get(0).text === "312,897,982"
        $$(".records tbody .distanceWalked").get(0).text === "8,506"

        $$(".records tbody .phase").get(1).text === msg("registerAfterRecord")
        $$(".records tbody .faction").get(1).text === "Enlightened"
        $$(".records tbody .agentName").get(1).text === "shanai"
        $$(".records tbody .agentLevel").get(1).text === "16"
        $$(".records tbody .lifetimeAp").get(1).text === "312,897,992"
        $$(".records tbody .distanceWalked").get(1).text === "8,507"

        $(".showScore a").click()
        $(".siteName").shouldHave(Condition.text("site"))

        $$("table.score tbody tr").shouldHaveSize(1)

        $("table.score tbody td.rank").text === "1"
        $("table.score tbody td.agentName").text === "shanai"
        $("table.score tbody td.faction").text === "Enlightened"
        $("table.score tbody td.startLevel").text === "14"
        $("table.score tbody td.endLevel").text === "16"
        $("table.score tbody td.earnedLevel").text === "2"
        $("table.score tbody td.startAp").text === "312,897,982"
        $("table.score tbody td.endAp").text === "312,897,992"
        $("table.score tbody td.earnedAp").text === "10"
        $("table.score tbody td.startWalked").text === "8,506"
        $("table.score tbody td.endWalked").text === "8,507"
        $("table.score tbody td.earnedWalked").text === "1"
      }
    }
  }
}
