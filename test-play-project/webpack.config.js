"use strict";

const webpack = require('webpack');
const sveltePreprocess = require("svelte-preprocess");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const pathModule = require("path");
const glob = require('glob');

let entry = {}
for (const relativePath of glob.globSync('./app/assets/svelte/**/*.svelte')) {
  if (pathModule.basename(relativePath).startsWith('_')) {
    continue
  }

  const key = relativePath.substring('app/assets/'.length, relativePath.length - '.svelte'.length)

  entry[key] = [
    "webpack-hot-middleware/client?path=http://localhost:9001/__webpack_hmr&timeout=5000",
    "./public/stylesheets/tailwindbase.css",
    `./${relativePath}`
  ]
}

const replacePathVariables = (path, data) => {
  const REGEXP_CAMEL_CASE_NAME = /\[camel-case-name\]/gi;
  if (typeof path === "function") {
    path = path(data);
  }

  if (data && data.chunk && data.chunk.name) {
    let tokens = data.chunk.name.split(pathModule.sep);
    return path.replace(
      REGEXP_CAMEL_CASE_NAME,
      tokens[tokens.length - 1]
        .replace(/(\-\w)/g, (matches) => { return matches[1].toUpperCase(); })
        .replace(/(^\w)/, (matches) => { return matches[0].toUpperCase(); })
    );
  } else {
    return path;
  }
};

class CamelCaseNamePlugin {
  apply(compiler) {
    compiler.hooks.compilation.tap("sbt-js-compilation", (compilation) => {
      compilation.hooks.assetPath.tap('sbt-js-asset-path', replacePathVariables);
    });
  }
}


const config = {
  mode: 'development',
  cache: true,
  stats: 'minimal',
  entry,
  resolve: {
    extensions: ['.mjs', '.js', '.svelte'],
    mainFields: ['svelte', 'browser', 'module', 'main'],
    conditionNames: ['svelte', 'browser']
  },
  module: {
    rules: [
      {
        test: /\.svelte(\.ts)?$/,
        use: {
          loader: 'svelte-loader',
          options: {
            emitCss: true,
            preprocess: sveltePreprocess({}),
            compilerOptions: {
              dev: false,
              compatibility: {
                componentApi: 4
              }
            },
            hotReload: false
          }
        }
      },
      {
        test: /\.css$/,
        exclude: /node_modules/,
        use: [
          MiniCssExtractPlugin.loader,
          {
            loader: 'css-loader',
            options: {
              importLoaders: 1
            }
          },
          'postcss-loader'
        ],
      },
    ]
  },
  plugins: [
    new MiniCssExtractPlugin(),
    new CamelCaseNamePlugin()
  ],
  output: {
    publicPath: '/assets/',
    library: '[camel-case-name]',
    filename: '[name].js',
  },
  performance: {
    hints: 'error',
    maxAssetSize: 2000000,
    maxEntrypointSize: 2000000,
    assetFilter: function(assetFilename) {
      return assetFilename.endsWith('.js');
    }
  },
  devtool: 'eval-cheap-source-map',
};

module.exports = (env, argv) => {
  if (argv.mode === 'production') {
    console.log('Webpack for production');
    config.devtool = false;
    config.performance.maxAssetSize = 250000;
    config.performance.maxEntrypointSize = 250000;
    config.optimization = (config.optimization || {});
  } else if (argv.mode === 'development') {
    console.log('Webpack for development')

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
  } else if (argv.mode === 'none') {

  } else {
    throw new Error('argv.mode must be either development, none, or production.')
  }

  return config;
};
