# ChatGPT Style Diagram Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Halo 文章和单页前台使用独立 TypeScript/CSS 运行时，将 Mermaid 与 PlantUML 渲染为具备完整工具栏、自动主题和可配置窄屏行为的 ChatGPT 风格卡片。

**Architecture:** Java 内容处理器只读取设置、安全序列化页面配置并加载运行时资源；独立 Rsbuild 入口产出 `text-diagram-runtime.js` 和 `text-diagram-runtime.css`。前台运行时以统一 Card Shell 包装节点，通过 Mermaid/PlantUML 适配器生成内容，并以幂等扫描支持首次加载和 PJAX。

**Tech Stack:** Java 21、Halo Plugin API 2.25、Reactor、JUnit 5、Vue/TypeScript 4.7、Rsbuild、Vitest、jsdom、Mermaid 11、CSS。

---

## 2026-07-11 输出格式与本地 PlantUML 渲染增量计划

当前分支已完成独立前台运行时的主体实现。本增量计划替换旧的 PlantUML `data-src` 图片模式，并给 Mermaid 与 PlantUML 分别增加 `svg`、`png`、`webp` 输出格式配置。

### 增量文件结构

- Modify: `src/main/java/run/halo/plugin/textdiagram/BasicConfig.java`，增加 `mermaid_output_format`、`plantuml_output_format` 默认值和合法值回退。
- Modify: `src/main/resources/extensions/settings.yaml`，增加 Mermaid 与 PlantUML 输出格式下拉配置。
- Modify: `src/main/java/run/halo/plugin/textdiagram/JSInjector.java`，把两个输出格式注入 `data-config`。
- Modify: `src/main/java/run/halo/plugin/textdiagram/DefaultPostContentHandler.java`、`DefaultSinglePageContentHandler.java`，传递新增配置。
- Modify: `console/package.json`、`console/pnpm-lock.yaml`，增加 `@plantuml/core`。
- Modify: `console/src/editor/text-diagram/index.ts`、`TextDiagramView.vue`，新 PlantUML 内容不再写入 `data-src`；旧内容仍可解析。
- Modify: `console/src/runtime/text-diagram/types.ts`，增加 `DiagramOutputFormat` 和两类格式配置。
- Create: `console/src/runtime/text-diagram/image-exporter.ts`，把 SVG 按配置输出为 SVG、PNG 或 WebP。
- Modify: `console/src/runtime/text-diagram/mermaid-adapter.ts`，返回 SVG 基础结果并按 Mermaid 配置导出最终格式。
- Modify: `console/src/runtime/text-diagram/plantuml-adapter.ts`，懒加载 `@plantuml/core` 本地渲染 SVG，并按 PlantUML 配置导出最终格式，旧 `data-src` 仅兜底。
- Modify: `console/src/runtime/text-diagram/index.ts`，解析新增配置并传入适配器。
- Test: 更新 `BasicConfigTest`、`JSInjectorTest`、运行时 adapter/index 测试，覆盖默认值、非法值、格式输出、旧 `data-src` 兼容和错误隔离。

### 增量任务

- [ ] **Task A: 后台配置和注入**

增加两个配置字段，默认 `svg`，只允许 `svg|png|webp`。Java 注入 JSON 中新增 `mermaidOutputFormat`、`plantumlOutputFormat`，文章与单页处理器保持成对修改。

Run: `.\gradlew.bat test --tests run.halo.plugin.textdiagram.BasicConfigTest --tests run.halo.plugin.textdiagram.JSInjectorTest --tests run.halo.plugin.textdiagram.DefaultPostContentHandlerTest --tests run.halo.plugin.textdiagram.DefaultSinglePageContentHandlerTest`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Task B: 前端依赖和编辑器存储**

安装 `@plantuml/core`。编辑器 PlantUML 预览可继续生成临时图片，但不得把临时图片 URL 写入节点属性；`renderHTML` 对新 PlantUML 节点输出源码和 `data-content`，不再输出 `data-src` 或子级 `<img>`。保留 `parseHTML` 读取旧 `data-src`。

Run: `pnpm --dir console install`

Expected: `console/pnpm-lock.yaml` 同步更新。

- [ ] **Task C: 统一 SVG 导出器**

