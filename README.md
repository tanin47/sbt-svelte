sbt-svelte
===========

sbt-svelte integrates Webpack + Svelte into Playframework's asset generation.

It eliminates the need to run a separate node process to hot-reload your Svelte code. Moreover, it enables you to mix between SPA and non-SPA pages in your application; it's your choice whether to use SPA or not.

It works with both `sbt run` (which hot-reloads the code changes) and `sbt stage`.

Please see the example project in the folder `test-play-project`.

Requirements
-------------

* __[Webpack 5.x](https://webpack.js.org/):__ you'll need to specify the webpack binary location and webpack's configuration localtion. This enables you to choose your own version of Webpack and your own Webpack's configuration. You can see an example in the folder `test-play-project`.
* __Playframework 2.8.x__
* __Scala >= 2.12.x and SBT 1.x:__ Because the artifact is only published this setting (See: https://search.maven.org/artifact/io.github.tanin47/sbt-svelte/0.1.0/jar). If you would like other combinations of Scala and SBT versions, please open an issue.

How to use
-----------

### 1. Install the plugin

Add the below line to `project/plugins.sbt`:

```
addSbtPlugin("io.github.tanin47" % "sbt-svelte" % "0.1.0")
```

The artifacts are published to Maven Central here: https://search.maven.org/artifact/io.github.tanin47/sbt-svelte

### 2. Configure Webpack config file.

Create `webpack.config.js. Below is a working minimal example:

```
"use strict";

const sveltePreprocess = require("svelte-preprocess");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require("path");

module.exports = {
  mode: 'development',
  resolve: {
    alias: {
      svelte: path.join(process.env.NODE_PATH, 'svelte/src/runtime')
    },
    extensions: ['.mjs', '.js', '.svelte'],
    mainFields: ['svelte', 'browser', 'module', 'main'],
    conditionNames: ['svelte', 'browser']
  },
  module: {
    rules: [
      {
        test: /\.svelte$/,
        use: {
          loader: 'svelte-loader',
          options: {
            emitCss: true,
            preprocess: sveltePreprocess({}),
          }
        }
      },
      {
        test: /\.css$/,
        exclude: /node_modules/,
        use: [
          MiniCssExtractPlugin.loader,
          'css-loader',
        ],
      },
    ]
  },
  plugins: [
    new MiniCssExtractPlugin(),
  ],
};
```

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
1. Get the latest master by running `git fetch`.
2. Run `sbt clean publishSigned`
3. Tag the current commit with the current version: `git tag -a v[VERSION] -m "Version v[VERSION]"` and `git push origin --tags`.
