organization := "tanin.play.svelte"
name := "sbt-svelte"

lazy val `sbt-svelte` = (project in file("."))
  .enablePlugins(SbtWebBase)
  .settings(
    scalaVersion := "2.12.18",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json"   % "2.8.1",
      "org.mockito"       % "mockito-core" % "3.0.0" % Test,
      "com.lihaoyi"       %% "utest"       % "0.7.1" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

addSbtWeb("1.5.0-M1")

addCommandAlias(
  "fmt",
  "all scalafmtSbt scalafmt test:scalafmt"
)
addCommandAlias(
  "check",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)