实现 `image-exporter.ts`，输入 SVG 字符串、输出格式和文件前缀；`svg` 直接返回 SVG 元素，`png/webp` 通过 `Image` + Canvas 生成 Blob URL 与 `<img>`。转换失败抛出明确错误。

Run: `pnpm --dir console run test:runtime -- image-exporter.test.ts`

Expected: `svg/png/webp` 单元测试 PASS。

- [ ] **Task D: Mermaid 与 PlantUML 适配器**

Mermaid 继续用浏览器 Mermaid 生成 SVG，再调用导出器。PlantUML 改为懒加载 `@plantuml/core` 的 `renderToString`，在浏览器内生成 SVG，再调用导出器；旧 `data-src` 只在源码为空时作为图片兜底。

Run: `pnpm --dir console run test:runtime -- mermaid-adapter.test.ts plantuml-adapter.test.ts`

Expected: 两个适配器测试 PASS。

- [ ] **Task E: 运行时配置、完整构建和真实页面验证**

运行时解析新增格式配置，非法值回退 `svg`。执行运行时测试、类型检查、前端构建、Gradle 构建，并在 Halo 文章页确认两个图表按最新后台配置渲染。

Run: `pnpm --dir console run test:runtime`

Run: `pnpm --dir console run type-check`

Run: `pnpm --dir console run build`

Run: `.\gradlew.bat build`

Expected: 全部成功。

## 文件结构

### 新建

- `console/rsbuild.runtime.config.mjs`：独立前台运行时构建入口。
- `console/vitest.config.ts`：jsdom 单元测试配置。
- `console/src/runtime/text-diagram/types.ts`：配置、适配器和渲染结果接口。
- `console/src/runtime/text-diagram/icons.ts`：ChatGPT 风格内联 SVG 图标。
- `console/src/runtime/text-diagram/theme.ts`：明暗模式检测与监听。
- `console/src/runtime/text-diagram/responsive.ts`：三种窄屏策略。
- `console/src/runtime/text-diagram/card.ts`：统一卡片结构和交互。
- `console/src/runtime/text-diagram/mermaid-adapter.ts`：Mermaid 加载与 SVG 渲染。
- `console/src/runtime/text-diagram/plantuml-adapter.ts`：PlantUML 图片渲染和下载元数据。
- `console/src/runtime/text-diagram/index.ts`：首次加载、PJAX 和幂等调度入口。
- `console/src/runtime/text-diagram/style.css`：卡片、主题、全屏及响应式样式。
- `console/src/runtime/text-diagram/*.test.ts`：运行时模块单元测试。

### 修改

- `console/package.json`、`console/pnpm-lock.yaml`：增加运行时构建、Vitest 和 jsdom。
- `src/main/java/run/halo/plugin/textdiagram/BasicConfig.java`：增加 `mobile_layout_mode` 及默认值。
- `src/main/resources/extensions/settings.yaml`：增加三选一后台设置。
- `src/main/java/run/halo/plugin/textdiagram/JSInjector.java`：缩减为配置和资源注入器。
- `src/main/java/run/halo/plugin/textdiagram/DefaultPostContentHandler.java`：使用默认配置并注入运行时。
- `src/main/java/run/halo/plugin/textdiagram/DefaultSinglePageContentHandler.java`：与文章处理器保持一致。
- `src/test/java/run/halo/plugin/textdiagram/BasicConfigTest.java`：覆盖新增默认值。
- `src/test/java/run/halo/plugin/textdiagram/JSInjectorTest.java`：验证安全配置和资源引用。
- `src/test/java/run/halo/plugin/textdiagram/UiBundleLocationTest.java`：验证运行时产物路径。

## Task 1：完成后台默认配置和窄屏设置

**Files:**
- Modify: `src/main/java/run/halo/plugin/textdiagram/BasicConfig.java`
- Modify: `src/main/resources/extensions/settings.yaml`
- Test: `src/test/java/run/halo/plugin/textdiagram/BasicConfigTest.java`

- [ ] **Step 1: 在现有测试中加入窄屏默认值失败用例**

```java
@Test
void shouldUseScrollAsDefaultMobileLayoutMode() {
    BasicConfig defaults = BasicConfig.defaults();
    assertEquals("scroll", defaults.getMobile_layout_mode());
}

@Test
void shouldReplaceBlankMobileLayoutMode() {
    BasicConfig config = new BasicConfig();
    config.setMobile_layout_mode("  ");
    assertEquals("scroll", BasicConfig.withDefaults(config).getMobile_layout_mode());
}
```

