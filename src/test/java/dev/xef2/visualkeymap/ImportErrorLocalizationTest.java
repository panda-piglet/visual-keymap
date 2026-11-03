package dev.xef2.visualkeymap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportErrorLocalizationTest {
    private static final String[] REQUIRED_KEYS = {
            "visualkeymap.gui.import_error_title",
            "visualkeymap.gui.import_error_invalid",
            "visualkeymap.gui.import_error_io"
    };

    @Test
    void englishAndSimplifiedChineseContainConciseImportErrors() throws Exception {
        JsonObject english = readLanguage("en_us");
        JsonObject chinese = readLanguage("zh_cn");

        for (String key : REQUIRED_KEYS) {
            assertTrue(english.has(key), "English is missing " + key);
            assertTrue(chinese.has(key), "Simplified Chinese is missing " + key);
            assertFalse(english.get(key).getAsString().isBlank(), key + " must not be blank in English");
            assertFalse(chinese.get(key).getAsString().isBlank(), key + " must not be blank in Chinese");
        }
    }

    private JsonObject readLanguage(String language) throws Exception {
        String resource = "/assets/visualkeymap/lang/" + language + ".json";
        try (var reader = new InputStreamReader(
                getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
