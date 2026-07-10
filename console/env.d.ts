/// <reference types="@rsbuild/core" />
/// <reference types="unplugin-icons/types/vue" />

declare module "~icons/*" {
  import type { DefineComponent } from "vue";

  const component: DefineComponent;
  export default component;
}

declare module "*.vue" {
  import { defineComponent } from "vue";

  const component: ReturnType<typeof defineComponent>;
  export default component;
}