- [ ] **Step 2: 运行测试并确认因字段不存在而失败**

Run: `./gradlew.bat test --tests run.halo.plugin.textdiagram.BasicConfigTest`

Expected: 编译失败，提示 `getMobile_layout_mode` 或 `setMobile_layout_mode` 不存在。

- [ ] **Step 3: 加入确定的默认值与表单选项**

在 `BasicConfig` 中加入：

```java
static final String DEFAULT_MOBILE_LAYOUT_MODE = "scroll";
String mobile_layout_mode;
```

在 `withDefaults` 返回前加入：

```java
result.setMobile_layout_mode(
    hasText(config == null ? null : config.getMobile_layout_mode())
        ? config.getMobile_layout_mode()
        : DEFAULT_MOBILE_LAYOUT_MODE
);
```

在 `settings.yaml` 的 `basic.formSchema` 末尾加入：

```yaml
- $formkit: select
  name: mobile_layout_mode
  label: 移动端宽图显示方式
  value: scroll
  options:
    - label: 横向滚动
      value: scroll
    - label: 自动缩放
      value: scale
    - label: 缩略图，点击全屏
      value: thumbnail
  help: 仅在屏幕宽度小于 768px 时生效
```

- [ ] **Step 4: 运行配置测试**

Run: `./gradlew.bat test --tests run.halo.plugin.textdiagram.BasicConfigTest`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: 仅提交本任务文件**

```powershell
git add -- src/main/java/run/halo/plugin/textdiagram/BasicConfig.java src/main/resources/extensions/settings.yaml src/test/java/run/halo/plugin/textdiagram/BasicConfigTest.java
git commit -m "feat: add mobile diagram layout setting"
```

## Task 2：建立独立运行时构建和测试入口

**Files:**
- Create: `console/rsbuild.runtime.config.mjs`
- Create: `console/vitest.config.ts`
- Create: `console/src/runtime/text-diagram/index.ts`
- Modify: `console/package.json`
- Modify: `console/pnpm-lock.yaml`
- Test: `src/test/java/run/halo/plugin/textdiagram/UiBundleLocationTest.java`

- [ ] **Step 1: 增加运行时产物位置测试**

```java
@Test
void runtimeAssetsShouldUseStaticBundleLocation() {
    assertTrue(Files.exists(Path.of("src/main/resources/static/text-diagram-runtime.js")));
    assertTrue(Files.exists(Path.of("src/main/resources/static/text-diagram-runtime.css")));
}
```

- [ ] **Step 2: 创建最小运行时入口和独立 Rsbuild 配置**

`console/src/runtime/text-diagram/index.ts`：

```ts
import "./style.css";

export function startTextDiagramRuntime(): void {
  document.documentElement.dataset.textDiagramRuntime = "ready";
}

startTextDiagramRuntime();
```

`console/src/runtime/text-diagram/style.css`：

```css
:root {
  --text-diagram-runtime-ready: 1;
}
```

`console/rsbuild.runtime.config.mjs`：

```js
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
    },
  },
});
```

- [ ] **Step 3: 配置 Vitest 与构建脚本**

`console/vitest.config.ts`：

```ts
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "jsdom",
    include: ["src/runtime/**/*.test.ts"],
    restoreMocks: true,
  },
});
```

把 `console/package.json` 脚本改为：

```json
"build": "rsbuild build && rsbuild build -c rsbuild.runtime.config.mjs",
"test:runtime": "vitest run"
```

并加入开发依赖：

```json
"jsdom": "^26.1.0",
"vitest": "^3.2.4"
```

- [ ] **Step 4: 安装依赖并验证两个构建入口**

Run: `pnpm --dir console install`

Expected: `console/pnpm-lock.yaml` 同步更新且安装成功。

Run: `pnpm --dir console run build`

Expected: UI bundle 构建成功，且 `src/main/resources/static/` 出现两个运行时文件。

- [ ] **Step 5: 运行资源位置测试**

Run: `./gradlew.bat test --tests run.halo.plugin.textdiagram.UiBundleLocationTest`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 6: 提交构建入口**

