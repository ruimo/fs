package functionals

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
import org.specs2.specification.AfterAll

class LoginSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()
  def appl: PlayApp = GuiceApplicationBuilder().configure(conf).build()

  "Login" should {
    "Show login page for admin" in new WithServer(app = appl, port = testPort) {
      val userRepo: UserRepo = inject[UserRepo]
      val db = inject[Database]
      db.withConnection { implicit conn =>
        val user = userRepo.create(
          Helper.TestUserName, "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.ADMIN
        )
      }

      open("/")
      $("#adminLink").click()

      $("#userName").setValue(Helper.TestUserName)
      $("#password").setValue(Helper.TestPassword)
      $("#loginButton").click()
      $("#logoffButton").should(Condition.visible)
    }
  }
}
