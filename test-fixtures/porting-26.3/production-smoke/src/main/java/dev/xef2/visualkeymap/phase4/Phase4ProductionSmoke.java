package dev.xef2.visualkeymap.phase4;

import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.KeymapSnapshot;
import dev.xef2.visualkeymap.VisualKeymap;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;
import dev.xef2.visualkeymap.gui.screen.VisualKeymapScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Loads the release JAR as an external Fabric mod and verifies the P1-P6 startup matrix. */
public final class Phase4ProductionSmoke implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            FabricLoader loader = FabricLoader.getInstance();
            String profile = System.getProperty("visualkeymap.phase4.profile", "P1").toUpperCase(Locale.ROOT);
            Path result = loader.getGameDir().resolve("phase4-production-smoke.txt");
            try {
                List<String> evidence = verify(loader, client.options, profile);
                evidence.add(0, "status=PASS");
                evidence.add(1, "profile=" + profile);
                evidence.add(2, "gameDir=" + loader.getGameDir().toAbsolutePath());
                Files.write(result, evidence);
            } catch (Throwable throwable) {
                try {
                    StringWriter stack = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(stack));
                    Files.writeString(result, "status=FAIL\nprofile=" + profile + "\n" + stack);
                } catch (Exception writeError) {
                    throwable.addSuppressed(writeError);
                }
            } finally {
                if (!Boolean.getBoolean("visualkeymap.phase4.manual")) {
                    client.stop();
                }
            }
        });
    }

    private static List<String> verify(FabricLoader loader, net.minecraft.client.Options options, String profile)
            throws Exception {
        Set<String> expectedOptional = switch (profile) {
            case "P1", "P6" -> Set.of();
            case "P2" -> Set.of("modmenu");
            case "P3" -> Set.of("malilib", "tweakeroo");
            case "P4" -> Set.of("commandkeys");
            case "P5" -> Set.of("malilib", "tweakeroo", "commandkeys", "modmenu");
            default -> throw new AssertionError("Unexpected profile: " + profile);
        };

        check(loader.isModLoaded("visual-keymap"), "Visual Keymap is not loaded");
        for (String mod : List.of("malilib", "tweakeroo", "commandkeys", "modmenu")) {
            check(loader.isModLoaded(mod) == expectedOptional.contains(mod),
                    mod + " loaded=" + loader.isModLoaded(mod) + ", expected=" + expectedOptional.contains(mod));
        }

        Path releaseJar = Path.of(System.getProperty("visualkeymap.phase4.releaseJar")).toAbsolutePath().normalize();
        check(Files.isRegularFile(releaseJar), "Release JAR is missing: " + releaseJar);
        ModContainer visualKeymap = loader.getModContainer("visual-keymap").orElseThrow();
        List<Path> origins = visualKeymap.getOrigin().getPaths();
        check(origins.stream().allMatch(Files::isRegularFile), "Visual Keymap was loaded from a source directory: " + origins);
        String classPath = System.getProperty("java.class.path", "").replace('\\', '/');
        check(!classPath.contains("VisualKeyMap26.3-repo/build/classes/java/main"),
                "Repository main source output leaked into the standalone classpath");

        // Loading the target class validates the required ControlsScreen redirect; reading bindings validates KeyMappingAccessor.
        Class.forName("net.minecraft.client.gui.screens.options.controls.ControlsScreen", true,
                Phase4ProductionSmoke.class.getClassLoader());
        List<? extends KeyBinding> bindings = VisualKeymap.getKeyBindings();
        check(!bindings.isEmpty(), "No bindings were enumerated");

        List<VisualKeymapApi<?>> providers = VisualKeymap.getApiImplementations();
        List<String> providerClasses = providers.stream().map(api -> api.getClass().getSimpleName()).toList();
        List<String> expectedProviders = switch (profile) {
            case "P1", "P2" -> List.of("MinecraftImpl");
            case "P3" -> List.of("MinecraftImpl", "MaLiLibIntegration");
            case "P4" -> List.of("MinecraftImpl", "CommandKeysIntegration");
            case "P5" -> List.of("MinecraftImpl", "MaLiLibIntegration", "CommandKeysIntegration");
            case "P6" -> List.of("MinecraftImpl", "Phase4Provider");
            default -> throw new AssertionError("Unexpected profile: " + profile);
        };
        check(providerClasses.equals(expectedProviders),
                "Provider order differs: " + providerClasses + " expected " + expectedProviders);

        ProbeParent parent = new ProbeParent();
        VisualKeymapScreen screen = new VisualKeymapScreen(parent, options);
        net.minecraft.client.Minecraft.getInstance().setScreenAndShow(screen);
        check(net.minecraft.client.Minecraft.getInstance().gui.screen() == screen,
                "VisualKeymapScreen did not initialize");
        verifyInvalidSnapshotAndAlert(loader, screen, bindings, profile);
        screen.onClose();
        check(net.minecraft.client.Minecraft.getInstance().gui.screen() == parent,
                "VisualKeymapScreen did not return to its parent");

        if (profile.equals("P2") || profile.equals("P5")) {
            verifyModMenu(loader, parent);
        }
        if (profile.equals("P3") || profile.equals("P5")) {
            check(bindings.stream().anyMatch(binding -> binding.getClass().getName().contains("MaLiLibKeyBinding")),
                    "MaLiLib loaded but contributed no binding");
        }
        if (profile.equals("P6")) {
            verifyExternalProvider(loader, providers);
        }

        List<String> evidence = new ArrayList<>();
        evidence.add("releaseJar=" + releaseJar);
        evidence.add("releaseJar.sha256=" + sha256(releaseJar));
        evidence.add("visualKeymap.origin=" + origins);
        evidence.add("visualKeymap.version=" + visualKeymap.getMetadata().getVersion());
        evidence.add("loaded.optional=" + expectedOptional);
        evidence.add("providers=" + providerClasses);
        evidence.add("bindings=" + bindings.size());
        evidence.add("mixins.controlsScreen.loaded=true");
        evidence.add("mixins.keyMappingAccessor.used=true");
        evidence.add("mixins.headerAndFooterAccessor.used=true");
        evidence.add("screen.lifecycle=true");
        evidence.add("invalidSnapshot.rejected=true");
        evidence.add("invalidSnapshot.bindingsUnchanged=true");
        evidence.add("invalidSnapshot.alertLifecycle=true");
        return evidence;
    }

    private static void verifyInvalidSnapshotAndAlert(FabricLoader loader, VisualKeymapScreen screen,
                                                       List<? extends KeyBinding> bindings, String profile)
            throws Exception {
        Path invalidSnapshot = loader.getGameDir().resolve("bug01-invalid-snapshot.json");
        Files.writeString(invalidSnapshot, "null");
        List<List<Integer>> keysBefore = bindings.stream().map(KeyBinding::getKeyCodes).map(List::copyOf).toList();
        try {
            VisualKeymap.importKeyBindings(invalidSnapshot.toFile());
            throw new AssertionError("Invalid snapshot was accepted");
        } catch (JsonParseException expected) {
            // Expected user-data failure; the release JAR must reject it before matching or applying bindings.
        }
        check(keysBefore.equals(bindings.stream().map(KeyBinding::getKeyCodes).map(List::copyOf).toList()),
                "Invalid snapshot changed a live binding");

        int externalSavesBefore = Phase4Provider.saveCalls;
        Method showImportError = VisualKeymapScreen.class.getDeclaredMethod("showImportError", String.class);
        showImportError.setAccessible(true);
        showImportError.invoke(screen, "gui.import_error_invalid");
        Screen current = net.minecraft.client.Minecraft.getInstance().gui.screen();
        check(current instanceof AlertScreen, "Invalid snapshot did not open AlertScreen");
        check(current.getTitle().equals(Component.translatable("visualkeymap.gui.import_error_title")),
                "Unexpected import alert title component: " + current.getTitle());
        current.onClose();
        check(net.minecraft.client.Minecraft.getInstance().gui.screen() == screen,
                "Import alert did not return to VisualKeymapScreen");
        if (profile.equals("P6")) {
            check(Phase4Provider.saveCalls == externalSavesBefore,
                    "Opening the invalid-import alert saved the external provider");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void verifyModMenu(FabricLoader loader, Screen parent) throws Exception {
        Class<?> apiClass = Class.forName("com.terraformersmc.modmenu.api.ModMenuApi");
        List containers = loader.getEntrypointContainers("modmenu", (Class) apiClass);
        Object container = containers.stream()
                .filter(value -> ((net.fabricmc.loader.api.entrypoint.EntrypointContainer<?>) value)
                        .getProvider().getMetadata().getId().equals("visual-keymap"))
                .findFirst()
                .orElse(null);
        check(container != null, "Visual Keymap Mod Menu entrypoint is missing");
        Object api = ((net.fabricmc.loader.api.entrypoint.EntrypointContainer<?>) container).getEntrypoint();
        Object factory = apiClass.getMethod("getModConfigScreenFactory").invoke(api);
        Class<?> factoryClass = Class.forName("com.terraformersmc.modmenu.api.ConfigScreenFactory");
        Object created = factoryClass.getMethod("create", Screen.class).invoke(factory, parent);
        check(created instanceof VisualKeymapScreen,
                "Mod Menu factory returned " + created.getClass().getName());
    }

    private static void verifyExternalProvider(FabricLoader loader, List<VisualKeymapApi<?>> providers)
            throws Exception {
        check(loader.getEntrypointContainers("visualkeymap", VisualKeymapApi.class).size() == 2,
                "Expected valid and throwing external entrypoints");
        check(providers.stream().noneMatch(api -> api.getClass().getName().contains("ThrowingProvider")),
                "Throwing provider was not isolated");
        Phase4Provider provider = providers.stream()
                .filter(Phase4Provider.class::isInstance)
                .map(Phase4Provider.class::cast)
                .findFirst()
                .orElseThrow();
        Phase4Provider.Binding binding = provider.getKeyBindings().getFirst();
        check(binding.getFullKeyCodes().equals(List.of(341, 66)), "External modifier/key semantics changed");
        binding.setBoundKeys(List.of(InputConstants.getKey("key.keyboard.a")));
        check(binding.getKeyCodes().equals(List.of(65)), "External edit callback failed");
        binding.setBoundKeys(List.of(InputConstants.getKey("key.keyboard.b")));
        KeymapSnapshot snapshot = KeymapSnapshot.fromBindings(List.of(provider));
        check(snapshot.version() == 1 && snapshot.bindings().size() == 1,
                "External version 1 snapshot changed");
        int savesBefore = Phase4Provider.saveCalls;
        VisualKeymap.saveKeyBindings();
        check(Phase4Provider.saveCalls == savesBefore + 1, "External save callback was not called exactly once");
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().withUpperCase().formatHex(digest.digest(Files.readAllBytes(path)));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class ProbeParent extends Screen {
        private ProbeParent() {
            super(Component.literal("Phase 4 parent"));
        }
    }
}
