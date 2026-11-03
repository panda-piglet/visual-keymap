package dev.xef2.visualkeymap.gui.screen;

import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.KeymapSnapshot;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportFailureFlowTest {
    @TempDir
    Path tempDir;

    @Test
    void invalidInputStopsBeforeProviderEnumerationWritesOrSaves() throws Exception {
        Path invalid = write("invalid.json", "null");
        ProbeBinding binding = new ProbeBinding("key.test", List.of(66));
        ProbeApi api = new ProbeApi(binding);

        assertThrows(JsonParseException.class,
                () -> VisualKeymapScreen.prepareImport(invalid, List.of(api)));

        assertEquals(0, api.enumerations);
        assertEquals(0, api.saves);
        assertEquals(0, binding.writes);
        assertEquals(List.of(66), binding.getKeyCodes());
    }

    @Test
    void validImportStillWorksImmediatelyAfterAnInvalidAttempt() throws Exception {
        Path invalid = write("invalid.json", "{\"version\":1}");
        Path valid = write("valid.json", """
                {"version":1,"bindings":[
                  {"provider":"probe","id":"key.test","keys":[65],"modifiers":[]}
                ]}
                """);
        ProbeBinding binding = new ProbeBinding("key.test", List.of(66));
        ProbeApi api = new ProbeApi(binding);

        assertThrows(JsonParseException.class,
                () -> VisualKeymapScreen.prepareImport(invalid, List.of(api)));
        List<KeymapSnapshot.ImportMatch> matches = VisualKeymapScreen.prepareImport(valid, List.of(api));

        assertEquals(1, matches.size());
        assertTrue(matches.getFirst().selected);
        assertEquals(List.of(65), matches.getFirst().entry.keys());
        assertEquals(1, api.enumerations);
        assertEquals(0, api.saves);
        assertEquals(0, binding.writes, "preparing the preview must not apply a binding");
        assertEquals(List.of(66), binding.getKeyCodes());
    }

    private Path write(String name, String content) throws Exception {
        Path file = this.tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static final class ProbeApi implements VisualKeymapApi<ProbeBinding> {
        private final ProbeBinding binding;
        private int enumerations;
        private int saves;

        private ProbeApi(ProbeBinding binding) {
            this.binding = binding;
        }

        @Override
        public List<ProbeBinding> getKeyBindings() {
            this.enumerations++;
            return List.of(this.binding);
        }

        @Override
        public void save() {
            this.saves++;
        }

        @Override
        public String getProviderName() {
            return "probe";
        }
    }

    private static final class ProbeBinding extends KeyBinding {
        private final String id;
        private final List<Integer> keys;
        private int writes;

        private ProbeBinding(String id, List<Integer> keys) {
            super(Component.literal("Probe"), Component.literal(id), 1);
            this.id = id;
            this.keys = List.copyOf(keys);
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public List<Integer> getKeyCodes() {
            return this.keys;
        }

        @Override
        public void setBoundKeys(List<InputConstants.Key> keys) {
            this.writes++;
        }

        @Override
        public boolean isDefault() {
            return false;
        }

        @Override
        public void resetToDefault() {
        }
    }
}
