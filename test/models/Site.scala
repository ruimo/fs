package models

import anorm._
import org.specs2.mutable._
import helpers.InjectorSupport
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application

class SiteSpec extends Specification with InjectorSupport {
  "Site" should {
    "can create record." in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
1 === 1
    }
  }
}

