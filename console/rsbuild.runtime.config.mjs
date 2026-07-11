import { defineConfig } from "@rsbuild/core";

export default defineConfig({
  source: {
    entry: {
      "text-diagram-runtime": "./src/runtime/text-diagram/index.ts",
    },
  },
  output: {
    cleanDistPath: false,
    filename: {
      js: "[name].js",
      css: "[name].css",
    },
    distPath: {
      root: "../src/main/resources/static",
      js: ".",
      css: ".",
    },
  },
});
