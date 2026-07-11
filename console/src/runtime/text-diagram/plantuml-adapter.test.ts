import { expect, it } from "vitest";
import { createPlantUmlAdapter } from "./plantuml-adapter";

it("rejects PlantUML nodes without an image URL", async () => {
  await expect(createPlantUmlAdapter().render(document.createElement("div"), "@startuml", "light"))
    .rejects.toThrow("图片地址缺失");
});
