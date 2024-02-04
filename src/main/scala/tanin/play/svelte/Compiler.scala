package tanin.play.svelte

import java.io.{ File, FileOutputStream, PrintWriter }
import java.nio.file.{ Files, Path }

import play.api.libs.json._
import sbt.internal.util.ManagedLogger

import scala.io.Source

case class CompilationResult(success: Boolean, entries: Seq[CompilationEntry])
case class CompilationEntry(inputFile: File, filesRead: Set[Path], filesWritten: Set[Path])
case class Input(name: String, path: Path)

class Shell {
  def execute(logger: ManagedLogger, cmd: String, cwd: File, envs: (String, String)*): Int = {
    import scala.sys.process._

    val envString = if (envs.nonEmpty) {
      envs.toList.map { case (k, v) => s"$k=$v"}.mkString(" ") + " "
    } else {
      ""
    }

    logger.info(s"Executing: ${envString}${cmd}")

    val exitCode = Process(cmd, cwd, envs: _*).!
    logger.info(s"Exited with $exitCode")

    exitCode
  }

  def fileExists(file: File): Boolean = file.exists()
}

class ComputeDependencyTree {

  val LOCAL_PATH_PREFIX_REGEX = "^\\./".r

  def sanitize(s: String): String =
    LOCAL_PATH_PREFIX_REGEX.replaceAllIn(s, "").replaceAllLiterally("/", sbt.Path.sep.toString)

  def apply(file: File): Map[String, Set[String]] =
    apply(scala.io.Source.fromFile(file).mkString)

  def apply(content: String): Map[String, Set[String]] = {
    val json = Json.parse(content)

    val deps = json
      .as[JsArray]
      .value
      .flatMap { obj =>
        // For some reason, webpack or svelte-loader includes the string ` + 4 modules` in `name`.
        val name = obj("name").as[String].split(" \\+ ").head
        val relations = obj("reasons")
          .as[Seq[JsValue]]
          .flatMap {
            case JsNull => None
            // For some reason, webpack or svelte-loader includes the string ` + 4 modules` in `moduleName`.
            // See: https://github.com/webpack/webpack/issues/8507
            case JsString(v) => Some(v.split(" \\+ ").head)
            case _           => throw new IllegalArgumentException()
          }
          .map { reason =>
            reason -> name
          }

        relations ++ Seq(name -> name) // the file also depends on itself.
      }
      .groupBy { case (key, _) => key }
      .mapValues(_.map(_._2).toSet)

    flatten(deps)
      // We only care about our directories.
      // The path separator here is always `/`, even on windows.
      .filter { case (key, _) => key.startsWith("./") }.mapValues { vs =>
      vs.filter { v =>
        // There are some dependencies that we don't care about.
        // An example: ./vue/component-a.vue?vue&type=style&index=0&id=f8aaa26e&scoped=true&lang=scss&
        // Another example: /home/tanin/projects/sbt-svelte/node_modules/vue-style-loader!/home/ta...
        v.startsWith("./") && !v.contains("?")
      }
    }.map {
      case (key, values) =>
        sanitize(key) -> values.map(sanitize)
    }
  }

  private[this] def flatten(deps: Map[String, Set[String]]): Map[String, Set[String]] = {
    var changed = false
    val newDeps = deps.map {
      case (key, children) =>
        val newChildren = children ++ children.flatMap { v =>
          deps.getOrElse(v, Set.empty)
        }
        if (newChildren.size != children.size) {
          changed = true
        }
        key -> newChildren
    }

    if (changed) {
      flatten(newDeps)
    } else {
      newDeps
    }
  }
}

