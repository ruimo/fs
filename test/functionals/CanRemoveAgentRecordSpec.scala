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

class CanRemoveAgentRecordSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Agent record" should {
    "Can remove agent record" in new WithServer(app = appl, port = testPort) {
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

        TsvTool.setTsvTo(tsvStr, $("#tsv"))
        $("#tsv").sendKeys(" ")
        $("#tsv").sendKeys(Keys.BACK_SPACE)

        $(".startRecord").getAttribute("disabled") === null
        $(".startRecord").click()

        $$(".records tbody tr").shouldHaveSize(1)

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

        $(".showScore a").click()
        $(".siteName").shouldHave(Condition.text("site00"))
        $$("table.score tbody tr").shouldHaveSize(1)

        open("/attend/" + site00.id.get.value)
        val tsv2Str = tsvStr.replace(
          "shanai", "shanai2"
        )
        TsvTool.setTsvTo(tsv2Str, $("#tsv"))
        $("#tsv").sendKeys(" ")
        $("#tsv").sendKeys(Keys.BACK_SPACE)
        $(".agentNameWrapper .clear").click()
        $(".dialogButtons .clear").click()
        $(".startRecord").click()

        val tsv2StrAft = tsv2Str.replace(
          "\t14\t", "\t15\t",
        ).replace(
          "312897982", "312897999"
        ).replace(
          "8506", "8509"
        )
        TsvTool.setTsvTo(tsv2StrAft, $("#tsv"))
        $("#tsv").sendKeys(" ")
        $("#tsv").sendKeys(Keys.BACK_SPACE)
        $(".endRecord").click()

        open("/agentRecords/" + site00.id.get.value)
        $$("table.score tbody tr").shouldHaveSize(2)
        $$("table.score tbody tr").get(0).find(".agentName").text === "shanai2"
        $$("table.score tbody tr").get(0).find(".removeRecord button").click()
        $(".clearAgentRecordConfirm .dialogButtons .cancel").click()

        $$("table.score tbody tr").shouldHaveSize(2)
        $$("table.score tbody tr").get(0).find(".agentName").text === "shanai2"
        $$("table.score tbody tr").get(0).find(".removeRecord button").click()
        $(".clearAgentRecordConfirm .dialogButtons .clearAgentRecord").click()
        $$("table.score tbody tr").shouldHaveSize(1)
        $$("table.score tbody tr").get(0).find(".agentName").text === "shanai"
      }
    }
  }
}

