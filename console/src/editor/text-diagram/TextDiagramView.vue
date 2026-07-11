<script lang="ts">
const mermaidRenderConfig = {
  startOnLoad: false,
  htmlLabels: false,
  flowchart: {
    htmlLabels: false,
    useMaxWidth: false,
  },
};

let mermaidLoader: Promise<typeof import("mermaid").default> | undefined;
let mermaidRenderChain: Promise<void> = Promise.resolve();
let mermaidElementId = 0;
let plantUmlEncoderLoader:
  | Promise<typeof import("./plantuml/encoder")>
  | undefined;

function getMermaid() {
  if (!mermaidLoader) {
    mermaidLoader = import("mermaid").then(({ default: mermaid }) => {
      mermaid.initialize(mermaidRenderConfig);
      return mermaid;
    });
  }

  return mermaidLoader;
}

function renderMermaid(graphDefinition: string) {
  const render = mermaidRenderChain.then(async () => {
    const mermaid = await getMermaid();
    const id = `text-diagram-mermaid-${++mermaidElementId}`;
    return mermaid.render(id, graphDefinition);
  });

  mermaidRenderChain = render.then(
    () => undefined,
    () => undefined
  );
  return render;
}

function getPlantUmlEncoder() {
  if (!plantUmlEncoderLoader) {
    plantUmlEncoderLoader = import("./plantuml/encoder").catch((error) => {
      plantUmlEncoderLoader = undefined;
      throw error;
    });
  }

  return plantUmlEncoderLoader;
}
</script>
<script lang="ts" setup>
import { nodeViewProps, NodeViewWrapper } from "@halo-dev/richtext-editor";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import IcOutlineTipsAndUpdates from "~icons/ic/outline-tips-and-updates";
import IcOutlineCode from "~icons/ic/outline-code";
import IcOutlineContentCopy from "~icons/ic/outline-content-copy";
import IcOutlineDownload from "~icons/ic/outline-download";
import IcOutlineFullscreen from "~icons/ic/outline-fullscreen";
import IcOutlineFullscreenExit from "~icons/ic/outline-fullscreen-exit";
import IcOutlinePlayArrow from "~icons/ic/outline-play-arrow";

const props = defineProps(nodeViewProps);
const previewRef = ref<HTMLElement>();
const fullscreen = ref(false);
const renderedSvg = ref("");
let renderRequestId = 0;
let previewTimer: number | undefined;

const languages = [
  {
    value: "mermaid",
    label: "Mermaid",
    document: "https://mermaid.js.org/",
  },
  {
    value: "plantuml",
    label: "PlantUML",
    document: "https://plantuml.com/zh/",
  },
];

const languageValue = computed({
  get: () => {
    return props.node?.attrs.type;
  },
  set: (language: string) => {
    props.updateAttributes({ type: language });
  },
});

const language = computed(() => {
  return languages.find((lan) => lan.value === languageValue.value);
});

function renderError(element: HTMLElement, error: unknown) {
  const pre = document.createElement("pre");
  pre.style.color = "red";
  pre.style.backgroundColor = "#f6f8fa";
  pre.style.padding = "0.75em";
  pre.style.whiteSpace = "pre-wrap";
  pre.textContent = error instanceof Error ? error.message : String(error);
  element.replaceChildren(pre);
}

function isCurrentRender(requestId: number, element: HTMLElement) {
  return (
    requestId === renderRequestId &&
    element === previewRef.value &&
    element.isConnected
  );
}

