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

addSbtWeb("1.5.3")

addCommandAlias(
  "fmt",
  "all scalafmtSbt scalafmt test:scalafmt"
)
addCommandAlias(
  "check",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

organization := "io.github.tanin47"
organizationName := "tanin47"

Test / publishArtifact := false

homepage := Some(url("https://github.com/tanin47/sbt-svelte"))

publishMavenStyle := true
publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
pomIncludeRepository := { _ =>
  false
}
licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/tanin47/sbt-svelte"),
    "scm:git@github.com:tanin47/sbt-svelte.git"
  )
)

developers := List(
  Developer(
    id = "tanin",
    name = "Tanin Na Nakorn",
    email = "@tanin",
    url = url("https://github.com/tanin47")
  )
)

versionScheme := Some("semver-spec")