```powershell
git add -- console/package.json console/pnpm-lock.yaml console/rsbuild.runtime.config.mjs console/vitest.config.ts console/src/runtime/text-diagram/index.ts console/src/runtime/text-diagram/style.css src/test/java/run/halo/plugin/textdiagram/UiBundleLocationTest.java
git commit -m "build: add frontend diagram runtime bundle"
```

## Task 3：实现配置、主题和响应式基础模块

**Files:**
- Create: `console/src/runtime/text-diagram/types.ts`
- Create: `console/src/runtime/text-diagram/theme.ts`
- Create: `console/src/runtime/text-diagram/responsive.ts`
- Test: `console/src/runtime/text-diagram/theme.test.ts`
- Test: `console/src/runtime/text-diagram/responsive.test.ts`

- [ ] **Step 1: 写主题和响应式失败测试**

```ts
import { describe, expect, it } from "vitest";
import { detectTheme } from "./theme";
import { applyMobileLayout } from "./responsive";

describe("runtime presentation", () => {
  it("detects a configured dark selector", () => {
    document.documentElement.className = "dark";
    expect(detectTheme("html[class~=dark]")).toBe("dark");
  });

  it("uses scroll below 768px", () => {
    const card = document.createElement("section");
    applyMobileLayout(card, "scroll", 767);
    expect(card.dataset.textDiagramMobileLayout).toBe("scroll");
  });

  it("disables the mobile strategy at 768px", () => {
    const card = document.createElement("section");
    applyMobileLayout(card, "thumbnail", 768);
    expect(card.dataset.textDiagramMobileLayout).toBe("desktop");
  });
});
```

- [ ] **Step 2: 运行并确认模块不存在**

Run: `pnpm --dir console run test:runtime`

Expected: FAIL，提示无法解析 `theme` 或 `responsive`。

- [ ] **Step 3: 实现公共类型和纯函数**

`types.ts`：

```ts
export type DiagramType = "mermaid" | "plantuml";
export type MobileLayoutMode = "scroll" | "scale" | "thumbnail";
export type DiagramTheme = "light" | "dark";

export interface RuntimeConfig {
  darkClassSelector: string;
  mermaidSelector: string;
  mobileLayoutMode: MobileLayoutMode;
}

export interface RenderResult {
  element: HTMLElement | SVGElement;
  filename: string;
  mimeType: string;
  downloadUrl?: string;
}

export interface DiagramAdapter {
  render(target: HTMLElement, source: string, theme: DiagramTheme): Promise<RenderResult>;
}
```

`theme.ts`：

```ts
import type { DiagramTheme } from "./types";

export function detectTheme(selector: string): DiagramTheme {
  try {
    return document.querySelector(selector) ? "dark" : "light";
  } catch {
    return "light";
  }
}

export function observeTheme(selector: string, callback: (theme: DiagramTheme) => void): MutationObserver {
  const observer = new MutationObserver(() => callback(detectTheme(selector)));
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ["class", "data-theme"] });
  return observer;
}
```

`responsive.ts`：

```ts
import type { MobileLayoutMode } from "./types";

export const MOBILE_BREAKPOINT = 768;

export function applyMobileLayout(card: HTMLElement, mode: MobileLayoutMode, width = window.innerWidth): void {
  card.dataset.textDiagramMobileLayout = width < MOBILE_BREAKPOINT ? mode : "desktop";
}
```

- [ ] **Step 4: 运行运行时测试和类型检查**

Run: `pnpm --dir console run test:runtime`

Expected: 所有测试 PASS。

Run: `pnpm --dir console run type-check`

Expected: 无 TypeScript 错误。

- [ ] **Step 5: 提交基础模块**

```powershell
git add -- console/src/runtime/text-diagram/types.ts console/src/runtime/text-diagram/theme.ts console/src/runtime/text-diagram/responsive.ts console/src/runtime/text-diagram/theme.test.ts console/src/runtime/text-diagram/responsive.test.ts
git commit -m "feat: add diagram runtime presentation config"
```

## Task 4：实现统一卡片和 ChatGPT 风格图标

**Files:**
- Create: `console/src/runtime/text-diagram/icons.ts`
- Create: `console/src/runtime/text-diagram/card.ts`
- Expand: `console/src/runtime/text-diagram/style.css`
- Test: `console/src/runtime/text-diagram/card.test.ts`

