package dev.xef2.visualkeymap.c3btest;

import com.example.mymod.MyModVisualKeymapImpl;
import com.example.mymod.MyModVisualKeymapImpl.ExternalBinding;
import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.KeymapSnapshot;
import dev.xef2.visualkeymap.VisualKeymap;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Development-only C3-B runtime probe, compiled unchanged on Minecraft 26.2 and 26.3. */
public final class PublicApiProbe implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            FabricLoader loader = FabricLoader.getInstance();
            Path result = loader.getGameDir().resolve("c3b-api-probe.txt");
            try {
                List<String> evidence = exerciseApi(loader, result.getParent());
                evidence.add(0, "status=PASS");
                evidence.add(1, "gameDir=" + loader.getGameDir().toAbsolutePath());
                evidence.add(2, "minecraft=" + loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion());
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
                if (!Boolean.getBoolean("visualkeymap.c3.manual")) {
                    client.stop();
                }
            }
        });
    }

    private static List<String> exerciseApi(FabricLoader loader, Path outputDirectory) throws Exception {
        List<EntrypointContainer<VisualKeymapApi>> containers = loader
                .getEntrypointContainers("visualkeymap", VisualKeymapApi.class);
        check(containers.size() == 2, "Expected valid and throwing visualkeymap entrypoints: " + containers.size());

        List<VisualKeymapApi<?>> apis = VisualKeymap.getApiImplementations();
        check(apis.get(0).getProviderName().equals("minecraft"), "Minecraft provider is not first");
        MyModVisualKeymapImpl external = apis.stream()
                .filter(MyModVisualKeymapImpl.class::isInstance)
                .map(MyModVisualKeymapImpl.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Valid external provider was not initialized after throwing entrypoint"));
        check(apis.stream().noneMatch(api -> api.getClass().getName().contains("ThrowingVisualKeymapImpl")),
                "Throwing entrypoint leaked into provider list");
        check(apis.indexOf(external) == 1, "External provider order changed: " + providerClasses(apis));

        List<ExternalBinding> bindings = external.getKeyBindings();
        check(bindings.stream().map(ExternalBinding::getId).toList()
                        .equals(List.of("alpha", "conflict", "empty", "four")),
                "Binding IDs/order changed");
        ExternalBinding alpha = binding(bindings, "alpha");
        ExternalBinding conflict = binding(bindings, "conflict");
        ExternalBinding empty = binding(bindings, "empty");
        ExternalBinding four = binding(bindings, "four");

        check(alpha.getKeyCodes().equals(List.of(66)), "Initial key list changed");
        check(alpha.getModifierKeyCodes().equals(List.of(341)), "Modifier semantics changed");
        check(alpha.getFullKeyCodes().equals(List.of(341, 66)), "Full key order changed");
        check(alpha.getOrder() == 42, "Order changed");
        check(alpha.getMaxBoundKeys() == 4, "maxBoundKeys changed");
        check(alpha.getDisplayName().getString().equals("External Category - Alpha Action"),
                "Display name changed: " + alpha.getDisplayName().getString());
        check(alpha.getComment().getString().equals("Alpha comment"), "Comment changed");
        check(alpha.containsSearchText("external needle"), "Custom searchable strings were not used");
        check(alpha.containsSearchText("category alpha"), "Base category/name search changed");
        check(four.getKeyCodes().size() == 4, "Four-key contract changed");
        check(empty.getKeyCodes().isEmpty(), "Empty binding contract changed");
        check(KeyBinding.getConflictBindings(List.of(alpha, conflict)).size() == 1,
                "Conflict grouping no longer uses full keys/order");

        alpha.setBoundKeys(List.of(InputConstants.getKey("key.keyboard.a")));
        check(alpha.getKeyCodes().equals(List.of(65)), "External edit callback did not preserve canonical key");
        check(alpha.getSetCalls() == 1, "setBoundKeys callback count changed");
        alpha.resetToDefault();
        check(alpha.getKeyCodes().equals(List.of(65)) && alpha.isDefault(), "Reset/default semantics changed");
        check(alpha.getResetCalls() == 1, "reset callback count changed");

        conflict.setBoundKeys(List.of(InputConstants.getKey("key.keyboard.d")));
        KeymapSnapshot snapshot = KeymapSnapshot.fromBindings(List.of(external));
        check(snapshot.version() == 1, "Snapshot schema version changed");
        check(snapshot.bindings().size() == 1, "Snapshot export/default filtering changed: " + snapshot.bindings());
        KeymapSnapshot.BindingEntry entry = snapshot.bindings().getFirst();
        check(entry.provider().equals(MyModVisualKeymapImpl.PROVIDER), "Snapshot provider changed");
        check(entry.id().equals("conflict"), "Snapshot binding ID changed");
        check(entry.keys().equals(List.of(68)), "Snapshot canonical keys changed");
        check(entry.modifiers().equals(List.of(341)), "Snapshot modifiers changed");

        Path exported = outputDirectory.resolve("c3b-external-v1.json");
        snapshot.writeToFile(exported.toFile());
        KeymapSnapshot reread = KeymapSnapshot.readFromFile(exported.toFile());
        List<KeymapSnapshot.ImportMatch> matches = reread.matchBindings(List.of(external));
        check(matches.size() == 1 && matches.getFirst().binding == conflict,
                "Snapshot provider/ID matching changed");
        conflict.setBoundKeys(matches.getFirst().getKeysFromEntry());
        check(conflict.getKeyCodes().equals(List.of(68)), "Snapshot typed-key apply changed canonical value");
        check(conflict.getModifierKeyCodes().equals(List.of(341)),
                "Version 1 modifier non-application semantics changed");

        int savesBefore = MyModVisualKeymapImpl.saveCalls;
        VisualKeymap.saveKeyBindings();
        check(MyModVisualKeymapImpl.saveCalls == savesBefore + 1, "Provider save callback was not invoked");
        check(VisualKeymap.getKeyBindings().stream().anyMatch(binding -> binding == alpha),
                "VisualKeymap.getKeyBindings() omitted external bindings");

        return new ArrayList<>(List.of(
                "visualkeymap.entrypoints=2",
                "throwing.entrypoint.isolated=true",
                "providers=" + providerClasses(apis),
                "external.provider=" + external.getProviderName(),
                "external.ids=" + bindings.stream().map(ExternalBinding::getId).toList(),
                "external.modifiers=" + alpha.getModifierKeyCodes(),
                "external.order=" + alpha.getOrder(),
                "external.edit.reset=true",
                "external.save=true",
                "external.snapshot=" + entry.provider() + "|" + entry.id() + "|" + entry.keys() + "|" + entry.modifiers()
        ));
    }

    private static ExternalBinding binding(List<ExternalBinding> bindings, String id) {
        return bindings.stream().filter(binding -> binding.getId().equals(id)).findFirst().orElseThrow();
    }

    private static List<String> providerClasses(List<VisualKeymapApi<?>> apis) {
        return apis.stream().map(api -> api.getClass().getName()).toList();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
