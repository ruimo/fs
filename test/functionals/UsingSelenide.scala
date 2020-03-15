package functionals

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application => PlayApp}
import play.api.test.Helpers._
import com.codeborne.selenide.{Browsers, Configuration, Selenide, WebDriverRunner}
import helpers.InjectorSupport
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions, FirefoxProfile}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.AroundEach
import org.specs2.specification.AfterAll
import org.specs2.specification.BeforeAll

trait UsingSelenide extends AroundEach {
  import UsingSelenide.BrowserKind

  def around[R: AsResult](r: => R): Result = {
    setUp()
    try AsResult(r)
    finally shutdown()
  }

  def setUp(): Unit = {
    Configuration.baseUrl = "http://localhost:" + testPort

    browserKind match {
      case BrowserKind.Chrome =>
        Configuration.browser = Browsers.CHROME
        WebDriverRunner.setWebDriver(chrome())

      case BrowserKind.Firefox =>
        Configuration.browser = Browsers.FIREFOX
        WebDriverRunner.setWebDriver(firefox())
    }
  }

  def shutdown(): Unit = {
    WebDriverRunner.closeWebDriver()
  }

  val testPort: Int = 9080

  val conf: Map[String, Any]

  lazy val appl: PlayApp = GuiceApplicationBuilder().configure(conf).build()

  lazy val browserKind: BrowserKind = BrowserKind(conf)

  def chrome(): ChromeDriver = {
    val opt = new ChromeOptions()
    val prefs = new java.util.HashMap[String, AnyRef]()
    prefs.put("intl.accept_languages", "ja-JP")
    opt.setExperimentalOption("prefs", prefs)
    opt.addArguments("--disable-gpu")
    opt.addArguments("--disable-seccomp-filter-sandbox")
    opt.addArguments("--disable-extensions")
    new ChromeDriver(opt)
  }

  def firefox(): FirefoxDriver = {
    val options = new FirefoxOptions()
    options.addPreference("intl.accept_languages", "ja")
    new FirefoxDriver(options)
  }
}

object UsingSelenide extends InjectorSupport {
  sealed trait BrowserKind

  object BrowserKind {
    case object Chrome extends BrowserKind
    case object Firefox extends BrowserKind

    def apply(conf: Map[String, Any] = inMemoryDatabase()): BrowserKind = {
      val config = Option(System.getenv("BROWSER_KIND"))
      println("browser.kind = '" + config + "'")

      val browserKind = config match {
        case None => Chrome
        case Some("firefox") => Firefox
        case Some(_) => Chrome
      }

      println("browser = " + browserKind)
      browserKind
    }
  }
}

