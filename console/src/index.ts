import { definePlugin } from "@halo-dev/ui-shared";

export default definePlugin({
  components: {},
  routes: [],
  extensionPoints: {
    "default:editor:extension:create": async () => {
      try {
        const { ExtensionTextDiagram } = await import("./editor/text-diagram");
        return [ExtensionTextDiagram];
      } catch (error) {
        console.error("Text Diagram editor extension failed to load", error);
        return [];
      }
    },
  },
});
