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
                if (maLiLibLoaded || tweakerooLoaded) {
                    throw new AssertionError("MaLiLib/Tweakeroo unexpectedly loaded");
                }
                Files.write(result, List.of(
                        "status=PASS",
                        "gameDir=" + FabricLoader.getInstance().getGameDir().toAbsolutePath(),
                        "visual-keymap=" + FabricLoader.getInstance().isModLoaded("visual-keymap"),
                        "malilib=" + maLiLibLoaded,
                        "tweakeroo=" + tweakerooLoaded
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
}
