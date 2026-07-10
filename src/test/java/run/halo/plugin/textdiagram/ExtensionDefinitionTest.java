package run.halo.plugin.textdiagram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ExtensionDefinitionTest {

    @Test
    void shouldLoadAllClassesDeclaredInExtensionDefinitions() throws Exception {
        String extensions = Files.readString(Path.of("src/main/resources/extensions/extensions.yaml"));
        Matcher matcher = Pattern.compile("className:\\s*([^\\s]+)").matcher(extensions);
        boolean foundClassName = false;

        while (matcher.find()) {
            foundClassName = true;
            String className = matcher.group(1);
            assertDoesNotThrow(() -> Class.forName(className), className + " should be loadable");
        }

        assertTrue(foundClassName, "extensions.yaml should declare at least one className");
    }
}
