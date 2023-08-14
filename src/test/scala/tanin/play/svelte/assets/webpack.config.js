"use strict";

const path = require("path");
const sveltePreprocess = require("svelte-preprocess");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

module.exports = {
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
  performance: {
    hints: 'error',
  },
  stats: 'minimal'
};
