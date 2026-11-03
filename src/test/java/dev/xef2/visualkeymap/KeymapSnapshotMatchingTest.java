package dev.xef2.visualkeymap;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;
import dev.xef2.visualkeymap.input.CanonicalInputCode;
import dev.xef2.visualkeymap.input.MinecraftInputCode;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeymapSnapshotMatchingTest {

    @Test
    void realExportRoundTripHasNoChangesOrWrites() {
        Scenario scenario = realExportScenario(false);

        for (int iteration = 0; iteration < 3; iteration++) {
            List<KeymapSnapshot.ImportMatch> preview = preview(scenario.snapshot, scenario.apis);
            assertEquals(0, preview.size(), "an unchanged real export must not produce a preview");
            applySelected(preview);
        }

        scenario.duplicateBindings.forEach(binding -> {
            assertEquals(0, binding.writes.size(), binding.owner + " must not receive a no-op write");
            assertEquals(binding.initialKeys, binding.getKeyCodes());
        });
    }

    @Test
    void uniqueIdentityStillRestoresChangedKeys() {
        ProbeBinding binding = binding("Unique", "open", List.of(66));
        KeymapSnapshot snapshot = snapshot(entry("provider-a", "open", List.of(65)));

        List<KeymapSnapshot.ImportMatch> preview = preview(snapshot, List.of(api("provider-a", binding)));

        assertEquals(1, preview.size());
        assertTrue(preview.getFirst().selected);
        applySelected(preview);
        assertEquals(List.of(65), binding.getKeyCodes());
        assertEquals(List.of(List.of(65)), binding.writes);
    }

    @Test
    void changedDuplicateIdentityIsAmbiguousAndDefaultsUnselected() {
        Scenario scenario = realExportScenario(false);
        scenario.duplicateBindings.getFirst().keys = new ArrayList<>(List.of(257, 48));

        List<KeymapSnapshot.ImportMatch> preview = preview(scenario.snapshot, scenario.apis);

        assertEquals(1, preview.size(), "three exact pairs leave one candidate edge");
        assertFalse(preview.getFirst().selected, "v1 cannot prove the remaining owner identity");
        applySelected(preview);
        scenario.duplicateBindings.forEach(binding -> assertEquals(0, binding.writes.size()));
    }

    @Test
    void changedDuplicateToAnExistingKeyDoesNotChooseAnOwnerByEnumerationOrder() {
        ProbeBinding first = binding("Mod A", "open", List.of(66));
        ProbeBinding second = binding("Mod B", "open", List.of(66));
        KeymapSnapshot snapshot = snapshot(
                entry("malilib", "open", List.of(65)),
                entry("malilib", "open", List.of(66))
        );

        List<KeymapSnapshot.ImportMatch> preview = preview(
                snapshot, List.of(api("malilib", List.of(first, second)))
        );

        assertEquals(2, preview.size(), "either live binding may be the one that originally used key 65");
        assertTrue(preview.stream().allMatch(match -> match.entry.keys().equals(List.of(65))));
        assertTrue(preview.stream().anyMatch(match -> match.binding == first));
        assertTrue(preview.stream().anyMatch(match -> match.binding == second));
        assertTrue(preview.stream().noneMatch(match -> match.selected));
    }

    @Test
    void modifiersOnlyDifferenceDoesNotBecomeAKeyChange() {
        ProbeBinding binding = binding("Modifier test", "debug", List.of(65));
        binding.modifiers = new ArrayList<>(List.of(341));
        KeymapSnapshot snapshot = snapshot(new KeymapSnapshot.BindingEntry(
                "provider-a", "debug", List.of(65), List.of(340)
        ));

        List<KeymapSnapshot.ImportMatch> matches = snapshot.matchBindings(List.of(api("provider-a", binding)));

        assertEquals(1, matches.size());
        assertEquals(0, preview(snapshot, List.of(api("provider-a", binding))).size());
        applySelected(preview(snapshot, List.of(api("provider-a", binding))));
        assertEquals(0, binding.writes.size());
    }

    @Test
    void partialDuplicateExportDoesNotGuessAnOwner() {
        List<ProbeBinding> live = List.of(
                binding("Bedrock-Miner", "openConfigGui", List.of(257, 55)),
                binding("Item Scroller", "openConfigGui", List.of(257, 54)),
                binding("MiniHUD", "openConfigGui", List.of(257, 51)),
                binding("Tweakeroo", "openConfigGui", List.of(257, 49))
        );
        KeymapSnapshot snapshot = snapshot(entry("malilib", "openConfigGui", List.of(257, 48)));

        List<KeymapSnapshot.ImportMatch> preview = preview(snapshot, List.of(api("malilib", live)));

        assertEquals(4, preview.size());
        assertTrue(preview.stream().noneMatch(match -> match.selected));
    }

    @Test
    void unchangedPartialDuplicateExportDoesNotProduceFalseChanges() {
        List<ProbeBinding> live = List.of(
                binding("Modified binding", "openConfigGui", List.of(65)),
                binding("Default binding omitted from export", "openConfigGui", List.of(66))
        );
        KeymapSnapshot snapshot = snapshot(entry("malilib", "openConfigGui", List.of(65)));

        assertEquals(0, preview(snapshot, List.of(api("malilib", live))).size());
    }

    @Test
    void duplicateEqualKeysRoundTripWithoutChanges() {
        List<ProbeBinding> live = List.of(
                binding("Mod A", "open", List.of(65)),
                binding("Mod B", "open", List.of(65))
        );
        KeymapSnapshot snapshot = snapshot(
                entry("malilib", "open", List.of(65)),
                entry("malilib", "open", List.of(65))
        );

        assertEquals(0, preview(snapshot, List.of(api("malilib", live))).size());
    }

    @Test
    void missingDuplicateOwnerDoesNotRetargetItsEntry() {
        List<ProbeBinding> live = List.of(
                binding("Bedrock-Miner", "openConfigGui", List.of(257, 55)),
                binding("Item Scroller", "openConfigGui", List.of(257, 54)),
                binding("MiniHUD", "openConfigGui", List.of(257, 48))
        );
        KeymapSnapshot snapshot = snapshot(
                entry("malilib", "openConfigGui", List.of(257, 55)),
                entry("malilib", "openConfigGui", List.of(257, 54)),
                entry("malilib", "openConfigGui", List.of(257, 51)),
                entry("malilib", "openConfigGui", List.of(257, 49))
        );

        List<KeymapSnapshot.ImportMatch> preview = preview(snapshot, List.of(api("malilib", live)));

        assertEquals(2, preview.size());
        assertTrue(preview.stream().noneMatch(match -> match.selected));
    }

    @Test
    void differentProvidersWithTheSameIdRemainIndependent() {
        ProbeBinding first = binding("Provider A", "open", List.of(66));
        ProbeBinding second = binding("Provider B", "open", List.of(68));
        KeymapSnapshot snapshot = snapshot(
                entry("provider-a", "open", List.of(65)),
                entry("provider-b", "open", List.of(67))
        );

        List<KeymapSnapshot.ImportMatch> preview = preview(snapshot, List.of(
                api("provider-a", first), api("provider-b", second)
        ));

        assertEquals(2, preview.size());
        assertTrue(preview.stream().allMatch(match -> match.selected));
        applySelected(preview);
        assertEquals(List.of(65), first.getKeyCodes());
        assertEquals(List.of(67), second.getKeyCodes());
    }

    @Test
    void mouseAndMultiKeyTargetsRemainCanonicalAfterApplication() {
        ProbeBinding binding = binding("Canonical", "combo", List.of(66));
        KeymapSnapshot snapshot = snapshot(entry("provider-a", "combo", List.of(0, 257, 55)));

        List<KeymapSnapshot.ImportMatch> preview = preview(snapshot, List.of(api("provider-a", binding)));

        assertEquals(1, preview.size());
        assertTrue(preview.getFirst().canApply());
        applySelected(preview);
        assertEquals(List.of(0, 257, 55), binding.getKeyCodes());
        assertEquals(List.of(List.of(0, 257, 55)), binding.writes);
    }

    @Test
    void f25AndUnknownExtensionRemainUnapplicable() {
        ProbeBinding f25Binding = binding("F25", "f25", List.of(65));
        ProbeBinding unknownBinding = binding("Unknown", "unknown", List.of(66));
        KeymapSnapshot snapshot = snapshot(
                entry("provider-a", "f25", List.of(CanonicalInputCode.F25_TOMBSTONE)),
                entry("provider-a", "unknown", List.of(CanonicalInputCode.EXTENSION_NAMESPACE_START | 128))
        );

        List<KeymapSnapshot.ImportMatch> preview = preview(snapshot, List.of(api(
                "provider-a", List.of(f25Binding, unknownBinding)
        )));

        assertEquals(2, preview.size());
        assertTrue(preview.stream().noneMatch(KeymapSnapshot.ImportMatch::canApply));
        assertThrows(IllegalStateException.class, preview.getFirst()::getKeysFromEntry);
        applySelected(preview);
        assertTrue(f25Binding.writes.isEmpty());
        assertTrue(unknownBinding.writes.isEmpty());
    }

    @Test
    void reversedLiveEnumerationDoesNotChangeRoundTripResult() {
        Scenario normal = realExportScenario(false);
        Scenario reversed = realExportScenario(true);

        assertEquals(0, preview(normal.snapshot, normal.apis).size());
        assertEquals(0, preview(reversed.snapshot, reversed.apis).size());
    }

    @Test
    void emptyAndDefaultBindingsKeepExistingExportSemantics() {
        ProbeBinding modifiedEmpty = binding("Modified empty", "empty", List.of());
        ProbeBinding defaultBinding = binding("Default", "default", List.of(65));
        defaultBinding.isDefault = true;
        ProbeApi api = api("provider-a", List.of(modifiedEmpty, defaultBinding));

        KeymapSnapshot snapshot = KeymapSnapshot.fromBindings(List.of(api));

        assertEquals(1, snapshot.bindings().size());
        assertEquals("empty", snapshot.bindings().getFirst().id());
        assertEquals(List.of(), snapshot.bindings().getFirst().keys());
        assertEquals(0, preview(snapshot, List.of(api)).size());
    }

    public static void main(String[] args) {
        KeymapSnapshotMatchingTest test = new KeymapSnapshotMatchingTest();
        test.realExportRoundTripHasNoChangesOrWrites();
        test.uniqueIdentityStillRestoresChangedKeys();
        test.changedDuplicateIdentityIsAmbiguousAndDefaultsUnselected();
        test.changedDuplicateToAnExistingKeyDoesNotChooseAnOwnerByEnumerationOrder();
        test.modifiersOnlyDifferenceDoesNotBecomeAKeyChange();
        test.partialDuplicateExportDoesNotGuessAnOwner();
        test.unchangedPartialDuplicateExportDoesNotProduceFalseChanges();
        test.duplicateEqualKeysRoundTripWithoutChanges();
        test.missingDuplicateOwnerDoesNotRetargetItsEntry();
        test.differentProvidersWithTheSameIdRemainIndependent();
        test.mouseAndMultiKeyTargetsRemainCanonicalAfterApplication();
        test.f25AndUnknownExtensionRemainUnapplicable();
        test.reversedLiveEnumerationDoesNotChangeRoundTripResult();
        test.emptyAndDefaultBindingsKeepExistingExportSemantics();
    }

    private static List<KeymapSnapshot.ImportMatch> preview(KeymapSnapshot snapshot,
                                                            List<VisualKeymapApi<?>> apis) {
        return snapshot.matchBindings(apis).stream()
                .filter(match -> !match.binding.getKeyCodes().equals(match.entry.keys()))
                .toList();
    }

    private static void applySelected(List<KeymapSnapshot.ImportMatch> matches) {
        matches.stream().filter(match -> match.selected && match.canApply())
                .forEach(match -> match.binding.setBoundKeys(match.getKeysFromEntry()));
    }

    private static Scenario realExportScenario(boolean reverseMalilib) {
        ProbeBinding minecraft = binding("Minecraft", "visualkeymap.key.open_keymap", List.of(297));
        List<ProbeBinding> malilib = new ArrayList<>(List.of(
                binding("Bedrock-Miner", "openConfigGui", List.of(257, 55)),
                binding("Item Scroller", "openConfigGui", List.of(257, 54)),
                binding("Litematica", "openGuiSettings", List.of(257, 52)),
                binding("MiniHUD", "openConfigGui", List.of(257, 51)),
                binding("Tweakeroo", "openConfigGui", List.of(257, 49)),
                binding("Tweakeroo", "tweakFreeCamera", List.of(74)),
                binding("Tweakeroo", "tweakHoldAttack", List.of(298)),
                binding("Litematica Printer", "openScreen", List.of(257, 53)),
                binding("TweakerMore", "openTweakerMoreConfigGui", List.of(257, 50))
        ));
        List<ProbeBinding> duplicates = malilib.stream()
                .filter(binding -> binding.getId().equals("openConfigGui"))
                .toList();
        if (reverseMalilib) {
            Collections.reverse(malilib);
        }
        KeymapSnapshot snapshot = snapshot(
                entry("minecraft", "visualkeymap.key.open_keymap", List.of(297)),
                entry("malilib", "openConfigGui", List.of(257, 55)),
                entry("malilib", "openConfigGui", List.of(257, 54)),
                entry("malilib", "openGuiSettings", List.of(257, 52)),
                entry("malilib", "openConfigGui", List.of(257, 51)),
                entry("malilib", "openConfigGui", List.of(257, 49)),
                entry("malilib", "tweakFreeCamera", List.of(74)),
                entry("malilib", "tweakHoldAttack", List.of(298)),
                entry("malilib", "openScreen", List.of(257, 53)),
                entry("malilib", "openTweakerMoreConfigGui", List.of(257, 50))
        );
        return new Scenario(snapshot, List.of(api("minecraft", minecraft), api("malilib", malilib)), duplicates);
    }

    private static KeymapSnapshot snapshot(KeymapSnapshot.BindingEntry... entries) {
        return new KeymapSnapshot(1, List.of(entries));
    }

    private static KeymapSnapshot.BindingEntry entry(String provider, String id, List<Integer> keys) {
        return new KeymapSnapshot.BindingEntry(provider, id, keys, List.of());
    }

    private static ProbeBinding binding(String owner, String id, List<Integer> keys) {
        return new ProbeBinding(owner, id, keys);
    }

    private static ProbeApi api(String provider, ProbeBinding binding) {
        return api(provider, List.of(binding));
    }

    private static ProbeApi api(String provider, List<ProbeBinding> bindings) {
        return new ProbeApi(provider, bindings);
    }

    private static final class ProbeBinding extends KeyBinding {
        private final String owner;
        private final String id;
        private final List<Integer> initialKeys;
        private List<Integer> keys;
        private List<Integer> modifiers = new ArrayList<>();
        private boolean isDefault;
        private final List<List<Integer>> writes = new ArrayList<>();

        private ProbeBinding(String owner, String id, List<Integer> keys) {
            super(Component.literal(owner), Component.literal(id), 4);
            this.owner = owner;
            this.id = id;
            this.initialKeys = List.copyOf(keys);
            this.keys = new ArrayList<>(keys);
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public List<Integer> getKeyCodes() {
            return List.copyOf(this.keys);
        }

        @Override
        public List<Integer> getModifierKeyCodes() {
            return List.copyOf(this.modifiers);
        }

        @Override
        public void setBoundKeys(List<InputConstants.Key> keys) {
            this.keys = keys.stream().map(MinecraftInputCode::requireCanonical)
                    .collect(ArrayList::new, List::add, List::addAll);
            this.writes.add(List.copyOf(this.keys));
        }

        @Override
        public boolean isDefault() {
            return this.isDefault;
        }

        @Override
        public void resetToDefault() {
        }
    }

    private record ProbeApi(String provider, List<ProbeBinding> bindings)
            implements VisualKeymapApi<ProbeBinding> {
        @Override
        public List<ProbeBinding> getKeyBindings() {
            return this.bindings;
        }

        @Override
        public void save() {
        }

        @Override
        public String getProviderName() {
            return this.provider;
        }
    }

    private record Scenario(KeymapSnapshot snapshot, List<VisualKeymapApi<?>> apis,
                            List<ProbeBinding> duplicateBindings) {
    }
}
