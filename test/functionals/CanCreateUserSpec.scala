package functionals

import com.codeborne.selenide.Condition
import play.api.i18n.MessagesApi
import helpers.{Helper, InjectorSupport, TimeZoneInfo}
import models.{SiteRepo, User, UserRepo, UserRole}
import org.specs2.mutable.Specification
import play.api.db.Database
import play.api.test.WithServer
import play.api.test.Helpers._
import com.codeborne.selenide.Selenide._
import play.api.i18n.Lang

class CanCreateUserSpec extends Specification with InjectorSupport with UsingSelenide {
  override val conf: Map[String, Any] = inMemoryDatabase()

  "User" should {
    "be created" in new WithServer(app = appl, port = testPort) {
      inject[Database].withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val userRepo = inject[UserRepo]
        val messagesRepo = inject[MessagesApi]
        val user00 = userRepo.create(
          "user0000", "set@your.mail", Helper.TestHash, Helper.TestSalt, UserRole.SUPER
        )

        open("/")
        $("#adminLink").click()
        $("#userName").setValue("user0000")
        $("#password").setValue(Helper.TestPassword)
        $("#loginButton").click()

        $("#userMaintenance").click()
        $("#createUser").click()

        $(".userName .error").shouldHave(Condition.text(messagesRepo("error.minLength", 8)))
        $(".password .error").shouldHave(Condition.text(messagesRepo("error.minLength", 8)))
        $(".email .error").shouldHave(Condition.text(messagesRepo("error.email")))
        $(".email .error").shouldHave(Condition.text(messagesRepo("error.required")))

        $("#userName").setValue("user000")
        $("#password").setValue("user000")
        $("#email").setValue("user000")
        $("#createUser").click()

        $(".userName .error").shouldHave(Condition.text(messagesRepo("error.minLength", 8)))
        $(".password .error").shouldHave(Condition.text(messagesRepo("error.minLength", 8)))
        $(".email .error").shouldHave(Condition.text(messagesRepo("error.email")))

        $("#userName").setValue("user0001")
        $("#password").setValue("password")
        $("#email").setValue("user000@null.com")
        $("#createUser").click()

        $(".message").shouldHave(Condition.text(messagesRepo("registerCompleted")))

        val records: Seq[User] = userRepo.list().records
        records.size === 2
        records.find(_.name == "user0001").isDefined

        $("#userName").setValue("user0001")
        $("#password").setValue("password")
        $("#email").setValue("user000@null.com")
        $("#createUser").click()

        $(".message").shouldHave(Condition.text(messagesRepo("duplicated")))

        $("#logoffButton").click()

        $("#logoffButton").shouldNotHave(Condition.visible)

        open("/")
        $("#adminLink").click()
        $("#userName").setValue("user0001")
        $("#password").setValue("password")
        $("#loginButton").click()

        $("#siteMaintenance").shouldHave(Condition.visible)
        // Since user0001 is not super user, cannot create user.
        $("#userMaintenance").shouldNotHave(Condition.visible)
      }
    }
  }
}

