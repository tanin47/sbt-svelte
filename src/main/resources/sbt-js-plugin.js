"use strict";

const pathModule = require('path')

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

const writeStats = (compilation, assets) => {
  const ms = [];
  for (let module of compilation.getStats().toJson().modules) {
    let reasons = [];
    for (let reason of module.reasons) {
      reasons.push(reason.moduleName);
    }
    ms.push({
      name: module.name,
      reasons: reasons
    })
  }

  const s = JSON.stringify(ms);
  assets['sbt-js-tree.json'] = {
    source() {
      return s;
    },
    size() {
      return s.length;
    }
  };
};

class SbtJsPlugin {
  apply(compiler) {
    compiler.hooks.compilation.tap("sbt-js-compilation", (compilation) => {
      compilation.hooks.assetPath.tap('sbt-js-asset-path', replacePathVariables);
      compilation.hooks.processAssets.tap(
        {
          name: "sbt-js-emit",
          stage: compiler.webpack.Compilation.PROCESS_ASSETS_STAGE_REPORT
        },
        (assets) => {
          writeStats(compilation, assets);
        });
    });
  }
}

module.exports = SbtJsPlugin;
