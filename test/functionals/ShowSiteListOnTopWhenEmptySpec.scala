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

class ShowSiteListOnTopWhenEmptySpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "Show site list on top page" should {
    "Empty message should be shown if there is no sites" in new WithServer(app = appl, port = testPort) {
      open("/")
      $(".emptyMessage").should(Condition.visible)
    }
  }
}

