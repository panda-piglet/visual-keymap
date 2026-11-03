package dev.xef2.visualkeymap;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeymapSnapshotReadTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsTheUserProvidedInvalidJsonFixture() throws IOException {
        Path snapshot = this.tempDir.resolve("invalid-user-input.json");
        try (InputStream input = getClass().getResourceAsStream("/dev/xef2/visualkeymap/invalid-user-input.json")) {
            Files.copy(input, snapshot);
        }

        assertThrows(JsonParseException.class, () -> KeymapSnapshot.readFromFile(snapshot.toFile()));
    }

    @Test
    void rejectsEmptyMalformedAndNullDocuments() throws IOException {
        assertInvalid("");
        assertInvalid("{\"version\":1,");
        assertInvalid("null");
    }

    @Test
    void rejectsScalarAndArrayRoots() throws IOException {
        assertInvalid("\"snapshot\"");
        assertInvalid("42");
        assertInvalid("[]");
    }

    @Test
    void rejectsMissingOrIncorrectTopLevelFields() throws IOException {
        assertInvalid("{}");
        assertInvalid("{\"version\":1}");
        assertInvalid("{\"version\":\"one\",\"bindings\":[]}");
        assertInvalid("{\"version\":1,\"bindings\":{}}");
    }

    @Test
    void rejectsNullEntriesAndNullIntegerElementsBeforeMatching() throws IOException {
        assertInvalid("{\"version\":1,\"bindings\":[null]}");
        assertInvalid("{\"version\":1,\"bindings\":[{\"provider\":\"minecraft\",\"id\":\"key.test\",\"keys\":[null],\"modifiers\":[]}]}");
        assertInvalid("{\"version\":1,\"bindings\":[{\"provider\":\"minecraft\",\"id\":\"key.test\",\"keys\":[65],\"modifiers\":[null]}]}");
    }

    @Test
    void preservesBindingEntryNullFieldNormalization() throws Exception {
        KeymapSnapshot snapshot = read("{\"version\":1,\"bindings\":[{}]}");

        assertEquals(1, snapshot.bindings().size());
        assertEquals("", snapshot.bindings().getFirst().provider());
        assertEquals("", snapshot.bindings().getFirst().id());
        assertEquals(List.of(), snapshot.bindings().getFirst().keys());
        assertEquals(List.of(), snapshot.bindings().getFirst().modifiers());
        assertDoesNotThrow(() -> snapshot.matchBindings(List.of()));
    }

    @Test
    void readsValidVersionOneEmptyAndNonEmptySnapshots() throws Exception {
        KeymapSnapshot empty = read("{\"version\":1,\"bindings\":[]}");
        KeymapSnapshot nonEmpty = read("""
                {"version":1,"bindings":[
                  {"provider":"minecraft","id":"key.test","keys":[65,0],"modifiers":[341]}
                ]}
                """);

        assertEquals(1, empty.version());
        assertEquals(List.of(), empty.bindings());
        assertEquals(1, nonEmpty.version());
        assertEquals("minecraft", nonEmpty.bindings().getFirst().provider());
        assertEquals("key.test", nonEmpty.bindings().getFirst().id());
        assertEquals(List.of(65, 0), nonEmpty.bindings().getFirst().keys());
        assertEquals(List.of(341), nonEmpty.bindings().getFirst().modifiers());
    }

    @Test
    void preservesUnknownVersionBehavior() throws Exception {
        KeymapSnapshot snapshot = read("{\"version\":2,\"bindings\":[]}");

        assertEquals(2, snapshot.version());
        assertEquals(List.of(), snapshot.bindings());
    }

    @Test
    void preservesIoFailureForMissingFiles() {
        Path missing = this.tempDir.resolve("missing.json");

        assertThrows(IOException.class, () -> KeymapSnapshot.readFromFile(missing.toFile()));
    }

    private void assertInvalid(String content) throws IOException {
        Path file = this.tempDir.resolve("invalid-" + System.nanoTime() + ".json");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        assertThrows(JsonParseException.class, () -> KeymapSnapshot.readFromFile(file.toFile()));
    }

    private KeymapSnapshot read(String content) throws Exception {
        Path file = this.tempDir.resolve("snapshot-" + System.nanoTime() + ".json");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return KeymapSnapshot.readFromFile(file.toFile());
    }
}
