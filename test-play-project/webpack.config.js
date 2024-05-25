"use strict";

const sveltePreprocess = require("svelte-preprocess");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require("path");

const config = {
  mode: 'development',
  cache: true,
  bail: true,
  stats: 'minimal',
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
          'css-loader'
        ],
      },
    ]
  },
  plugins: [
    new MiniCssExtractPlugin(),
  ],
  performance: {
    hints: 'error',
    maxAssetSize: 1500000,
    maxEntrypointSize: 1500000,
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
  } else {
    console.log('Webpack for development')
  }

  return config;
};