- [ ] **Step 1: 写卡片交互失败测试**

```ts
import { describe, expect, it, vi } from "vitest";
import { createDiagramCard } from "./card";

it("switches views, copies source, and exits fullscreen with Escape", async () => {
  const writeText = vi.fn().mockResolvedValue(undefined);
  Object.assign(navigator, { clipboard: { writeText } });
  const host = document.createElement("text-diagram");
  const card = createDiagramCard(host, "mermaid", "graph TD;A-->B");

  card.querySelector<HTMLButtonElement>("[data-action='source']")!.click();
  expect(card.dataset.textDiagramView).toBe("source");
  card.querySelector<HTMLButtonElement>("[data-action='copy']")!.click();
  await Promise.resolve();
  expect(writeText).toHaveBeenCalledWith("graph TD;A-->B");
  card.querySelector<HTMLButtonElement>("[data-action='fullscreen']")!.click();
  document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
  expect(card.dataset.textDiagramFullscreen).toBe("false");
});
```

- [ ] **Step 2: 运行并确认 `createDiagramCard` 不存在**

Run: `pnpm --dir console run test:runtime -- card.test.ts`

Expected: FAIL，提示无法解析 `./card`。

- [ ] **Step 3: 实现图标和卡片公开接口**

`icons.ts` 导出固定 SVG 字符串，禁止使用字体字符：

```ts
export const icons = {
  source: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m8 9-3 3 3 3m8-6 3 3-3 3M14 5l-4 14"/></svg>`,
  preview: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m8.5 6.2 9.65 5.8-9.65 5.8z"/></svg>`,
  fullscreen: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 3H3v5m13-5h5v5M8 21H3v-5m13 5h5v-5"/></svg>`,
  copy: `<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="8" y="8" width="11" height="11" rx="2"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/></svg>`,
  download: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12m-4.5-4 4.5 4.5 4.5-4.5M5 21h14"/></svg>`,
  check: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>`,
};
```

`card.ts` 必须导出：

```ts
export interface DiagramCard extends HTMLElement {
  setPreview(element: HTMLElement | SVGElement): void;
  setError(message: string): void;
  setDownload(url: string, filename: string): void;
  destroy(): void;
}

export function createDiagramCard(host: HTMLElement, type: DiagramType, source: string): DiagramCard;
```

实现时使用 `textContent` 写源码和错误；按钮使用 `data-action`、`title`、`aria-label`，并在卡片根节点维护 `data-text-diagram-view` 和 `data-text-diagram-fullscreen`。剪贴板失败时切换源码视图并用 `Range` 选中 `<code>` 内容。

- [ ] **Step 4: 完成样式**

在 `style.css` 中使用 `[data-text-diagram-card]` 作用域，定义浅色/深色 CSS 变量、16px 卡片圆角、工具栏、40px 图标按钮、18px SVG、画布滚动、源码区、错误区、全屏覆盖和 `:focus-visible`。为 `data-text-diagram-mobile-layout="scroll|scale|thumbnail"` 分别定义滚动、`transform: scale()` 容器规则和可点击缩略图；不得污染主题全局元素。

- [ ] **Step 5: 运行测试、类型检查和构建**

Run: `pnpm --dir console run test:runtime -- card.test.ts`

Expected: PASS。

Run: `pnpm --dir console run type-check && pnpm --dir console run build`

Expected: 两条命令成功。

- [ ] **Step 6: 提交卡片层**

```powershell
git add -- console/src/runtime/text-diagram/icons.ts console/src/runtime/text-diagram/card.ts console/src/runtime/text-diagram/card.test.ts console/src/runtime/text-diagram/style.css
git commit -m "feat: add ChatGPT style diagram card"
```

## Task 5：实现 Mermaid 与 PlantUML 适配器

**Files:**
- Create: `console/src/runtime/text-diagram/mermaid-adapter.ts`
- Create: `console/src/runtime/text-diagram/plantuml-adapter.ts`
- Test: `console/src/runtime/text-diagram/mermaid-adapter.test.ts`
- Test: `console/src/runtime/text-diagram/plantuml-adapter.test.ts`

- [ ] **Step 1: 写适配器契约失败测试**

