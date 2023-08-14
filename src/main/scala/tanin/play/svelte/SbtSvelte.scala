package tanin.play.svelte

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.incremental._
import sbt.Keys._
import sbt._
import xsbti.{ Position, Problem, Severity }

import scala.io.Source

object SbtSvelte extends AutoPlugin {
  override def requires: Plugins      = SbtWeb
  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    object SvelteKeys {
      val svelte           = TaskKey[Seq[File]]("svelte", "Generate compiled Javascripts files from Svelte components.")
      val webpackBinary   = TaskKey[String]("svelteWebpackBinary", "The binary location for webpack.")
      val webpackConfig   = TaskKey[String]("svelteWebpackConfig", "The location for webpack config.")
      val nodeModulesPath = TaskKey[String]("svelteNodeModules", "The location of the node_modules.")
      val prodCommands = TaskKey[Set[String]](
        "svelteProdCommands",
        "A set of SBT commands that triggers production build. The default is `stage`. In other words, use -p (as opposed to -d) with webpack."
      )
    }
  }

  import autoImport.SvelteKeys._

  val baseSbtSvelteSettings = Seq(
    svelte / excludeFilter := HiddenFileFilter || "_*",
    svelte / includeFilter := "*.svelte",
    prodCommands := Set("stage", "docker:publish", "docker:stage", "docker:publishLocal"),
    nodeModulesPath := "./node_modules",
    webpackBinary := "please-define-the-binary",
    webpackConfig := "please-define-the-config-location.js",
    svelte / resourceManaged := webTarget.value / "svelte" / "main",
    Assets / managedResourceDirectories += (Assets / svelte / resourceManaged).value,
    Assets / resourceGenerators += Assets / svelte,
    Assets / svelte := task.dependsOn(Assets / WebKeys.webModules).value
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSvelteSettings)

  lazy val task = Def.task {
    val sourceDir         = (Assets / sourceDirectory).value
    val targetDir         = (Assets / svelte / resourceManaged).value
    val logger            = (Assets / streams).value.log
    val svelteReporter     = (Assets / reporter).value
    val prodCommandValues = (svelte / prodCommands).value
    val isProd = state.value.currentCommand.exists { exec =>
      val v = prodCommandValues.contains(exec.commandLine)
      logger.info(s"Detected the command: ${exec.commandLine}. Use the production mode: ${v}.")
      v
    }
    val webpackBinaryLocation = (svelte / webpackBinary).value
    val webpackConfigLocation = (svelte / webpackConfig).value
    val nodeModulesLocation   = (svelte / nodeModulesPath).value

    val sources = (sourceDir ** ((Assets / svelte / includeFilter).value -- (Assets / svelte / excludeFilter).value)).get

    implicit val fileHasherIncludingOptions = OpInputHasher[File] { f =>
      OpInputHash.hashString(
        Seq(
          f.getCanonicalPath,
          isProd,
          sourceDir.getAbsolutePath
        ).mkString("--")
      )
    }

    val results = incremental.syncIncremental((Assets / streams).value.cacheDirectory / "run", sources) { modifiedSources =>
      val startInstant = System.currentTimeMillis

      if (modifiedSources.nonEmpty) {
        logger.info(s"[Svelte] Compile on ${modifiedSources.size} changed files")
      } else {
        logger.info(s"[Svelte] No changes to compile")
      }

      val compiler = new Compiler(
        new File(webpackBinaryLocation),
        new File(webpackConfigLocation),
        sourceDir,
        targetDir,
        isProd,
        logger,
        new File(nodeModulesLocation)
      )

      // Compile all modified sources at once
      val result = compiler.compile(modifiedSources.map(_.toPath))

      // Report compilation problems
      CompileProblems.report(
        reporter = svelteReporter,
        problems = if (!result.success) {
          Seq(new Problem {
            override def category() = ""

            override def severity() = Severity.Error

            override def message() = ""

            override def position() = new Position {
              override def line() = java.util.Optional.empty()

              override def lineContent() = ""

              override def offset() = java.util.Optional.empty()

              override def pointer() = java.util.Optional.empty()

              override def pointerSpace() = java.util.Optional.empty()

              override def sourcePath() = java.util.Optional.empty()

              override def sourceFile() = java.util.Optional.empty()
            }
          })
        } else {
          Seq.empty
        }
      )

      // Collect OpResults
      val opResults: Map[File, OpResult] = result.entries.map { entry =>
        entry.inputFile -> OpSuccess(entry.filesRead.map(_.toFile), entry.filesWritten.map(_.toFile))
      }.toMap

      // Collect the created files
      val createdFiles = result.entries.flatMap(_.filesWritten.map(_.toFile))

      val endInstant = System.currentTimeMillis

      if (createdFiles.nonEmpty) {
        logger.info(s"[Svelte] finished compilation in ${endInstant - startInstant} ms and generated ${createdFiles.size} JS files")
      }

      (opResults, createdFiles)

    }(fileHasherIncludingOptions)

    // Return the dependencies
    (results._1 ++ results._2.toSet).toSeq
  }
}
