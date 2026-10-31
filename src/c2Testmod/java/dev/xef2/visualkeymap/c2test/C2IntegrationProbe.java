package dev.xef2.visualkeymap.c2test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.config.Macro;
import dev.terminalmc.commandkeys.config.Message;
import dev.terminalmc.commandkeys.config.Profile;
import dev.xef2.visualkeymap.KeymapSnapshot;
import dev.xef2.visualkeymap.integration.CommandKeysIntegration;
import dev.xef2.visualkeymap.input.MinecraftInputCode;
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
                    case "exercise" -> exerciseAdapter();
                    case "verify" -> verifyPersistedState();
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

    private static List<String> exerciseAdapter() {
        Config config = Config.resetAndSave();
        Profile alpha = config.activeProfile();
        alpha.name = "C2 Alpha";

        Macro named = macro("Named Macro", "alpha primary", "alpha searchable tail");
        Macro blank = macro("", "   ", "blank fallback");
        Macro single = macro("Single Macro", "single search needle");
        Macro mouse = macro("Mouse Macro", "mouse binding");
        alpha.addMacro(named);
        alpha.addMacro(blank);
        alpha.addMacro(single);
        alpha.addMacro(mouse);
        setMacroKeys(alpha, named, List.of(297));
        setMacroKeys(alpha, blank, List.of(345, 291));
        setMacroKeys(alpha, single, List.of(75));
        setMacroKeys(alpha, mouse, List.of(0));

        Profile beta = config.addNewProfile();
        beta.name = "C2 Beta";
        Macro betaMacro = macro("Beta Macro", "beta only");
        beta.addMacro(betaMacro);
        setMacroKeys(beta, betaMacro, List.of(66));

        CommandKeysIntegration api = new CommandKeysIntegration();
        List<CommandKeysIntegration.CommandKeysKeyBinding> alphaBindings = api.getKeyBindings();
        check(alphaBindings.stream().map(CommandKeysIntegration.CommandKeysKeyBinding::getId).toList().equals(List.of(
                "C2 Alpha:Named Macro", "C2 Alpha:", "C2 Alpha:Single Macro", "C2 Alpha:Mouse Macro"
        )), "Alpha macro enumeration or order changed: " + alphaBindings.stream()
                .map(CommandKeysIntegration.CommandKeysKeyBinding::getId).toList());

        CommandKeysIntegration.CommandKeysKeyBinding namedBinding = binding(api, "C2 Alpha:Named Macro");
        CommandKeysIntegration.CommandKeysKeyBinding blankBinding = binding(api, "C2 Alpha:");
        CommandKeysIntegration.CommandKeysKeyBinding singleBinding = binding(api, "C2 Alpha:Single Macro");
        CommandKeysIntegration.CommandKeysKeyBinding mouseBinding = binding(api, "C2 Alpha:Mouse Macro");
        check(namedBinding.getDisplayName().getString().contains("Named Macro"),
                "Non-empty macro name is missing from display name");
        check(!blankBinding.getDisplayName().getString().endsWith(" - "),
                "Empty macro name added a separator");
        check(namedBinding.getComment().getString().equals("alpha primary [+1]"),
                "Multi-message comment changed: " + namedBinding.getComment().getString());
        check(blankBinding.getComment().getString().equals("... [+1]"),
                "Blank first message or multi-message suffix changed");
        check(namedBinding.containsSearchText("searchable tail"),
                "Secondary macro message is not searchable");
        check(singleBinding.containsSearchText("single needle"),
                "Macro message multi-term search changed");
        check(!namedBinding.containsSearchText("beta only"),
                "Search leaked messages from another profile");

        namedBinding.setBoundKeys(decode(List.of(74)));
        blankBinding.setBoundKeys(decode(List.of(341, 290)));
        assertCanonical(namedBinding, List.of(74));
        assertCanonical(blankBinding, List.of(341, 290));
        assertCanonical(mouseBinding, List.of(0));
        check(named.getKeybind().getLimitKey().equals(InputConstants.UNKNOWN),
                "Single-key binding unexpectedly set a limit key");
        check(named.getKeybind().getKey().getName().equals("key.keyboard.j"),
                "Single-key binding did not set J as the main key");
        check(blank.getKeybind().getLimitKey().getName().equals("key.keyboard.left.control"),
                "Two-key binding did not set the first key as limit key");
        check(blank.getKeybind().getKey().getName().equals("key.keyboard.f1"),
                "Two-key binding did not set the second key as main key");
        check(mouse.getKeybind().getKey().getType() == InputConstants.Type.MOUSE
                        && mouse.getKeybind().getKey().getValue() == 1,
                "Mouse binding did not decode canonical left click to SDL mouse button 1");

        namedBinding.resetToDefault();
        check(namedBinding.isDefault(), "CommandKeys reset did not clear the binding");
        check(named.getKeybind().getKey().equals(InputConstants.UNKNOWN)
                        && named.getKeybind().getLimitKey().equals(InputConstants.UNKNOWN),
                "CommandKeys reset did not clear main and limit keys");
        namedBinding.setBoundKeys(decode(List.of(74)));
        singleBinding.resetToDefault();
        check(singleBinding.isDefault() && singleBinding.getKeyCodes().isEmpty(),
                "CommandKeys clear did not produce an unbound macro");

        check(api.getProviderName().isEmpty(), "CommandKeys provider contract changed");
        check(KeymapSnapshot.fromBindings(List.of(api)).bindings().isEmpty(),
                "CommandKeys unexpectedly participated in version 1 snapshot export");
        api.save();

        config.activateProfile(config.getProfiles().indexOf(beta));
        List<CommandKeysIntegration.CommandKeysKeyBinding> betaBindings = api.getKeyBindings();
        check(betaBindings.size() == 1 && betaBindings.getFirst().getId().equals("C2 Beta:Beta Macro"),
                "Active-profile enumeration leaked Alpha macros: " + betaBindings.stream()
                        .map(CommandKeysIntegration.CommandKeysKeyBinding::getId).toList());
        config.activateProfile(config.getProfiles().indexOf(alpha));
        api.save();

        return new ArrayList<>(List.of(
                "activeProfile=C2 Alpha",
                "alpha.ids=" + alphaBindings.stream().map(CommandKeysIntegration.CommandKeysKeyBinding::getId).toList(),
                "beta.ids=" + betaBindings.stream().map(CommandKeysIntegration.CommandKeysKeyBinding::getId).toList(),
                "single.canonical=[74]",
                "single.raw=[key.keyboard.j]",
                "combination.canonical=[341, 290]",
                "combination.raw=[key.keyboard.left.control, key.keyboard.f1]",
                "mouse.canonical=[0]",
                "mouse.raw=[key.mouse.left]",
                "blank.comment=... [+1]",
                "multi.comment=alpha primary [+1]",
                "secondaryMessageSearch=true",
                "resetClears=true",
                "savedUnbound=[]",
                "snapshotBindings=0"
        ));
    }

    private static List<String> verifyPersistedState() throws Exception {
        Config config = Config.get();
        check(config.activeProfile().name.equals("C2 Alpha"),
                "Active profile did not persist: " + config.activeProfile().name);
        CommandKeysIntegration api = new CommandKeysIntegration();
        List<CommandKeysIntegration.CommandKeysKeyBinding> bindings = api.getKeyBindings();
        check(bindings.size() == 4, "Persisted Alpha macro count changed: " + bindings.size());
        assertCanonical(binding(api, "C2 Alpha:Named Macro"), List.of(74));
        assertCanonical(binding(api, "C2 Alpha:"), List.of(341, 290));
        assertCanonical(binding(api, "C2 Alpha:Single Macro"), List.of());
        check(binding(api, "C2 Alpha:Single Macro").isDefault(),
                "Cleared CommandKeys binding did not persist as default");
        assertCanonical(binding(api, "C2 Alpha:Mouse Macro"), List.of(0));
        check(binding(api, "C2 Alpha:Named Macro").containsSearchText("searchable tail"),
                "Secondary message search did not survive restart");
        check(KeymapSnapshot.fromBindings(List.of(api)).bindings().isEmpty(),
                "CommandKeys entered snapshot export after restart");

        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(Config.FILE_NAME);
        String configJson = Files.readString(configFile);
        check(configJson.contains("C2 Alpha") && configJson.contains("key.keyboard.j")
                        && configJson.contains("key.keyboard.left.control") && configJson.contains("key.keyboard.f1"),
                "Saved CommandKeys config is missing expected profile or key names");

        return new ArrayList<>(List.of(
                "activeProfile=" + config.activeProfile().name,
                "persisted.ids=" + bindings.stream().map(CommandKeysIntegration.CommandKeysKeyBinding::getId).toList(),
                "persisted.single=[74]",
                "persisted.combination=[341, 290]",
                "persisted.unbound=[]",
                "persisted.mouse=[0]",
                "persisted.search=true",
                "snapshotBindings=0",
                "config=" + configFile.toAbsolutePath()
        ));
    }

    private static Macro macro(String name, String firstMessage, String... additionalMessages) {
        Macro macro = new Macro();
        macro.name = name;
        Message first = new Message();
        first.string = firstMessage;
        macro.addMessage(first);
        for (String value : additionalMessages) {
            Message message = new Message();
            message.string = value;
            macro.addMessage(message);
        }
        return macro;
    }

    private static void setMacroKeys(Profile profile, Macro macro, List<Integer> canonicalCodes) {
        List<InputConstants.Key> keys = decode(canonicalCodes);
        InputConstants.Key limitKey = keys.size() > 1 ? keys.getFirst() : InputConstants.UNKNOWN;
        InputConstants.Key key = keys.isEmpty() ? InputConstants.UNKNOWN : keys.getLast();
        profile.setKey(macro, macro.getKeybind(), key);
        profile.setLimitKey(macro, macro.getKeybind(), limitKey);
    }

    private static CommandKeysIntegration.CommandKeysKeyBinding binding(
            CommandKeysIntegration api,
            String id
    ) {
        return api.getKeyBindings().stream()
                .filter(binding -> binding.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing CommandKeys binding: " + id));
    }

    private static List<InputConstants.Key> decode(List<Integer> canonicalCodes) {
        return canonicalCodes.stream()
                .map(code -> MinecraftInputCode.decode(code).orElseThrow())
                .toList();
    }

    private static void assertCanonical(
            CommandKeysIntegration.CommandKeysKeyBinding binding,
            List<Integer> expected
    ) {
        check(binding.getKeyCodes().equals(expected),
                binding.getId() + " expected canonical " + expected + " but got " + binding.getKeyCodes());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