```ts
it("renders Mermaid SVG with html labels disabled", async () => {
  const run = vi.fn(async ({ nodes }) => { nodes[0].innerHTML = "<svg></svg>"; });
  const initialize = vi.fn();
  const adapter = createMermaidAdapter(async () => ({ initialize, run }));
  const target = document.createElement("div");
  const result = await adapter.render(target, "graph TD;A-->B", "light");
  expect(initialize).toHaveBeenCalledWith(expect.objectContaining({ startOnLoad: false, htmlLabels: false }));
  expect(result.mimeType).toBe("image/svg+xml");
});

it("uses the existing PlantUML image", async () => {
  const target = document.createElement("text-diagram");
  target.dataset.src = "https://example.test/diagram.png";
  const pending = createPlantUmlAdapter().render(target, "@startuml\nA->B\n@enduml", "light");
  target.querySelector<HTMLImageElement>("img")!.dispatchEvent(new Event("load"));
  expect((await pending).filename).toBe("text-diagram.png");
});
```

- [ ] **Step 2: 运行并确认适配器模块不存在**

Run: `pnpm --dir console run test:runtime -- adapter.test.ts`

Expected: FAIL，提示无法解析适配器模块。

- [ ] **Step 3: 实现 Mermaid 适配器**

`createMermaidAdapter(loader)` 接受可注入 loader 以便测试；生产 loader 动态加载 `/plugins/text-diagram/assets/static/mermaid.min.js` 并以共享 Promise 去重。每次主题改变时调用 `initialize`：

```ts
mermaid.initialize({
  startOnLoad: false,
  htmlLabels: false,
  theme: theme === "dark" ? "dark" : "default",
  flowchart: { htmlLabels: false, useMaxWidth: false },
});
```

等待 `document.fonts?.ready` 后在隔离目标中执行 `mermaid.run({ nodes: [renderTarget] })`，取出 SVG 并返回 `filename: "text-diagram.svg"` 与 `mimeType: "image/svg+xml"`。加载失败时清空共享 Promise，允许下一次扫描重试。

- [ ] **Step 4: 实现 PlantUML 适配器**

读取 `target.dataset.src`，创建新的 `<img>`，以 `load`/`error` Promise 返回结果。根据 URL 后缀只允许 `svg|png|webp`，其他情况使用 `png`；返回同源或原始 URL 作为 `downloadUrl`。不得重新请求或改变 PlantUML 编码方式。

- [ ] **Step 5: 运行适配器测试与类型检查**

Run: `pnpm --dir console run test:runtime -- adapter.test.ts`

Expected: Mermaid 与 PlantUML 测试 PASS。

Run: `pnpm --dir console run type-check`

Expected: 无类型错误。

- [ ] **Step 6: 提交适配器**

```powershell
git add -- console/src/runtime/text-diagram/mermaid-adapter.ts console/src/runtime/text-diagram/plantuml-adapter.ts console/src/runtime/text-diagram/mermaid-adapter.test.ts console/src/runtime/text-diagram/plantuml-adapter.test.ts
git commit -m "feat: add Mermaid and PlantUML runtime adapters"
```

## Task 6：实现幂等扫描、PJAX 和主题重渲染

**Files:**
- Modify: `console/src/runtime/text-diagram/index.ts`
- Test: `console/src/runtime/text-diagram/index.test.ts`

- [ ] **Step 1: 写首次扫描和 PJAX 去重失败测试**

```ts
it("renders new PJAX nodes without wrapping old nodes twice", async () => {
  document.body.innerHTML = `<text-diagram data-type="mermaid" data-content="graph TD;A-->B">graph TD;A-->B</text-diagram>`;
  const runtime = createRuntime(testConfig, adapters);
  await runtime.scan();
  const firstCard = document.querySelector("[data-text-diagram-card]");
  document.dispatchEvent(new Event("pjax:success"));
  await runtime.whenIdle();
  expect(document.querySelectorAll("[data-text-diagram-card]")).toHaveLength(1);
  expect(document.querySelector("[data-text-diagram-card]")).toBe(firstCard);
});
```

- [ ] **Step 2: 运行并确认 `createRuntime` 不存在**

Run: `pnpm --dir console run test:runtime -- index.test.ts`

Expected: FAIL。

- [ ] **Step 3: 实现运行时公开接口**

