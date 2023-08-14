lazy val root =
  Project("plugins", file(".")).aggregate(sbtSvelte).dependsOn(sbtSvelte)
lazy val sbtSvelte = RootProject(file("./..").getCanonicalFile.toURI)

addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.8.19")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.0.0")