class PrepareWebpackConfig {
  def apply(originalWebpackConfig: File, inputs: Seq[Input]) = {
    import sbt._

    val tmpDir     = Files.createTempDirectory("sbt-svelte")
    val targetFile = tmpDir.toFile / "webpack.config.js"

    Files.copy(originalWebpackConfig.toPath, targetFile.toPath)

    val webpackConfigFile = new PrintWriter(new FileOutputStream(targetFile, true))
    try {
      val entries = inputs.map { input =>
        input.name -> JsString(input.path.toAbsolutePath.toString)
      }

      webpackConfigFile.write("\n")
      webpackConfigFile.write(s"""
                                 |const userDefinedModuleExports = module.exports;
                                 |
                                 |module.exports = (env, argv) => {
                                 |  let config;
                                 |  if (typeof userDefinedModuleExports === 'function') {
                                 |    config = userDefinedModuleExports(env, argv);
                                 |  } else {
                                 |    config = userDefinedModuleExports;
                                 |  }
                                 |  config.entry = ${Json.prettyPrint(JsObject(entries))};
                                 |  config.output = config.output || {};
                                 |  config.output.publicPath = config.output.publicPath || '/assets';
                                 |  config.output.library = config.output.library || '[camel-case-name]';
                                 |  config.output.filename = config.output.filename || '[name].js';
                                 |
                                 |  const SbtJsPlugin = require('./sbt-js-plugin.js');
                                 |  config.plugins = config.plugins || [];
                                 |  config.plugins.push(new SbtJsPlugin());
                                 |  return config;
                                 |}
                                 |
        """.stripMargin)
    } finally {
      webpackConfigFile.close()
    }

    val sbtJsPluginFile = new PrintWriter(tmpDir.toFile / "sbt-js-plugin.js")
    try {
      sbtJsPluginFile.write(Source.fromInputStream(getClass.getResourceAsStream("/sbt-js-plugin.js")).mkString)
    } finally {
      sbtJsPluginFile.close()
    }

    targetFile.getAbsolutePath
  }
}

class Compiler(
  webpackBinary: File,
  webpackConfig: File,
  sourceDir: File,
  targetDir: File,
  isProd: Boolean,
  logger: ManagedLogger,
  nodeModules: File,
  shell: Shell = new Shell,
  dependencyComputer: ComputeDependencyTree = new ComputeDependencyTree,
  prepareWebpackConfig: PrepareWebpackConfig = new PrepareWebpackConfig
) {

  def compile(inputFiles: Seq[Path]): CompilationResult = {
    import sbt._

    if (inputFiles.isEmpty) {
      return CompilationResult(success = true, entries = Seq.empty)
    }

    val inputs = inputFiles.map { inputFile =>
      val name = sourceDir.toPath.relativize((inputFile.getParent.toFile / inputFile.toFile.base).toPath).toString
      Input(name, inputFile)
    }

    val cmd = Seq(
      webpackBinary.getCanonicalPath,
      "--config",
      prepareWebpackConfig.apply(webpackConfig, inputs),
      "--output-path",
      targetDir.getCanonicalPath,
      "--mode",
      if (isProd) {
        "production"
      } else {
        "development"
      }
    ).mkString(" ")

    val exitCode = shell.execute(logger, cmd, sourceDir, "NODE_PATH" -> nodeModules.getCanonicalPath)
    val success = exitCode == 0

    CompilationResult(
      success = success,
      entries = if (success) {
        val dependencyMap = dependencyComputer.apply(targetDir / "sbt-js-tree.json")

        inputs.map { input =>
          val outputJsRelativePath =
            sourceDir.toPath.relativize((input.path.getParent.toFile / s"${input.path.toFile.base}.js").toPath).toString
          val outputJsFile = targetDir / outputJsRelativePath
          val outputCssRelativePath =
            sourceDir.toPath.relativize((input.path.getParent.toFile / s"${input.path.toFile.base}.css").toPath).toString
          val outputCssFile = targetDir / outputCssRelativePath

          val dependencies = dependencyMap
            .getOrElse(s"${input.name}.svelte", Set.empty)
            .map { relativePath =>
              (sourceDir / relativePath).toPath
            }

          CompilationEntry(
            inputFile = input.path.toFile,
            filesRead = dependencies,
            filesWritten = Set(outputJsFile, outputCssFile).filter(shell.fileExists).map(_.toPath)
          )
        }
      } else {
        Seq.empty
      }
    )
  }
}
