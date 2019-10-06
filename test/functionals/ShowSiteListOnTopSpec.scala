package functionals

import models._
import org.specs2.mutable.Specification
import play.api.{Application => PlayApp}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithServer
import play.api.test.Helpers._
import com.codeborne.selenide.Selenide._
import com.codeborne.selenide.{Browsers, Condition, Configuration, WebDriverRunner}
import helpers.{Helper, InjectorSupport}
import models.{UserRepo, UserRole}
import org.fluentlenium.core.conditions.Conditions
import play.api.db.Database
import java.time.Instant
import java.time.ZoneId
import com.codeborne.selenide.CollectionCondition

class ShowSiteListOnTopSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Show site list on top page" should {
    "Sites should be shown in descending order of held_on_utc" in new WithServer(app = appl, port = testPort) {
      inject[Database].withConnection { implicit conn =>
        val siteRepo = inject[SiteRepo]
        val userRepo = inject[UserRepo]

        val user = userRepo.create(
          Helper.TestUserName, "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )

        val site00 = siteRepo.create(
          "site00", heldOnUtc = Instant.ofEpochMilli(3L), heldOnZoneId = ZoneId.systemDefault,
          owner = user.id.get, now = Instant.ofEpochMilli(2L)
        )

        val site01 = siteRepo.create(
          "site01", heldOnUtc = Instant.ofEpochMilli(1L), heldOnZoneId = ZoneId.systemDefault,
          owner = user.id.get, now = Instant.ofEpochMilli(1L)
        )

        val site02 = siteRepo.create(
          "site02", heldOnUtc = Instant.ofEpochMilli(2L), heldOnZoneId = ZoneId.systemDefault,
          owner = user.id.get, now = Instant.ofEpochMilli(3L)
        )
      }

      open("/")

      $$("#siteTable tr").shouldHave(CollectionCondition.size(3))
      $$("#siteTable tr").get(0).find(".siteName .siteNameBody").text === "site00"
      $$("#siteTable tr").get(1).find(".siteName .siteNameBody").text === "site02"
      $$("#siteTable tr").get(2).find(".siteName .siteNameBody").text === "site01"
    }
  }
}
