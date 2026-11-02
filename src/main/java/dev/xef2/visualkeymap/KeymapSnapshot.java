package dev.xef2.visualkeymap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;
import dev.xef2.visualkeymap.input.MinecraftInputCode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record KeymapSnapshot(int version, List<KeymapSnapshot.BindingEntry> bindings) {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static KeymapSnapshot fromBindings(List<VisualKeymapApi<?>> apis) {
        List<BindingEntry> entries = new ArrayList<>();
        for (VisualKeymapApi<?> api : apis) {
            String provider = api.getProviderName();
            if (provider.isEmpty()) continue;
            for (KeyBinding binding : api.getKeyBindings()) {
                if (binding.isDefault()) continue;
                entries.add(new BindingEntry(
                        provider,
                        binding.getId(),
                        new ArrayList<>(binding.getKeyCodes()),
                        new ArrayList<>(binding.getModifierKeyCodes())
                ));
            }
        }
        return new KeymapSnapshot(1, entries);
    }

    public void writeToFile(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        }
    }

    public static KeymapSnapshot readFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, KeymapSnapshot.class);
        }
    }

    public List<ImportMatch> matchBindings(List<VisualKeymapApi<?>> apis) {
        List<ImportMatch> matches = new ArrayList<>();
        Map<BindingIdentity, List<BindingEntry>> entriesByIdentity = new LinkedHashMap<>();
        Map<BindingIdentity, List<KeyBinding>> bindingsByIdentity = new LinkedHashMap<>();

        for (BindingEntry entry : this.bindings) {
            BindingIdentity identity = new BindingIdentity(entry.provider(), entry.id());
            entriesByIdentity.computeIfAbsent(identity, _ -> new ArrayList<>()).add(entry);
        }

        for (VisualKeymapApi<?> api : apis) {
            String provider = api.getProviderName();
            for (KeyBinding binding : api.getKeyBindings()) {
                BindingIdentity identity = new BindingIdentity(provider, binding.getId());
                bindingsByIdentity.computeIfAbsent(identity, _ -> new ArrayList<>()).add(binding);
            }
        }

        for (var group : entriesByIdentity.entrySet()) {
            List<BindingEntry> entries = group.getValue();
            List<KeyBinding> bindings = bindingsByIdentity.getOrDefault(group.getKey(), List.of());
            if (bindings.isEmpty()) continue;

            if (entries.size() == 1 && bindings.size() == 1) {
                matches.add(new ImportMatch(bindings.getFirst(), entries.getFirst()));
                continue;
            }

            List<BindingEntry> unmatchedEntries = new ArrayList<>(entries);
            List<KeyBinding> unmatchedBindings = new ArrayList<>(bindings);
            Map<List<Integer>, Integer> entryKeyCounts = new HashMap<>();
            Map<List<Integer>, Integer> bindingKeyCounts = new HashMap<>();

            entries.forEach(entry -> entryKeyCounts.merge(entry.keys(), 1, Integer::sum));
            bindings.forEach(binding -> bindingKeyCounts.merge(binding.getKeyCodes(), 1, Integer::sum));

            for (var entryIterator = unmatchedEntries.iterator(); entryIterator.hasNext(); ) {
                BindingEntry entry = entryIterator.next();
                if (entryKeyCounts.get(entry.keys()) != 1
                        || bindingKeyCounts.getOrDefault(entry.keys(), 0) != 1) {
                    continue;
                }
                KeyBinding exactBinding = unmatchedBindings.stream()
                        .filter(binding -> binding.getKeyCodes().equals(entry.keys()))
                        .findFirst()
                        .orElse(null);
                if (exactBinding != null) {
                    matches.add(new ImportMatch(exactBinding, entry));
                    unmatchedBindings.remove(exactBinding);
                    entryIterator.remove();
                }
            }

            for (BindingEntry entry : unmatchedEntries) {
                for (KeyBinding binding : unmatchedBindings) {
                    if (binding.getKeyCodes().equals(entry.keys())) continue;
                    ImportMatch ambiguousMatch = new ImportMatch(binding, entry);
                    ambiguousMatch.selected = false;
                    matches.add(ambiguousMatch);
                }
            }
        }
        return matches;
    }

    private record BindingIdentity(String provider, String id) {
    }

    public record BindingEntry(String provider, String id, List<Integer> keys, List<Integer> modifiers) {
        public BindingEntry {
            provider = provider != null ? provider : "";
            id = id != null ? id : "";
            keys = keys != null ? keys : List.of();
            modifiers = modifiers != null ? modifiers : List.of();
        }
    }

    public static class ImportMatch {
        public final KeyBinding binding;
        public final BindingEntry entry;
        public boolean selected = true;

        public ImportMatch(KeyBinding binding, BindingEntry entry) {
            this.binding = binding;
            this.entry = entry;
        }

        public List<InputConstants.Key> getKeysFromEntry() {
            if (!this.canApply()) {
                throw new IllegalStateException("Snapshot contains an unsupported canonical key code");
            }
            return entry.keys().stream()
                    .map(code -> MinecraftInputCode.decode(code).orElseThrow())
                    .toList();
        }

        public boolean canApply() {
            return entry.keys().stream().allMatch(MinecraftInputCode::canApply);
        }
    }
}
