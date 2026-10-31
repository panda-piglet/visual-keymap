package com.example.mymod;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * README-style external provider used unchanged against both the 26.2 baseline and 26.3 target.
 */
public final class MyModVisualKeymapImpl implements VisualKeymapApi<MyModVisualKeymapImpl.ExternalBinding> {
    public static final String PROVIDER = "c3-external";
    public static int saveCalls;
    private static final List<ExternalBinding> BINDINGS = List.of(
            new ExternalBinding("alpha", "Alpha Action", "Alpha comment", 4,
                    List.of(66), List.of(341), List.of(65), 42),
            new ExternalBinding("conflict", "Conflict Action", null, 1,
                    List.of(66), List.of(341), List.of(67), 42),
            new ExternalBinding("empty", "Empty Action", null, 4,
                    List.of(), List.of(), List.of(), 0),
            new ExternalBinding("four", "Four Key Action", null, 4,
                    List.of(67, 68, 69, 70), List.of(), List.of(67, 68, 69, 70), 7)
    );

    @Override
    public List<ExternalBinding> getKeyBindings() {
        return BINDINGS;
    }

    @Override
    public void save() {
        saveCalls++;
    }

    @Override
    public String getProviderName() {
        return PROVIDER;
    }

    public static final class ExternalBinding extends KeyBinding {
        private static final Map<String, Integer> CANONICAL_BY_NAME = Map.ofEntries(
                Map.entry("key.keyboard.a", 65),
                Map.entry("key.keyboard.b", 66),
                Map.entry("key.keyboard.c", 67),
                Map.entry("key.keyboard.d", 68),
                Map.entry("key.keyboard.e", 69),
                Map.entry("key.keyboard.f", 70),
                Map.entry("key.keyboard.left.control", 341)
        );

        private final String id;
        private final Component comment;
        private final List<Integer> defaultKeys;
        private final List<Integer> modifiers;
        private final int order;
        private List<Integer> keys;
        private int setCalls;
        private int resetCalls;

        public ExternalBinding(
                String id,
                String name,
                String comment,
                int maxBoundKeys,
                List<Integer> keys,
                List<Integer> modifiers,
                List<Integer> defaultKeys,
                int order
        ) {
            super(Component.literal("External Category"), Component.literal(name), maxBoundKeys);
            this.id = id;
            this.comment = comment == null ? null : Component.literal(comment);
            this.keys = new ArrayList<>(keys);
            this.modifiers = List.copyOf(modifiers);
            this.defaultKeys = List.copyOf(defaultKeys);
            this.order = order;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public Component getComment() {
            return this.comment;
        }

        @Override
        public List<Integer> getKeyCodes() {
            return List.copyOf(this.keys);
        }

        @Override
        public List<Integer> getModifierKeyCodes() {
            return this.modifiers;
        }

        @Override
        public int getOrder() {
            return this.order;
        }

        @Override
        protected List<String> getSearchableStrings() {
            List<String> values = new ArrayList<>(super.getSearchableStrings());
            values.add("external provider needle");
            return values;
        }

        @Override
        public void setBoundKeys(List<InputConstants.Key> keys) {
            this.keys = keys.stream().map(ExternalBinding::toCanonical).toList();
            this.setCalls++;
        }

        @Override
        public boolean isDefault() {
            return this.keys.equals(this.defaultKeys);
        }

        @Override
        public void resetToDefault() {
            this.keys = new ArrayList<>(this.defaultKeys);
            this.resetCalls++;
        }

        public int getSetCalls() {
            return this.setCalls;
        }

        public int getResetCalls() {
            return this.resetCalls;
        }

        private static int toCanonical(InputConstants.Key key) {
            if (key.getName().startsWith("key.mouse.")) {
                return key.getValue();
            }
            Integer canonical = CANONICAL_BY_NAME.get(key.getName());
            if (canonical == null) {
                throw new IllegalArgumentException("Unsupported test key: " + key.getName());
            }
            return canonical;
        }
    }
}
