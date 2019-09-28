package controllers

import javax.inject._

import play.api.Configuration
import play.api.http.HttpErrorHandler
import play.api.mvc._

/**
  * Frontend controller managing all static resource associate routes.
  * @param assets Assets controller reference.
  * @param cc Controller components reference.
  */
@Singleton
class FrontendController @Inject()(assets: Assets, errorHandler: HttpErrorHandler, config: Configuration, cc: ControllerComponents) extends AbstractController(cc) {
  val apiPrefix = config.get[String]("apiPrefix")

  def index: Action[AnyContent] = assets.at("index.html")

  def assetOrDefault(resource: String): Action[AnyContent] = if (resource.startsWith(apiPrefix)){
    println("Assert '" + resource + "' is api. (apiPrefix: '"  + apiPrefix + "')")
    Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
  } else {
    println("Assert '" + resource + "' is not api. (apiPrefix: '"  + apiPrefix + "')")
    if (resource.contains(".")) assets.at(resource) else index
  }
}
