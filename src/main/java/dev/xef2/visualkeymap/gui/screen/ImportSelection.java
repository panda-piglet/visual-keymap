package dev.xef2.visualkeymap.gui.screen;

import dev.xef2.visualkeymap.KeymapSnapshot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class ImportSelection {
    private ImportSelection() {
    }

    static void sanitize(List<KeymapSnapshot.ImportMatch> matches) {
        matches.stream()
                .filter(match -> !match.canApply())
                .forEach(match -> match.selected = false);

        Set<Object> conflictingBindings = identitySet();
        Set<Object> conflictingEntries = identitySet();

        for (int i = 0; i < matches.size(); i++) {
            KeymapSnapshot.ImportMatch first = matches.get(i);
            if (!first.selected) continue;
            for (int j = i + 1; j < matches.size(); j++) {
                KeymapSnapshot.ImportMatch second = matches.get(j);
                if (!second.selected) continue;
                if (first.binding == second.binding) {
                    conflictingBindings.add(first.binding);
                }
                if (first.entry == second.entry) {
                    conflictingEntries.add(first.entry);
                }
            }
        }

        matches.stream()
                .filter(match -> conflictingBindings.contains(match.binding)
                        || conflictingEntries.contains(match.entry))
                .forEach(match -> match.selected = false);
    }

    static void setSelected(List<KeymapSnapshot.ImportMatch> matches,
                            KeymapSnapshot.ImportMatch target,
                            boolean selected) {
        if (selected && !target.canApply()) {
            target.selected = false;
            return;
        }
        if (selected) {
            matches.stream()
                    .filter(match -> match != target)
                    .filter(match -> match.binding == target.binding || match.entry == target.entry)
                    .forEach(match -> match.selected = false);
        }
        target.selected = selected;
    }

    private static Set<Object> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
