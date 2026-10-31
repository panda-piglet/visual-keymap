package dev.xef2.visualkeymap.c1test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Development-only startup probe for the optional-dependency-absent configuration. */
public final class OptionalDependencyProbe implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Path result = FabricLoader.getInstance().getGameDir().resolve("optional-dependency-probe.txt");
            try {
                boolean maLiLibLoaded = FabricLoader.getInstance().isModLoaded("malilib");
                boolean tweakerooLoaded = FabricLoader.getInstance().isModLoaded("tweakeroo");
                boolean commandKeysLoaded = FabricLoader.getInstance().isModLoaded("commandkeys");
                checkExpected("malilib", maLiLibLoaded);
                checkExpected("tweakeroo", tweakerooLoaded);
                checkExpected("commandkeys", commandKeysLoaded);
                Files.write(result, List.of(
                        "status=PASS",
                        "gameDir=" + FabricLoader.getInstance().getGameDir().toAbsolutePath(),
                        "visual-keymap=" + FabricLoader.getInstance().isModLoaded("visual-keymap"),
                        "malilib=" + maLiLibLoaded,
                        "tweakeroo=" + tweakerooLoaded,
                        "commandkeys=" + commandKeysLoaded
                ));
            } catch (Throwable throwable) {
                try {
                    Files.writeString(result, "status=FAIL\n" + throwable);
                } catch (Exception writeError) {
                    throwable.addSuppressed(writeError);
                }
            } finally {
                client.stop();
            }
        });
    }

    private static void checkExpected(String modId, boolean actual) {
        String property = "visualkeymap.optional.expect" + switch (modId) {
            case "malilib" -> "Malilib";
            case "tweakeroo" -> "Tweakeroo";
            case "commandkeys" -> "Commandkeys";
            default -> throw new IllegalArgumentException("Unknown optional mod: " + modId);
        };
        boolean expected = Boolean.parseBoolean(System.getProperty(property));
        if (actual != expected) {
            throw new AssertionError(modId + " loaded=" + actual + ", expected=" + expected);
        }
    }
}
