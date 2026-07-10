# AGENTS.md

本文件适用于仓库根目录及其所有子目录。若子目录中存在更具体的 `AGENTS.md`，以距离目标文件最近的说明为准。

## 沟通与工作原则

- 使用中文沟通，所有新增或修改的文本文件统一使用 UTF-8 编码。
- Windows 环境优先使用 PowerShell 7（`pwsh`）和仓库自带的 `gradlew.bat`；只有遇到兼容性问题时才回退到 Windows PowerShell 5。
- 修改前先执行 `git status --short`，保留并绕开用户已有改动。不要擅自还原、覆盖或提交无关文件。
- 优先做范围最小、原因明确的修改，不顺手重构无关代码，不批量格式化未涉及的文件。
- 不提交运行时文件、构建产物、临时文件、日志、IDE 配置、本地 Halo 配置或任何密钥。提交前必须再次检查暂存区。

## 项目概览

这是一个面向 Halo 2.25+ 的“文本绘图”插件，为默认富文本编辑器和文章/单页渲染提供 Mermaid 与 PlantUML 支持。

- 后端：Java 21、Gradle、Halo Plugin API、Spring/Reactor。
- 前端：Vue 3、TypeScript、Halo UI Plugin Bundler Kit、Rsbuild、pnpm。
- 插件标识：`text-diagram`。
- Java 包名：`run.halo.plugin.textdiagram`。
- CI 基线：Node.js 20、pnpm 10；本地应尽量与 CI 保持一致。

## 目录与职责

- `src/main/java/run/halo/plugin/textdiagram/`
  - `StarterPlugin.java`：插件入口。
  - `DefaultPostContentHandler.java`、`DefaultSinglePageContentHandler.java`：为文章和单页内容注入渲染资源。
  - `JSInjector.java`：生成 Mermaid 样式及运行时脚本，是主题切换、PJAX 和重复渲染逻辑的核心。
  - `BasicConfig.java`：映射插件设置。
- `src/main/resources/plugin.yaml`：插件元数据和 Halo 版本约束。
- `src/main/resources/extensions/`：扩展点、反向代理和设置表单定义。
- `console/src/`：Halo 控制台编辑器扩展。
- `console/src/editor/text-diagram/TextDiagramView.vue`：Mermaid/PlantUML 编辑与预览的主要界面。
- `src/test/java/`：JUnit 5 测试，覆盖资源位置、扩展定义和注入脚本行为。
- `build/`、`console/node_modules/`、`src/main/resources/ui/`、`src/main/resources/static/`：生成内容，不要手工编辑或提交。

## 环境初始化与常用命令

在仓库根目录执行：

```powershell
# 确认版本；Java 必须为 21，Node/pnpm 应与 CI 基线兼容
java -version
node --version
pnpm --version

# 安装前端依赖
.\gradlew.bat pnpmInstall

# 完整构建（会先安装依赖并构建前端）
.\gradlew.bat build
```

前端快速开发与检查：

```powershell
pnpm --dir console run dev
pnpm --dir console run type-check
pnpm --dir console run build
```

`pnpm --dir console run lint` 当前带有 `--fix`，会直接修改文件；只有确认需要自动修复时才运行，并在运行后审查 diff。

## 开发与实现约束

### Java/Halo

- Java 源码必须保持 Java 21 兼容；不要绕过 Gradle toolchain 或随意调整 Halo 平台版本。
- 内容处理器采用 Reactor `Mono`，不要引入阻塞调用。异常时应记录错误并保留原始内容，避免因绘图失败破坏文章渲染。
- 文章和单页处理器具有成对行为；修改一侧时必须检查另一侧是否也需要同步。
- 设置组名、字段名及资源名跨 Java、YAML 和前端存在契约关系，变更时同步检查：
  - 设置组 `basic`；
  - `dark_class_selector`；
  - `mermaid_selector`；
  - `plugin-text-diagram-settings`；
  - `plugin-text-diagram-configMap`。
- 所有来自配置的字符串在嵌入 JavaScript 前必须安全转义。禁止直接拼接未转义的选择器或用户输入。

### Vue/TypeScript

- 遵循现有 Vue 3 Composition API、TypeScript 和双引号风格；保持组件改动局部、类型明确。
- Mermaid 是异步加载和异步渲染的。修改预览逻辑时要保留对并发请求、过期结果和渲染失败的处理。
- 文章页运行时必须继续兼容初次加载与 `pjax:success`，避免重复监听、重复渲染或多次初始化全局 Mermaid 状态。
- 暗色主题判断、字体加载等待、`htmlLabels: false` 和宽图横向滚动是现有兼容性行为，除非需求明确要求，否则不要移除。
- 不要直接修改构建生成的 `main.js`、`mermaid.min.js` 或资源目录；修改源文件或 `console/rsbuild.config.mjs` 后重新构建。

### 资源打包

- Halo 2.25 的 UI bundle 必须位于 `resources/ui`，不要恢复旧的 `resources/console` 路径。
- 运行时公开路径应保持为 `/plugins/text-diagram/assets/ui/`；Mermaid 静态资源通过 `/plugins/text-diagram/assets/static/` 访问。
- 生产构建输出到 `src/main/resources/ui`，开发模式输出到 `build/resources/main/ui`。资源路径变更必须执行完整构建和 `UiBundleLocationTest`。

## 验证要求

根据修改范围执行最小充分验证：

```powershell
# Java 或注入逻辑
.\gradlew.bat test

# Vue/TypeScript
pnpm --dir console run type-check
pnpm --dir console run build

# 依赖、资源路径、Gradle 或跨端改动
.\gradlew.bat build
```

- 修改 `JSInjector` 时，至少覆盖：配置字符串转义、首次渲染、PJAX 再渲染、未处理节点过滤、暗色主题和加载失败路径。
- 修改编辑器渲染时，至少人工验证：合法 Mermaid、非法 Mermaid、快速连续编辑、Mermaid/PlantUML 切换和全屏状态。
- Web 调试优先使用系统默认浏览器，其次使用 Codex 内置浏览器。
- 若需要在 Halo 中联调，使用 README 中的 development runtime 配置，并将 `fixedPluginPath` 指向本仓库；不要提交本地 Halo 配置。
- 完成前报告实际执行的命令及结果；未执行的检查要明确说明原因。

## Git 与交付

- 提交前执行 `git diff --check`、`git status --short` 和 `git diff --cached`。
- 仅暂存本次任务涉及的明确路径，不使用会把用户改动一并纳入的宽泛暂存命令。
- 依赖升级必须同时更新 `console/package.json` 和 `console/pnpm-lock.yaml`，并验证完整构建。
- 不推送 `build/`、`workplace/`、`console/node_modules/`、本地配置、日志、令牌、证书或其他密钥材料。
- 未经用户明确要求，不创建提交、不推送远程仓库、不发布 Release。
