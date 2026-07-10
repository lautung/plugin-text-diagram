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
import IcOutlineFullscreen from "~icons/ic/outline-fullscreen";
import IcOutlineFullscreenExit from "~icons/ic/outline-fullscreen-exit";

const props = defineProps(nodeViewProps);
const previewRef = ref<HTMLElement>();
const fullscreen = ref(false);
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
    return;
  }

  switch (diagramType) {
    case "mermaid": {
      try {
        const { svg } = await renderMermaid(graphDefinition);
        if (!isCurrentRender(currentRequestId, element)) return;

        element.innerHTML = svg;
      } catch (error) {
        if (isCurrentRender(currentRequestId, element)) {
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
        element.innerHTML = `<img src="${url}" alt="plantuml"/>`;
      } catch (error) {
        if (isCurrentRender(currentRequestId, element)) {
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
</script>
<template>
  <node-view-wrapper
    class="text-diagram-container"
    :class="{ 'text-diagram-fullscreen': fullscreen }"
  >
    <div class="text-diagram-nav">
      <div class="text-diagram-nav-start">
        <div>文本绘图</div>
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
        <div
          class="text-diagram-fullscreen-icon"
          @click="fullscreen = !fullscreen"
        >
          <IcOutlineFullscreenExit v-if="fullscreen" v-tooltip="'退出全屏'" />
          <IcOutlineFullscreen v-else v-tooltip="'全屏'" />
        </div>
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
  border: 1px #e7e7e7 solid;
  border-radius: 4px;
  overflow: hidden;
  margin-top: 0.75em;
}

.text-diagram-nav {
  border-bottom: 1px #e7e7e7 solid;
  display: flex;
  padding: 5px 10px;
  align-items: center;
}

.text-diagram-nav-start {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.text-diagram-nav-end {
  justify-content: flex-end;
}

.text-diagram-editor-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 100%;
  height: 100%;
}

.text-diagram-type-select {
  width: 7em;
}

.text-diagram-code {
  height: 100%;
  border-right: 1px #e7e7e7 solid;
}

.text-diagram-preview {
  padding: 5px;
  height: 100%;
  overflow: auto;
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
}

.text-diagram-fullscreen-icon {
  cursor: pointer;
}

.text-diagram-fullscreen-icon svg {
  font-size: 18px;
}

.text-diagram-fullscreen-icon:hover {
  color: #999;
}
</style>
