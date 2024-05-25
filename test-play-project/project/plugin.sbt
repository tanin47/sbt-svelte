lazy val root =
  Project("plugins", file(".")).aggregate(sbtSvelte, sbtPostcss).dependsOn(sbtSvelte, sbtPostcss)
lazy val sbtSvelte = RootProject(file("./..").getCanonicalFile.toURI)
lazy val sbtPostcss = RootProject(uri("https://github.com/tanin47/sbt-postcss.git#ea79ed35a5f4848f2718fe094b4e119bc9e2a538"))

addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.9.2")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.0.0")