```ts
export interface TextDiagramRuntime {
  scan(): Promise<void>;
  whenIdle(): Promise<void>;
  destroy(): void;
}

export function createRuntime(
  config: RuntimeConfig,
  adapters: Record<DiagramType, DiagramAdapter>,
): TextDiagramRuntime;
```

`scan` 选择 `text-diagram[data-type]` 与配置的 `mermaidSelector`，过滤 `data-text-diagram-state` 已为 `loading|ready|error` 的节点，按共享 Promise 链依次渲染。源码优先读取 `data-content`，不存在时读取 `textContent`。每个节点单独捕获错误并调用 `card.setError`，不得中断后续节点。

入口从 `script[data-halo-text-diagram-runtime]` 的 `data-config` 读取 JSON，校验 `mobileLayoutMode` 只接受 `scroll|scale|thumbnail`，否则回退 `scroll`。绑定一次 `pjax:success`、一次 `resize` 和一个主题观察器；`destroy` 移除监听并销毁卡片。

- [ ] **Step 4: 覆盖错误隔离、未知类型和主题变化**

在 `index.test.ts` 增加三个独立测试：第一张适配器 reject 时第二张仍进入 `ready`；未知 `data-type` 保持原 DOM；主题变化只重新调用 Mermaid 适配器而不重复创建卡片。

- [ ] **Step 5: 运行全部运行时测试**

Run: `pnpm --dir console run test:runtime`

Expected: 全部 PASS。

Run: `pnpm --dir console run type-check`

Expected: 无类型错误。

- [ ] **Step 6: 提交调度入口**

```powershell
git add -- console/src/runtime/text-diagram/index.ts console/src/runtime/text-diagram/index.test.ts
git commit -m "feat: orchestrate frontend diagram rendering"
```

## Task 7：把 Java 注入器迁移为资源与配置加载器

**Files:**
- Modify: `src/main/java/run/halo/plugin/textdiagram/JSInjector.java`
- Modify: `src/main/java/run/halo/plugin/textdiagram/DefaultPostContentHandler.java`
- Modify: `src/main/java/run/halo/plugin/textdiagram/DefaultSinglePageContentHandler.java`
- Test: `src/test/java/run/halo/plugin/textdiagram/JSInjectorTest.java`
- Test: `src/test/java/run/halo/plugin/textdiagram/DefaultPostContentHandlerTest.java`
- Test: `src/test/java/run/halo/plugin/textdiagram/DefaultSinglePageContentHandlerTest.java`

- [ ] **Step 1: 将注入测试改为外部资源契约**

```java
@Test
void shouldInjectRuntimeAssetsAndEscapedConfiguration() {
    String html = JSInjector.getRuntimeInjection(
        "html[class~=dark]",
        "text-diagram[data-type=mermaid]",
        "scroll"
    );
    assertTrue(html.contains("/plugins/text-diagram/assets/static/text-diagram-runtime.css"));
    assertTrue(html.contains("/plugins/text-diagram/assets/static/text-diagram-runtime.js"));
    assertTrue(html.contains("data-halo-text-diagram-runtime"));
    assertTrue(html.contains("&quot;mobileLayoutMode&quot;:&quot;scroll&quot;"));
    assertFalse(html.contains("function createCardShell"));
}
```

保留恶意选择器测试，断言 `</script>` 被 HTML 属性编码，无法结束 `<script>` 标签。

- [ ] **Step 2: 运行并确认新方法不存在**

Run: `./gradlew.bat test --tests run.halo.plugin.textdiagram.JSInjectorTest`

Expected: 编译失败，提示 `getRuntimeInjection` 不存在。

- [ ] **Step 3: 用小型注入器替换 Java 大文本块**

`JSInjector` 只保留常量、安全 JSON 字符串编码和 HTML 属性编码，输出：

```html
<link rel="stylesheet" href="/plugins/text-diagram/assets/static/text-diagram-runtime.css" data-halo-text-diagram-runtime-style>
<script defer src="/plugins/text-diagram/assets/static/text-diagram-runtime.js" data-halo-text-diagram-runtime data-config="{HTML_ATTRIBUTE_ENCODED_JSON}"></script>
```

