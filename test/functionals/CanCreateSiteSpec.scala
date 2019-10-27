package functionals

import play.api.i18n.MessagesApi
import helpers.{Helper, InjectorSupport, TimeZoneInfo}
import models.{SiteRepo, UserRepo, UserRole}
import org.specs2.mutable.Specification
import play.api.db.Database
import play.api.test.WithServer
import play.api.test.Helpers._
import com.codeborne.selenide.Selenide._
import play.api.i18n.Lang

class CanCreateSiteSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Site" should {
    "be created" in new WithServer(app = appl, port = testPort) {
      inject[Database].withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val siteRepo = inject[SiteRepo]
        val userRepo = inject[UserRepo]
        val messagesRepo = inject[MessagesApi]
        val user00 = userRepo.create(
          "user0000", "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )

        open("/")
        $("#adminLink").click()
        $("#userName").setValue("user0000")
        $("#password").setValue(Helper.TestPassword)
        $("#loginButton").click()

        $("#siteMaintenance").click()
        $("#siteName").setValue("Test First Saturaday site")
        $("#openTime").setValue("12:34")
        $$("#timeZoneSelect option").get(3).click() // Etc/GMT
        $("#createSite").click()

        $$(".sites tbody tr").shouldHaveSize(1)
        $(".sites tbody .siteName").text.trim === "Test First Saturaday site"
        $(".sites tbody .dateTime").text.endsWith("12:34") === true
        $(".sites tbody .timeZone").text === TimeZoneInfo.table(3).view
        $(".sites tbody .administrator").text === "user0000"

        $("#siteName").setValue("Test First Saturaday site2")
        $("#openTime").setValue("12:35")
        $$("#timeZoneSelect option").get(4).click() // Etc/GMT

        $(".editSite").click()
        $("#siteName").getValue === "Test First Saturaday site"
        $("#openTime").getValue === "12:34"
        $$("#timeZoneSelect option").get(3).text === TimeZoneInfo.table(3).view

        $("#createSite").click()
        $(".control.siteName .error").text === messagesRepo("duplicated")

        $("#siteName").setValue("Test First Saturaday site2")
        $("#openTime").setValue("12:35")
        $("#createSite").click()

        $$(".sites tbody tr").shouldHaveSize(2)

        $$(".sites tbody .siteName").get(0).text.trim === "Test First Saturaday site2"
        $$(".sites tbody .siteName").get(1).text.trim === "Test First Saturaday site"

        $$(".sites tbody .dateTime").get(0).text.endsWith("12:35") === true
        $$(".sites tbody .dateTime").get(1).text.endsWith("12:34") === true
      }
    }
  }
}
