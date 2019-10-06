name := """fs"""

lazy val root = (project in file(".")).enablePlugins(PlayScala, BuildInfoPlugin).settings(
  watchSources ++= (baseDirectory.value / "public/ui" ** "*").get,
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "version"
)

publishTo := Some(
  Resolver.file(
    "scoins",
    new File(Option(System.getenv("RELEASE_DIR")).getOrElse("/tmp"))
  )
)

resolvers += "ruimo.com" at "http://static.ruimo.com/release"

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.9"

libraryDependencies += guice
libraryDependencies += jdbc
libraryDependencies += evolutions
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.4"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.6"
libraryDependencies += "com.ruimo" %% "scoins" % "1.22"
libraryDependencies += "com.ruimo" %% "csvparser" % "1.2"

libraryDependencies += specs2 % Test
libraryDependencies += "com.h2database" % "h2" % "1.4.199" % Test
libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % Test
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59" % Test
libraryDependencies += "org.seleniumhq.selenium" % "selenium-remote-driver" % "3.141.59" % Test
libraryDependencies += "org.seleniumhq.selenium" % "selenium-server" % "3.141.59" % Test
libraryDependencies += "com.codeborne" % "selenide" % "5.0.0" % Test
libraryDependencies += "org.specs2" %% "specs2-core" % "4.0.0" % "test"  

javaOptions in Test ++= Option(System.getProperty("webdriver.chrome.driver")).map("-Dwebdriver.chrome.driver=" + _).toSeq
javaOptions in Test ++= Option(System.getProperty("selenide.remote")).map("-Dselenide.remote=" + _).toSeq
javaOptions in Test ++= Option(System.getProperty("test.port")).map("-Dtest.port=" + _).toSeq
javaOptions in Test ++= Option(System.getProperty("test.host")).map("-Dtest.host=" + _).toSeq

fork in Test := true
