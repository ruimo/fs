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

class CanResetAgentRecordSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Can reset agent record" should {
    "Empty message should be shown if there is no sites" in new WithServer(app = appl, port = testPort) {
      inject[Database].withConnection { implicit conn =>
        val siteRepo = inject[SiteRepo]
        val userRepo = inject[UserRepo]
        val agentRecordRepo = inject[AgentRecordRepo]

        val user00 = userRepo.create(
          "user0000", "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )
        val user01 = userRepo.create(
          "user0001", "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )

        val site00 = siteRepo.create(
          "site00", heldOnUtc = Instant.ofEpochMilli(3L), heldOnZoneId = ZoneId.systemDefault,
          owner = user00.id.get, now = Instant.ofEpochMilli(2L)
        )
        val site01 = siteRepo.create(
          "site01", heldOnUtc = Instant.ofEpochMilli(3L), heldOnZoneId = ZoneId.systemDefault,
          owner = user01.id.get, now = Instant.ofEpochMilli(2L)
        )

        open("/")
        $("#adminLink").click()
        $("#userName").setValue("user0000")
        $("#password").setValue(Helper.TestPassword)
        $("#loginButton").click()
        $(".panel .panel-block a").click()

        $$("#siteTable tr").shouldHaveSize(1)
        $("#siteTable tr .siteName").text.trim === "site00"

        open("/attend/" + site00.id.get.value)
        $(".termOfUseConfirm").should(Condition.visible)
        $(".termOfUseConfirm .confirm.button").click()

        val tsvStr = new String(Files.readAllBytes(Paths.get("testdata/tsv/case02.tsv")), "utf-8")
        val tsv = Tsv.parse(tsvStr)

        while ($("#tsv").text.trim == "") {
          TsvTool.setTsvTo(tsvStr, $("#tsv"))
          $("#tsv").sendKeys(" ")
          $("#tsv").sendKeys(Keys.BACK_SPACE)
          Thread.sleep(100)
        }

        $(".registerRecord").getAttribute("disabled") === null
        $(".registerRecord").click()

        $$(".records tbody tr").shouldHaveSize(1)

        val tsvStrAft = tsvStr.replace(
          "\t14\t", "\t16\t",
        ).replace(
          "312897982", "312897992"
        ).replace(
          "8506", "8507"
        )
        while ($("#tsv").text.trim == "") {
          TsvTool.setTsvTo(tsvStrAft, $("#tsv"))
          $("#tsv").sendKeys(" ")
          $("#tsv").sendKeys(Keys.BACK_SPACE)
          Thread.sleep(100)
        }

        $(".registerRecord").getAttribute("disabled") === null
        $(".registerRecord").click()

        open("/agentRecords/" + site00.id.get.value)
        $(".siteName").shouldHave(Condition.text("site00"))
        $$("table.score tbody tr").shouldHaveSize(1)

        open("/attend/" + site01.id.get.value)
        val tsv2Str = tsvStr.replace(
          "shanai", "shanai2"
        )
        while ($("#tsv").text.trim == "") {
          TsvTool.setTsvTo(tsv2Str, $("#tsv"))
          $("#tsv").sendKeys(" ")
          $("#tsv").sendKeys(Keys.BACK_SPACE)
          Thread.sleep(100)
        }
        $(".registerRecord").click()

        val tsv2StrAft = tsv2Str.replace(
          "\t14\t", "\t15\t",
        ).replace(
          "312897982", "312897999"
        ).replace(
          "8506", "8509"
        )
        while ($("#tsv").text.trim == "") {
          TsvTool.setTsvTo(tsv2StrAft, $("#tsv"))
          $("#tsv").sendKeys(" ")
          $("#tsv").sendKeys(Keys.BACK_SPACE)
          Thread.sleep(100)
        }
        $(".registerRecord").click()

        open("/attend/" + site01.id.get.value)
        $$(".records tbody tr").shouldHaveSize(2)

        open("/agentRecords/" + site00.id.get.value)
        $("table.score tr th.removeAllAgentRecords .button").should(Condition.visible)

        $("table.score tr th.removeAllAgentRecords .button").click()
        $(".button.cancel").click()
        $$("table.score tbody tr").shouldHaveSize(1)

        $("table.score tr th.removeAllAgentRecords .button").click()
        $(".clearAllAgentRecordsConfirm .clearAgentRecords").click()
        $$("table.score tbody tr").shouldHaveSize(0)

        agentRecordRepo.list(site00.id.get).isEmpty === true
        agentRecordRepo.list(site01.id.get).isEmpty === false

        open("/agentRecords/" + site01.id.get.value)
        $("table.score tr th.removeAllAgentRecords .placeHolder").should(Condition.visible)
      }
    }
  }
}

