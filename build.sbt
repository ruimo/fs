name := """fs"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  watchSources ++= (baseDirectory.value / "public/ui" ** "*").get
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
