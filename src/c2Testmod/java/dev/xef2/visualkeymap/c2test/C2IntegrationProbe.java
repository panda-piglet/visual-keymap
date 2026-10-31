package dev.xef2.visualkeymap.c2test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Development-only C2 probe. This source set is not included in Visual Keymap's published JAR. */
public final class C2IntegrationProbe implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("visual-keymap-c2-test");

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            String stage = System.getProperty("visualkeymap.c2.stage", "dependency");
            Path result = FabricLoader.getInstance().getGameDir().resolve("c2-probe-" + stage + ".txt");
            try {
                List<String> evidence = switch (stage) {
                    case "dependency" -> verifyDependency();
                    default -> throw new IllegalArgumentException("Unknown C2 probe stage: " + stage);
                };
                evidence.add(0, "status=PASS");
                evidence.add(1, "stage=" + stage);
                evidence.add(2, "gameDir=" + FabricLoader.getInstance().getGameDir().toAbsolutePath());
                Files.write(result, evidence);
                LOGGER.info("C2 integration probe stage '{}' passed; evidence: {}", stage, result);
            } catch (Throwable throwable) {
                try {
                    StringWriter stack = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(stack));
                    Files.writeString(result, "status=FAIL\nstage=" + stage + "\n" + stack);
                } catch (Exception writeError) {
                    throwable.addSuppressed(writeError);
                }
                LOGGER.error("C2 integration probe stage '{}' failed", stage, throwable);
            } finally {
                client.stop();
            }
        });
    }

    private static List<String> verifyDependency() {
        FabricLoader loader = FabricLoader.getInstance();
        check(loader.isModLoaded("commandkeys"), "FabricLoader did not load CommandKeys");
        ModContainer commandKeys = loader.getModContainer("commandkeys")
                .orElseThrow(() -> new AssertionError("CommandKeys ModContainer is missing"));
        check(commandKeys.getMetadata().getVersion().getFriendlyString().equals("3.104.1+26.3"),
                "Unexpected CommandKeys version: " + commandKeys.getMetadata().getVersion());

        return new ArrayList<>(List.of(
                "commandkeys.loaded=true",
                "commandkeys.id=" + commandKeys.getMetadata().getId(),
                "commandkeys.version=" + commandKeys.getMetadata().getVersion().getFriendlyString(),
                "commandkeys.name=" + commandKeys.getMetadata().getName(),
                "visual-keymap.loaded=" + loader.isModLoaded("visual-keymap"),
                "malilib.loaded=" + loader.isModLoaded("malilib"),
                "modmenu.loaded=" + loader.isModLoaded("modmenu")
        ));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