// render as svg
const doRenderPreview = async function (currentRequestId: number) {
  const element = previewRef.value;
  if (!element) return;

  const graphDefinition = props.node.attrs.content || "";
  const diagramType = languageValue.value;
  element.replaceChildren();

  if (!graphDefinition.trim()) {
    renderedSvg.value = "";
    return;
  }

  switch (diagramType) {
    case "mermaid": {
      try {
        const { svg } = await renderMermaid(graphDefinition);
        if (!isCurrentRender(currentRequestId, element)) return;

        renderedSvg.value = svg;
        element.innerHTML = svg;
      } catch (error) {
        if (isCurrentRender(currentRequestId, element)) {
          renderedSvg.value = "";
          renderError(element, error);
        }
      }
      break;
    }
    case "plantuml": {
      try {
        const { compress } = await getPlantUmlEncoder();
        if (!isCurrentRender(currentRequestId, element)) return;

        const url = compress(graphDefinition);
        if (!isCurrentRender(currentRequestId, element)) return;

        if (props.node.attrs.src !== url) {
          props.updateAttributes({ src: url });
        }
        renderedSvg.value = "";
        element.innerHTML = `<img src="${url}" alt="plantuml"/>`;
      } catch (error) {
        if (isCurrentRender(currentRequestId, element)) {
          renderedSvg.value = "";
          renderError(element, error);
        }
      }
      break;
    }
    default:
      break;
  }
};

function clearPreviewTimer() {
  if (previewTimer !== undefined) {
    window.clearTimeout(previewTimer);
    previewTimer = undefined;
  }
}

function scheduleRenderPreview() {
  const requestId = ++renderRequestId;
  clearPreviewTimer();
  previewTimer = window.setTimeout(() => {
    previewTimer = undefined;
    void doRenderPreview(requestId);
  }, 250);
}

watch(
  () => [props.node.attrs.content, props.node.attrs.type],
  scheduleRenderPreview,
  { flush: "post" }
);

onMounted(() => {
  scheduleRenderPreview();
});

onBeforeUnmount(() => {
  clearPreviewTimer();
  renderRequestId++;
});

// text diagram editor
function onEditorChange(value: string) {
  props.updateAttributes({ content: value });
}

function copyTextFallback(text: string) {
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "");
  textarea.style.position = "fixed";
  textarea.style.top = "0";
  textarea.style.left = "0";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.focus({ preventScroll: true });
  textarea.select();
  textarea.setSelectionRange(0, textarea.value.length);
  document.execCommand("copy");
  textarea.remove();
}

function copySource() {
  const source = props.node.attrs.content || "";
  if (navigator.clipboard?.writeText) {
    void navigator.clipboard.writeText(source).catch(() => {
      copyTextFallback(source);
    });
    return;
  }
  copyTextFallback(source);
}

