lazy val root =
  Project("plugins", file(".")).aggregate(sbtSvelte, sbtPostcss).dependsOn(sbtSvelte, sbtPostcss)
lazy val sbtSvelte = RootProject(file("./..").getCanonicalFile.toURI)
lazy val sbtPostcss = RootProject(uri("https://github.com/tanin47/sbt-postcss.git#7df3d113993f31523381c63103091e96636f8684"))

addSbtPlugin("com.github.sbt" % "sbt-digest" % "2.1.0")
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.7")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.0.0")
