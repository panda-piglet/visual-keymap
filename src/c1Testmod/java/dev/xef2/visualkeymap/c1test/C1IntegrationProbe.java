package dev.xef2.visualkeymap.c1test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.KeymapSnapshot;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.integration.MaLiLibIntegration;
import dev.xef2.visualkeymap.input.MinecraftInputCode;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Development-only C1 probe. This source set is not included in Visual Keymap's published JAR.
 */
public final class C1IntegrationProbe implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("visual-keymap-c1-test");
    private static final String FREE_CAMERA = "tweakFreeCamera";
    private static final String HOLD_ATTACK = "tweakHoldAttack";

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            String stage = System.getProperty("visualkeymap.c1.stage", "apply");
            Path result = FabricLoader.getInstance().getGameDir().resolve("c1-probe-" + stage + ".txt");
            try {
                List<String> evidence = switch (stage) {
                    case "apply" -> applySnapshot();
                    case "exercise" -> exerciseAdapter();
                    case "verifyCombo" -> verifyCombinationAndRestore();
                    case "verify" -> verifyRestoredState();
                    default -> throw new IllegalArgumentException("Unknown C1 probe stage: " + stage);
                };
                evidence.add(0, "status=PASS");
                evidence.add(1, "stage=" + stage);
                evidence.add(2, "gameDir=" + FabricLoader.getInstance().getGameDir().toAbsolutePath());
                Files.write(result, evidence);
                LOGGER.info("C1 integration probe stage '{}' passed; evidence: {}", stage, result);
            } catch (Throwable throwable) {
                try {
                    StringWriter stack = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(stack));
                    Files.writeString(result, "status=FAIL\nstage=" + stage + "\n" + stack);
                } catch (Exception writeError) {
                    throwable.addSuppressed(writeError);
                }
                LOGGER.error("C1 integration probe stage '{}' failed", stage, throwable);
            } finally {
                client.stop();
            }
        });
    }

    private static List<String> applySnapshot() throws Exception {
        MaLiLibIntegration api = new MaLiLibIntegration();
        List<MaLiLibIntegration.MaLiLibKeyBinding> bindings = api.getKeyBindings();
        check(!bindings.isEmpty(), "MaLiLib returned no registered hotkeys");

        Path snapshotPath = Path.of(System.getProperty("visualkeymap.c1.snapshot"));
        KeymapSnapshot snapshot = KeymapSnapshot.readFromFile(snapshotPath.toFile());
        check(snapshot.version() == 1, "Expected snapshot version 1");
        List<KeymapSnapshot.ImportMatch> matches = snapshot.matchBindings(List.of(api));
        check(matches.size() == 2, "Expected two MaLiLib snapshot matches, got " + matches.size());
        for (KeymapSnapshot.ImportMatch match : matches) {
            check(match.canApply(), "Snapshot entry cannot be applied: " + match.entry.id());
            match.binding.setBoundKeys(match.getKeysFromEntry());
        }
        api.save();

        assertCanonical(binding(api, FREE_CAMERA), List.of(74));
        assertCanonical(binding(api, HOLD_ATTACK), List.of(298));
        check(rawKeybind(FREE_CAMERA).getKeys().equals(List.of(13)), "Free camera did not receive SDL J scancode");
        check(rawKeybind(HOLD_ATTACK).getKeys().equals(List.of(66)), "Hold attack did not receive SDL F9 scancode");

        return new ArrayList<>(List.of(
                "provider=" + api.getProviderName(),
                "registeredBindings=" + bindings.size(),
                "snapshot=" + snapshotPath.toAbsolutePath(),
                "matchedIds=" + matches.stream().map(match -> match.entry.id()).toList(),
                "tweakFreeCamera.canonical=[74]",
                "tweakFreeCamera.raw=[13]",
                "tweakHoldAttack.canonical=[298]",
                "tweakHoldAttack.raw=[66]"
        ));
    }

    private static List<String> exerciseAdapter() throws Exception {
        MaLiLibIntegration api = new MaLiLibIntegration();
        MaLiLibIntegration.MaLiLibKeyBinding freeCamera = binding(api, FREE_CAMERA);
        MaLiLibIntegration.MaLiLibKeyBinding holdAttack = binding(api, HOLD_ATTACK);
        assertCanonical(freeCamera, List.of(74));
        assertCanonical(holdAttack, List.of(298));

        Path roundTrip = FabricLoader.getInstance().getGameDir()
                .resolve("c1-real-26.2-malilib-roundtrip-v1.json");
        KeymapSnapshot.fromBindings(List.of(api)).writeToFile(roundTrip.toFile());
        assertSnapshotEntry(roundTrip, FREE_CAMERA, List.of(74));
        assertSnapshotEntry(roundTrip, HOLD_ATTACK, List.of(298));

        freeCamera.setBoundKeys(decode(List.of(74, 298, 0, 1)));
        assertCanonical(freeCamera, List.of(74, 298, 0, 1));
        check(rawKeybind(FREE_CAMERA).getKeys().equals(List.of(13, 66, -99, -97)),
                "Keyboard/mouse combination did not reach MaLiLib unchanged");

        freeCamera.setBoundKeys(decode(List.of(0, 2, 1, 7)));
        assertCanonical(freeCamera, List.of(0, 2, 1, 7));
        check(rawKeybind(FREE_CAMERA).getKeys().equals(List.of(-99, -98, -97, -92)),
                "Mouse codes -99..-92 did not round-trip");

        IKeybind raw = rawKeybind(FREE_CAMERA);
        raw.clearKeys();
        List.of(13, -200, -300).forEach(raw::addKey);
        freeCamera.setBoundKeys(decode(List.of(298)));
        check(raw.getKeys().equals(List.of(66, -200, -300)),
                "Unsupported negative/gamepad codes were not retained during edit");
        assertCanonical(freeCamera, List.of(298));

        raw.clearKeys();
        raw.addKey(13);
        freeCamera.setBoundKeys(List.<InputConstants.Key>of());
        check(raw.getKeys().isEmpty(), "Supported binding did not clear");

        freeCamera.resetToDefault();
        check(freeCamera.isDefault(), "MaLiLib resetToDefault did not restore default state");

        freeCamera.setBoundKeys(decode(List.of(74, 298, 0, 1)));
        holdAttack.setBoundKeys(decode(List.of(298)));
        api.save();

        return new ArrayList<>(List.of(
                "restartLoad.tweakFreeCamera=[74]",
                "restartLoad.tweakHoldAttack=[298]",
                "snapshotRoundTrip=" + roundTrip.toAbsolutePath(),
                "combination.canonical=[74, 298, 0, 1]",
                "combination.raw=[13, 66, -99, -97]",
                "mouse.canonical=[0, 2, 1, 7]",
                "mouse.raw=[-99, -98, -97, -92]",
                "unsupportedPreserved.raw=[66, -200, -300]",
                "unbound=[]",
                "resetToDefault=true",
                "savedCombinationForRestart=[74, 298, 0, 1]"
        ));
    }

    private static List<String> verifyCombinationAndRestore() {
        MaLiLibIntegration api = new MaLiLibIntegration();
        MaLiLibIntegration.MaLiLibKeyBinding freeCamera = binding(api, FREE_CAMERA);
        MaLiLibIntegration.MaLiLibKeyBinding holdAttack = binding(api, HOLD_ATTACK);
        assertCanonical(freeCamera, List.of(74, 298, 0, 1));
        assertCanonical(holdAttack, List.of(298));
        check(rawKeybind(FREE_CAMERA).getKeys().equals(List.of(13, 66, -99, -97)),
                "Saved keyboard/mouse combination did not persist across restart");

        freeCamera.setBoundKeys(decode(List.of(74)));
        holdAttack.setBoundKeys(decode(List.of(298)));
        api.save();

        return new ArrayList<>(List.of(
                "persistedCombination.canonical=[74, 298, 0, 1]",
                "persistedCombination.raw=[13, 66, -99, -97]",
                "restoredForRestart=true"
        ));
    }

    private static List<String> verifyRestoredState() throws Exception {
        MaLiLibIntegration api = new MaLiLibIntegration();
        assertCanonical(binding(api, FREE_CAMERA), List.of(74));
        assertCanonical(binding(api, HOLD_ATTACK), List.of(298));
        check(rawKeybind(FREE_CAMERA).getKeys().equals(List.of(13)), "Restored J binding did not persist");
        check(rawKeybind(HOLD_ATTACK).getKeys().equals(List.of(66)), "Restored F9 binding did not persist");

        Path roundTrip = FabricLoader.getInstance().getGameDir()
                .resolve("c1-real-26.2-malilib-after-restart-v1.json");
        KeymapSnapshot.fromBindings(List.of(api)).writeToFile(roundTrip.toFile());
        assertSnapshotEntry(roundTrip, FREE_CAMERA, List.of(74));
        assertSnapshotEntry(roundTrip, HOLD_ATTACK, List.of(298));

        return new ArrayList<>(List.of(
                "persisted.tweakFreeCamera.canonical=[74]",
                "persisted.tweakFreeCamera.raw=[13]",
                "persisted.tweakHoldAttack.canonical=[298]",
                "persisted.tweakHoldAttack.raw=[66]",
                "snapshotAfterRestart=" + roundTrip.toAbsolutePath()
        ));
    }

    private static MaLiLibIntegration.MaLiLibKeyBinding binding(MaLiLibIntegration api, String id) {
        return api.getKeyBindings().stream()
                .filter(binding -> binding.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Tweakeroo hotkey: " + id));
    }

    private static IKeybind rawKeybind(String id) {
        return InputEventHandler.getKeybindManager().getKeybindCategories().stream()
                .flatMap(category -> category.getHotkeys().stream())
                .filter(hotkey -> hotkey.getName().equals(id))
                .map(IHotkey::getKeybind)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing raw Tweakeroo hotkey: " + id));
    }

    private static List<InputConstants.Key> decode(List<Integer> canonicalCodes) {
        return canonicalCodes.stream()
                .map(code -> MinecraftInputCode.decode(code).orElseThrow())
                .toList();
    }

    private static void assertCanonical(KeyBinding binding, List<Integer> expected) {
        check(binding.getKeyCodes().equals(expected),
                binding.getId() + " expected canonical " + expected + " but got " + binding.getKeyCodes());
    }

    private static void assertSnapshotEntry(Path path, String id, List<Integer> expectedKeys) throws Exception {
        KeymapSnapshot snapshot = KeymapSnapshot.readFromFile(path.toFile());
        check(snapshot.version() == 1, "Round-trip snapshot schema changed");
        KeymapSnapshot.BindingEntry entry = snapshot.bindings().stream()
                .filter(candidate -> candidate.provider().equals("malilib") && candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Round-trip snapshot missing MaLiLib entry: " + id));
        check(entry.keys().equals(expectedKeys), "Round-trip keys changed for " + id + ": " + entry.keys());
        check(entry.modifiers().isEmpty(), "Round-trip modifiers changed for " + id);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
