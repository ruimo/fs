package controllers

import javax.inject._

import play.api.libs.json.Json
import play.api.mvc._
import version.BuildInfo

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def version = Action {
    Ok(
      Json.obj(
        "name" -> BuildInfo.name,
        "version" -> BuildInfo.version
      )
    )
  }
}
