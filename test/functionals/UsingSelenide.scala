package functionals

import play.api.test.Helpers._
import com.codeborne.selenide.{Browsers, Configuration, WebDriverRunner}
import helpers.InjectorSupport
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.AroundEach

trait UsingSelenide extends AroundEach {
  import UsingSelenide.BrowserKind

  def around[R: AsResult](r: => R): Result = {
    setUp()
    try AsResult(r)
    finally shutdown()
  }

  def setUp(): Unit = browserKind match {
    case BrowserKind.Chrome =>
      Configuration.browser = Browsers.CHROME
      WebDriverRunner.setWebDriver(chrome)
  }

  def shutdown(): Unit = {
    WebDriverRunner.closeWebDriver()
  }

  val conf: Map[String, Any]

  lazy val browserKind: BrowserKind = BrowserKind(conf)

  lazy val chrome: ChromeDriver = {
    val opt = new ChromeOptions()
    val prefs = new java.util.HashMap[String, AnyRef]()
    prefs.put("intl.accept_languages", "ja-JP")
    opt.setExperimentalOption("prefs", prefs)
    new ChromeDriver(opt)
  }
}

object UsingSelenide extends InjectorSupport {
  sealed trait BrowserKind

  object BrowserKind {
    case object Chrome extends BrowserKind

    def apply(conf: Map[String, Any] = inMemoryDatabase()): BrowserKind = conf.get("browser.kind") match {
      case None => Chrome
      case Some(_) => Chrome
    }
  }
}

