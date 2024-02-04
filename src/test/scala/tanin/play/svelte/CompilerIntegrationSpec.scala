package tanin.play.svelte

import java.io.File
import java.nio.file.Files

import helpers.BaseSpec
import sbt.internal.util.ManagedLogger
import sbt.{ Tests => _, _ }
import utest._

object CompilerIntegrationSpec extends BaseSpec {

  def runTest(webpackConfigFilename: String) = {
    val targetDir = Files.createTempDirectory("sbt-svelte-compiler-integration-spec").toFile
    println(targetDir)
    val compiler = new Compiler(
      webpackBinary = if (sys.props.getOrElse("os.name", "").toLowerCase.contains("win")) {
        new File("node_modules") / ".bin" / "webpack.cmd" // Detect Windows
      } else {
        new File("node_modules") / ".bin" / "webpack"
      },
      webpackConfig = new File("src") / "test" / "scala" / "tanin" / "play" / "svelte" / "assets" / webpackConfigFilename,
      sourceDir = new File("src") / "test" / "scala" / "tanin" / "play" / "svelte" / "assets",
      targetDir = targetDir,
      isProd = true,
      logger = mock[ManagedLogger],
      nodeModules = new File("node_modules")
    )

    val baseInputDir = new File("src") / "test" / "scala" / "tanin" / "play" / "svelte" / "assets" / "svelte"
    val componentA   = baseInputDir / "component-a.svelte"
    val componentD   = baseInputDir / "component-d.svelte"
    val componentB   = baseInputDir / "dependencies/_component-b.svelte"
    val componentC   = baseInputDir / "dependencies/_component-c.svelte"
    val inputs       = Seq(componentA, componentD)
    val result       = compiler.compile(inputs.map(_.toPath))

    result.success ==> true
    result.entries.size ==> 2

    result.entries.head.inputFile ==> componentA
    result.entries.head.filesWritten.size ==> 2
    result.entries.head.filesWritten.foreach { fileWritten =>
      Files.exists(fileWritten) ==> true
    }
    result.entries.head.filesWritten ==> Set(
      (targetDir / "svelte" / "component-a.js").toPath, 
      (targetDir / "svelte" / "component-a.css").toPath
    )
    result.entries.head.filesRead ==> Set(componentA.toPath, componentB.toPath, componentC.toPath)

    result.entries(1).inputFile ==> componentD
    result.entries(1).filesWritten.size ==> 1
    result.entries(1).filesWritten.foreach { fileWritten =>
      Files.exists(fileWritten) ==> true
    }
    result.entries(1).filesWritten ==> Set((targetDir / "svelte" / "component-d.js").toPath)
    result.entries(1).filesRead ==> Set(componentD.toPath, componentC.toPath)
  }

  val tests = Tests {
    'compile - {
      "run webpack and get result correctly (webpack.config.js)" - {
        runTest("webpack.config.js")
      }
    }
  }
}
