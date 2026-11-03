package dev.xef2.visualkeymap.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.KeymapSnapshot;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.input.CanonicalInputCode;
import dev.xef2.visualkeymap.input.MinecraftInputCode;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportSelectionTest {

    @Test
    void selectingCandidatesEnforcesOneToOneMappingInBothDirections() {
        Fixture fixture = fixture();

        ImportSelection.setSelected(fixture.matches, fixture.firstToFirst, true);
        assertTrue(fixture.firstToFirst.selected);

        ImportSelection.setSelected(fixture.matches, fixture.firstToSecond, true);
        assertFalse(fixture.firstToFirst.selected, "one snapshot entry cannot target two live bindings");
        assertTrue(fixture.firstToSecond.selected);

        ImportSelection.setSelected(fixture.matches, fixture.secondToSecond, true);
        assertFalse(fixture.firstToSecond.selected, "one live binding cannot receive two snapshot entries");
        assertTrue(fixture.secondToSecond.selected);

        ImportSelection.setSelected(fixture.matches, fixture.firstToFirst, true);
        assertTrue(fixture.firstToFirst.selected);
        assertTrue(fixture.secondToSecond.selected, "independent edges may remain selected together");
    }

    @Test
    void sanitizeDisablesEveryMemberOfAnExistingConflict() {
        Fixture fixture = fixture();
        fixture.firstToFirst.selected = true;
        fixture.firstToSecond.selected = true;

        ImportSelection.sanitize(fixture.matches);

        assertFalse(fixture.firstToFirst.selected);
        assertFalse(fixture.firstToSecond.selected);
    }

    @Test
    void manualSelectionAppliesEachEntryAndBindingAtMostOnce() {
        Fixture fixture = fixture();
        ImportSelection.setSelected(fixture.matches, fixture.firstToFirst, true);
        ImportSelection.setSelected(fixture.matches, fixture.secondToSecond, true);

        fixture.matches.stream().filter(match -> match.selected)
                .forEach(match -> match.binding.setBoundKeys(match.getKeysFromEntry()));

        assertEquals(List.of(List.of(65)), fixture.firstBinding.writes);
        assertEquals(List.of(List.of(66)), fixture.secondBinding.writes);
    }

    @Test
    void leavingAllAmbiguousCandidatesUnselectedDoesNotWriteAnything() {
        Fixture fixture = fixture();

        fixture.matches.stream().filter(match -> match.selected)
                .forEach(match -> match.binding.setBoundKeys(match.getKeysFromEntry()));

        assertEquals(0, fixture.firstBinding.writes.size());
        assertEquals(0, fixture.secondBinding.writes.size());
    }

    @Test
    void unsupportedCandidatesCannotBeSelectedOrApplied() {
        ProbeBinding binding = new ProbeBinding("Unsupported", "open", List.of(65));
        KeymapSnapshot.ImportMatch f25 = match(binding, new KeymapSnapshot.BindingEntry(
                "malilib", "open", List.of(CanonicalInputCode.F25_TOMBSTONE), List.of()
        ));
        KeymapSnapshot.ImportMatch unknown = match(binding, new KeymapSnapshot.BindingEntry(
                "malilib", "open", List.of(CanonicalInputCode.EXTENSION_NAMESPACE_START | 128), List.of()
        ));
        f25.selected = true;
        unknown.selected = true;
        List<KeymapSnapshot.ImportMatch> matches = List.of(f25, unknown);

        ImportSelection.sanitize(matches);
        assertFalse(f25.selected);
        assertFalse(unknown.selected);

        ImportSelection.setSelected(matches, f25, true);
        ImportSelection.setSelected(matches, unknown, true);
        assertFalse(f25.selected, "F25 tombstone must remain unselectable");
        assertFalse(unknown.selected, "unknown extension must remain unselectable");

        matches.stream().filter(match -> match.selected && match.canApply())
                .forEach(match -> match.binding.setBoundKeys(match.getKeysFromEntry()));
        assertTrue(binding.writes.isEmpty());
    }

    public static void main(String[] args) {
        ImportSelectionTest test = new ImportSelectionTest();
        test.selectingCandidatesEnforcesOneToOneMappingInBothDirections();
        test.sanitizeDisablesEveryMemberOfAnExistingConflict();
        test.manualSelectionAppliesEachEntryAndBindingAtMostOnce();
        test.leavingAllAmbiguousCandidatesUnselectedDoesNotWriteAnything();
        test.unsupportedCandidatesCannotBeSelectedOrApplied();
    }

    private static Fixture fixture() {
        ProbeBinding firstBinding = new ProbeBinding("Mod A", "open", List.of(70));
        ProbeBinding secondBinding = new ProbeBinding("Mod B", "open", List.of(71));
        KeymapSnapshot.BindingEntry firstEntry = new KeymapSnapshot.BindingEntry(
                "malilib", "open", List.of(65), List.of()
        );
        KeymapSnapshot.BindingEntry secondEntry = new KeymapSnapshot.BindingEntry(
                "malilib", "open", List.of(66), List.of()
        );
        KeymapSnapshot.ImportMatch firstToFirst = match(firstBinding, firstEntry);
        KeymapSnapshot.ImportMatch firstToSecond = match(secondBinding, firstEntry);
        KeymapSnapshot.ImportMatch secondToFirst = match(firstBinding, secondEntry);
        KeymapSnapshot.ImportMatch secondToSecond = match(secondBinding, secondEntry);
        List<KeymapSnapshot.ImportMatch> matches = List.of(
                firstToFirst, firstToSecond, secondToFirst, secondToSecond
        );
        return new Fixture(matches, firstToFirst, firstToSecond, secondToSecond, firstBinding, secondBinding);
    }

    private static KeymapSnapshot.ImportMatch match(ProbeBinding binding,
                                                     KeymapSnapshot.BindingEntry entry) {
        KeymapSnapshot.ImportMatch match = new KeymapSnapshot.ImportMatch(binding, entry);
        match.selected = false;
        return match;
    }

    private static final class ProbeBinding extends KeyBinding {
        private final String id;
        private List<Integer> keys;
        private final List<List<Integer>> writes = new ArrayList<>();

        private ProbeBinding(String owner, String id, List<Integer> keys) {
            super(Component.literal(owner), Component.literal(id), 4);
            this.id = id;
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
        public void setBoundKeys(List<InputConstants.Key> keys) {
            this.keys = keys.stream().map(MinecraftInputCode::requireCanonical).toList();
            this.writes.add(this.keys);
        }

        @Override
        public boolean isDefault() {
            return false;
        }

        @Override
        public void resetToDefault() {
        }
    }

    private record Fixture(List<KeymapSnapshot.ImportMatch> matches,
                           KeymapSnapshot.ImportMatch firstToFirst,
                           KeymapSnapshot.ImportMatch firstToSecond,
                           KeymapSnapshot.ImportMatch secondToSecond,
                           ProbeBinding firstBinding,
                           ProbeBinding secondBinding) {
    }
}