function downloadSvg() {
  if (!renderedSvg.value || languageValue.value !== "mermaid") {
    return;
  }

  const blob = new Blob([renderedSvg.value], {
    type: "image/svg+xml;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "text-diagram.svg";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 0);
}
</script>
<template>
  <node-view-wrapper
    class="text-diagram-container"
    :class="{ 'text-diagram-fullscreen': fullscreen }"
  >
    <div class="text-diagram-nav">
      <div class="text-diagram-nav-start">
        <div class="text-diagram-title">
          <IcOutlinePlayArrow />
          <span>{{ language?.label || "文本绘图" }}</span>
        </div>
        <select
          v-model="languageValue"
          class="text-diagram-type-select block px-2 py-1.5 text-sm text-gray-900 border border-gray-300 rounded-md bg-gray-50 focus:ring-blue-500 focus:border-blue-500"
          contenteditable="false"
        >
          <option
            v-for="(lan, index) in languages"
            :key="index"
            :value="lan.value"
          >
            {{ lan.label }}
          </option>
        </select>
        <a
          v-if="language"
          v-tooltip="`查阅 ${language.label} 的文档`"
          :href="language.document"
          target="_blank"
        >
          <IcOutlineTipsAndUpdates />
        </a>
      </div>
      <div class="text-diagram-nav-end">
        <button
          class="text-diagram-tool-button"
          type="button"
          contenteditable="false"
          v-tooltip="'源码在左侧编辑区'"
        >
          <IcOutlineCode />
        </button>
        <button
          class="text-diagram-tool-button text-diagram-tool-button-active"
          type="button"
          contenteditable="false"
          v-tooltip="'实时预览'"
        >
          <IcOutlinePlayArrow />
        </button>
        <button
          class="text-diagram-tool-button"
          type="button"
          contenteditable="false"
          v-tooltip="'复制源码'"
          @click="copySource"
        >
          <IcOutlineContentCopy />
        </button>
        <button
          class="text-diagram-tool-button"
          type="button"
          contenteditable="false"
          :disabled="languageValue !== 'mermaid' || !renderedSvg"
          v-tooltip="'下载 SVG'"
          @click="downloadSvg"
        >
          <IcOutlineDownload />
        </button>
        <button
          class="text-diagram-tool-button"
          type="button"
          contenteditable="false"
          @click="fullscreen = !fullscreen"
        >
          <IcOutlineFullscreenExit v-if="fullscreen" v-tooltip="'退出全屏'" />
          <IcOutlineFullscreen v-else v-tooltip="'全屏'" />
        </button>
      </div>
    </div>
    <div class="text-diagram-editor-panel">
      <div class="text-diagram-code">
        <VCodemirror
          :model-value="node.attrs.content"
          height="100%"
          @change="onEditorChange"
        />
      </div>
      <div
        ref="previewRef"
        class="text-diagram-preview"
        contenteditable="false"
      ></div>
    </div>
  </node-view-wrapper>
</template>
<style>
.text-diagram-container {
  border: 1px #e5e7eb solid;
  border-radius: 14px;
  overflow: hidden;
  margin-top: 0.75em;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.text-diagram-nav {
  min-height: 52px;
  border-bottom: 1px #e5e7eb solid;
  display: flex;
  padding: 8px 12px;
  align-items: center;
  gap: 12px;
}

.text-diagram-nav-start {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.text-diagram-nav-end {
  display: inline-flex;
  justify-content: flex-end;
  align-items: center;
  gap: 4px;
}

.text-diagram-editor-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 100%;
  min-height: 280px;
  height: 100%;
}

.text-diagram-type-select {
  width: 7em;
}

.text-diagram-code {
  height: 100%;
  border-right: 1px #e5e7eb solid;
}

.text-diagram-preview {
  display: grid;
  align-items: center;
  padding: 24px;
  height: 100%;
  overflow: auto;
  background: #f7f7f8;
  overscroll-behavior: contain;
}

.text-diagram-preview svg {
  display: block;
  max-width: none;
  height: auto;
  margin: 0 auto;
}

.text-diagram-preview foreignObject p {
  margin: 0 !important;
  line-height: 1.5 !important;
}

.text-diagram-preview img {
  display: block;
  max-width: 100%;
  margin: 0 auto;
}

.text-diagram-code img {
  width: 100%;
}

.text-diagram-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  z-index: 9999;
  width: 100%;
  height: 100%;
  background: #fff;
  margin-top: 0;
  border-radius: 0;
}

.text-diagram-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-weight: 600;
  color: #111827;
}

.text-diagram-title svg {
  flex: 0 0 auto;
  font-size: 18px;
}

.text-diagram-tool-button {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  color: #111827;
  background: transparent;
  cursor: pointer;
}

.text-diagram-tool-button svg {
  font-size: 18px;
}

.text-diagram-tool-button:hover,
.text-diagram-tool-button:focus-visible,
.text-diagram-tool-button-active {
  background: #ececf0;
  outline: none;
}

.text-diagram-tool-button:disabled {
  color: #a1a1aa;
  cursor: not-allowed;
  background: transparent;
}

.text-diagram-fullscreen .text-diagram-editor-panel {
  min-height: 0;
  height: calc(100vh - 53px);
}

@media (max-width: 720px) {
  .text-diagram-editor-panel {
    grid-template-columns: 1fr;
  }

  .text-diagram-code {
    min-height: 220px;
    border-right: 0;
    border-bottom: 1px #e5e7eb solid;
  }
}
</style>
