package dev.xef2.visualkeymap.c3atest;

import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.xef2.visualkeymap.gui.screen.VisualKeymapScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Development-only C3-A probe. This source set is not included in the published JAR. */
public final class ModMenuIntegrationProbe implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Path result = FabricLoader.getInstance().getGameDir().resolve("c3a-modmenu-probe.txt");
            try {
                FabricLoader loader = FabricLoader.getInstance();
                check(loader.isModLoaded("modmenu"), "Mod Menu is not loaded");

                EntrypointContainer<ModMenuApi> container = loader
                        .getEntrypointContainers("modmenu", ModMenuApi.class)
                        .stream()
                        .filter(entrypoint -> entrypoint.getProvider().getMetadata().getId().equals("visual-keymap"))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Visual Keymap Mod Menu entrypoint is missing"));
                ModMenuApi api = container.getEntrypoint();

                Screen parent = new ProbeParentScreen();
                Screen first = api.getModConfigScreenFactory().create(parent);
                Screen second = api.getModConfigScreenFactory().create(parent);
                check(first instanceof VisualKeymapScreen,
                        "Config factory did not create VisualKeymapScreen: " + first.getClass().getName());
                check(second instanceof VisualKeymapScreen,
                        "Repeated config factory call changed screen type: " + second.getClass().getName());
                check(first != second, "Config factory reused a screen instance");

                client.setScreenAndShow(first);
                check(client.gui.screen() == first, "First Visual Keymap screen was not installed");
                first.onClose();
                check(client.gui.screen() == parent, "Done/Esc close path did not return to the supplied parent");

                client.setScreenAndShow(second);
                check(client.gui.screen() == second, "Second Visual Keymap screen was not installed");
                second.onClose();
                check(client.gui.screen() == parent, "Repeated close did not return to the supplied parent");

                List<String> evidence = new ArrayList<>();
                evidence.add("status=PASS");
                evidence.add("gameDir=" + loader.getGameDir().toAbsolutePath());
                evidence.add("modmenu.loaded=true");
                evidence.add("modmenu.version=" + loader.getModContainer("modmenu").orElseThrow().getMetadata().getVersion());
                evidence.add("entrypoint.provider=" + container.getProvider().getMetadata().getId());
                evidence.add("entrypoint.class=" + api.getClass().getName());
                evidence.add("screen.class=" + first.getClass().getName());
                evidence.add("screen.instances.distinct=true");
                evidence.add("parent.return.first=true");
                evidence.add("parent.return.second=true");
                Files.write(result, evidence);
            } catch (Throwable throwable) {
                try {
                    StringWriter stack = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(stack));
                    Files.writeString(result, "status=FAIL\n" + stack);
                } catch (Exception writeError) {
                    throwable.addSuppressed(writeError);
                }
            } finally {
                client.stop();
            }
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class ProbeParentScreen extends Screen {
        private ProbeParentScreen() {
            super(Component.literal("C3-A parent"));
        }
    }
}
