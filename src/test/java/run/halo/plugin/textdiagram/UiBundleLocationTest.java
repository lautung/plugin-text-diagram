package run.halo.plugin.textdiagram;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class UiBundleLocationTest {

    @Test
    void shouldPackageUiAssetsFromHalo225BundleLocation() throws Exception {
        Path uiMain = Path.of("build/resources/main/ui/main.js");
        Path legacyConsoleMain = Path.of("build/resources/main/console/main.js");

        assertTrue(Files.exists(uiMain), "Halo 2.25 UI plugin assets should be under resources/ui");
        assertFalse(
            Files.exists(legacyConsoleMain),
            "resources/console should not be packaged for Halo 2.25 UI plugin bundles"
        );
        assertTrue(
            Files.readString(uiMain).contains("/plugins/text-diagram/assets/ui/"),
            "runtime publicPath should match the packaged ui directory"
        );
    }
}