JSON 键固定为 `darkClassSelector`、`mermaidSelector`、`mobileLayoutMode`。复用已验证的 `toJavaScriptString` 逻辑生成 JSON 字符串值，再对 `& < > " '` 做 HTML 属性编码；不要直接拼接原始配置。

- [ ] **Step 4: 同步文章与单页处理器**

两处 `injectJS` 都改为调用：

```java
JSInjector.getRuntimeInjection(
    basicConfig.getDark_class_selector(),
    basicConfig.getMermaid_selector(),
    basicConfig.getMobile_layout_mode()
)
```

保留现有 `defaultIfEmpty(BasicConfig.defaults())`、`map(BasicConfig::withDefaults)` 和 `onErrorResume`，确保设置缺失仍注入、异常仍返回原正文。

- [ ] **Step 5: 运行全部 Java 测试**

Run: `./gradlew.bat test`

Expected: `BUILD SUCCESSFUL`；旧的内联脚本断言应被资源契约断言替换，不保留已经失效的实现细节测试。

- [ ] **Step 6: 提交后端迁移**

```powershell
git add -- src/main/java/run/halo/plugin/textdiagram/JSInjector.java src/main/java/run/halo/plugin/textdiagram/DefaultPostContentHandler.java src/main/java/run/halo/plugin/textdiagram/DefaultSinglePageContentHandler.java src/test/java/run/halo/plugin/textdiagram/JSInjectorTest.java src/test/java/run/halo/plugin/textdiagram/DefaultPostContentHandlerTest.java src/test/java/run/halo/plugin/textdiagram/DefaultSinglePageContentHandlerTest.java
git commit -m "refactor: load diagram frontend runtime assets"
```

## Task 8：完整验证和 Chrome 人工验收

**Files:**
- Modify only if verification finds a scoped defect in files from Tasks 1-7.

- [ ] **Step 1: 运行前端单元测试和类型检查**

Run: `pnpm --dir console run test:runtime`

Expected: 全部 Vitest 测试 PASS。

Run: `pnpm --dir console run type-check`

Expected: 无错误。

- [ ] **Step 2: 运行前端与完整 Gradle 构建**

Run: `pnpm --dir console run build`

Expected: UI bundle、`text-diagram-runtime.js`、`text-diagram-runtime.css` 和 Mermaid 静态资源生成成功。

Run: `./gradlew.bat build`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 在 Chrome 验证真实页面**

使用 README 的 development runtime，且 `fixedPluginPath` 只指向当前仓库。依次验证：

1. 合法 Mermaid 显示卡片和 SVG。
2. 非法 Mermaid 只使当前卡片报错，并可查看/复制源码。
3. PlantUML 正常图片、源码切换和下载。
4. PlantUML 图片失败时下载禁用。
5. 代码/预览、全屏、`Escape`、复制成功图标和真实下载。
6. 浅色/深色运行时切换。
7. 小于 `768px` 时分别选择 `scroll`、`scale`、`thumbnail`；默认值为 `scroll`。
8. 触发 `pjax:success` 后只渲染新增节点，旧卡片不重复包装。

- [ ] **Step 4: 检查差异和禁止提交内容**

Run: `git diff --check`

Expected: 无 whitespace error。

Run: `git status --short`

Expected: 不包含 `build/`、`workplace/`、`console/node_modules/`、`src/main/resources/ui/`、`src/main/resources/static/`、本地 Halo 配置、日志或密钥。若这些路径因构建出现，保持未暂存并依照现有 `.gitignore` 处理。

- [ ] **Step 5: 提交仅由验收发现的修复**

若步骤 1-4 没有产生源码修改，不创建空提交。若产生修复，明确列出并暂存相关源文件和测试后执行：

```powershell
git diff --cached --check
git diff --cached
git commit -m "fix: address diagram runtime verification findings"
```

## 交付检查

- Mermaid 与 PlantUML 共用统一卡片，但适配器保持独立。
- Java 不再包含完整前台 CSS/JS 文本块。
- 设置缺失时默认注入运行时，移动端默认 `scroll`。
- 配置值无法逃逸 `data-config` 属性。
- 首次加载、PJAX、主题切换、三种窄屏策略和单图失败隔离均有测试。
- Chrome 中验证真实复制、下载、全屏和响应式行为。
- 每次提交仅包含对应任务的明确文件，用户原有改动不得被宽泛暂存。
