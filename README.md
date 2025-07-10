sbt-svelte
===========

[![CircleCI](https://circleci.com/gh/tanin47/sbt-svelte.svg?style=svg)](https://circleci.com/gh/tanin47/sbt-svelte)

sbt-svelte integrates Webpack + Svelte into Playframework's asset generation.

It eliminates the need to run a separate node process to hot-reload your Svelte code. Moreover, it enables you to mix between SPA and non-SPA pages in your application; it's your choice whether to use SPA or not.

It works with both `sbt run` (which hot-reloads the code changes) and `sbt stage`.

Please see the example project in the folder `test-play-project`.

Requirements
-------------

* __[Webpack 5.x](https://webpack.js.org/):__ you'll need to specify the webpack binary location and webpack's configuration localtion. This enables you to choose your own version of Webpack and your own Webpack's configuration. You can see an example in the folder `test-play-project`.
* __Playframework 2.8.x__
* __Scala >= 2.12.x and SBT 1.x:__ Because the artifact is only published this setting. If you would like other combinations of Scala and SBT versions, please open an issue.

How to use
-----------

### 1. Install the plugin

Add the below line to `project/plugins.sbt`:

```
lazy val root =
  Project("plugins", file(".")).aggregate(SbtSvelte).dependsOn(SbtSvelte)
lazy val SbtSvelte = RootProject(uri("https://github.com/tanin47/sbt-svelte.git#<pick_a_commit>"))
```

### 2. Configure Webpack config file.

Create `webpack.config.js by copying from `test-play-project/webpack.config.js`

You should NOT specify `module.exports.output` because sbt-svelte will automatically set the field.

Your config file will be copied and added with some required additional code. Then, it will used by sbt-svelte when compiling the components.

When running sbt-svelte, we print the webpack command with the modified `webpack.config.js`, so you can inspect the config that we use.

### 3. Configure `build.sbt`

Specifying necessary configurations:

```
lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb, SbtSvelte) // Enable the plugin

// The location of the webpack binary. For windows, it might be `webpack.cmd`.
Assets / SvelteKeys.svelte / SvelteKeys.webpackBinary := "./node_modules/.bin/webpack"

// The location of the webpack configuration.
Assets / SvelteKeys.svelte / SvelteKeys.webpackConfig := "./webpack.config.js"
```

### 4. Find out where the output JS file is and how to use it

The plugin compiles `*.svelte` within `app/assets`.

For the path `app/assets/svelte/components/some-component.svelte`, the output JS should be at `http://.../assets/svelte/components/some-component.js`.
It should also work with `@routes.Assets.versioned("svelte/components/some-component.js")`.

The exported module name is the camel case of the file name. In the above example, the module name is `SomeComponent`.

Therefore, we can use the component as shown below:

```
<script src='@routes.Assets.versioned("svelte/components/some-component.js")'></script>
<link rel="stylesheet" href='@routes.Assets.versioned("svelte/components/some-component.css")'>

<div id="app"></div>
<script>
  new SomeComponent.default({
    target: document.getElementById('app'),
    props: {
      someProp: 'prop'
    }
  });
</script>
```

Please see the folder `test-play-project` for a complete example.

Use the Hot Module Reload (HMR)
--------------------------------

The setup for HMR is complex but completely worth it because it auto-reloads the JS code changes without reloading the page or triggering the recompilation of Play Framework. 
It's 10x faster for development. If you have issues with setting it up, please open an issue.

### 1. Make hmr.js and set up the command.

Please copy `test-play-project/hmr.js` to your project and set up the hmr command in `package.json` as shown below:

```
  "scripts": {
    "hmr": "NODE_PATH=./node_modules ENABLE_HMR=true node hmr.js"
  },
```

### 2. Configure the webpack config

Detect `ENABLE_HMR` and reconfigure the `svelte-loader` as shown below:

```
if (process.env.ENABLE_HMR) {
  console.log('Enable HMR')
  for (const rule of config.module.rules) {
    if (rule.use.loader === 'svelte-loader') {
      rule.use.options.emitCss = false
      rule.use.options.compilerOptions.dev = true
      rule.use.options.hotReload = true
    }
  }
  config.plugins.push(new webpack.HotModuleReplacementPlugin())
}
```

### 3. Configure the Play Framework to redirect assets to the HMR server

In `hmr.js`, the HMR server will listen to the port 9001. We will need to redirect the assets to the HMR server by making the Assets controller as follows:

```
@Singleton
class AssetsController @Inject()(
  errorHandler: HttpErrorHandler,
  meta: AssetsMetadata,
  env: Environment
)(implicit ec: ExecutionContext)
    extends Assets(errorHandler, meta, env) {

  override def versioned(path: String, file: Assets.Asset): Action[AnyContent] = Action.async { req =>
    if (
      env.mode == Mode.Dev &&
        (
          file.name.startsWith("svelte_") ||
            file.name.startsWith("svelte/") ||
            file.name.startsWith("stylesheets/tailwindbase.css")
          )
    ) {
      Future(Redirect(s"http://localhost:9001/assets/${file.name}"))
    } else {
      super.versioned(path, file)(req)
    }
  }
}
```

Then, you configure `conf/routes` as follows:

```
GET     /assets/*file               controllers.AssetsController.versioned(path="/public", file: Asset)
```

### 4. Try it out

Now, you can run `sbt run` in one terminal and `npm run hmr` in another terminal.

Go to `http://localhost:9000` and you should see the page. Now, you can make changes to the svelte components and see the changes immediately.

See a working example in the `test-play-project` folder.

Interested in using the plugin?
--------------------------------

Please feel free to open an issue to ask questions. Let us know how you want to use the plugin. We want to help you use the plugin successfully.


Contributing
---------------

The project welcomes any contribution. Here are the steps for testing when developing locally:

1. Run `npm install` in order to install packages needed for the integration tests.
2. Run `sbt test` to run all tests.
3. To test the plugin on an actual Playframework project, go to `test-play-project`, run `npm install`, and run `sbt run`.

Publish
--------
We are not publishing a jar file anymore. You can use it by referencing a github URL with a specific commit directly.
