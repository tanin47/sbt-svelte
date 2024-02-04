name := """test-play-project"""
organization := "tanin.play.svelte"
version := "1.0-SNAPSHOT"

bloopAggregateSourceDependencies in Global := true

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtWeb, SbtSvelte)
  .settings(
    scalaVersion := "2.13.11",
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    ),
    Assets / SvelteKeys.svelte / SvelteKeys.prodCommands := Set("stage"),
    Assets / SvelteKeys.svelte / SvelteKeys.webpackBinary := {
      // Detect windows
      if (sys.props.getOrElse("os.name", "").toLowerCase.contains("win")) {
        (new File(".") / "node_modules" / ".bin" / "webpack.cmd").getAbsolutePath
      } else {
        (new File(".") / "node_modules" / ".bin" / "webpack").getAbsolutePath
      }
    },
    Assets / SvelteKeys.svelte / SvelteKeys.webpackConfig := (new File(".") / "webpack.config.js").getAbsolutePath,
    // All non-entry-points components, which are not included directly in HTML, should have the prefix `_`.
    // Webpack shouldn't compile non-entry-components directly. It's wasteful.
    Assets / SvelteKeys.svelte / excludeFilter := "_*",
  )

addCommandAlias(
  "fmt",
  "all scalafmtSbt scalafmt test:scalafmt"
)
addCommandAlias(
  "check",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)
