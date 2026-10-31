package dev.xef2.visualkeymap.phase4;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** External-package provider used only by the isolated P6 production-JAR smoke test. */
public final class Phase4Provider implements VisualKeymapApi<Phase4Provider.Binding> {
    private static final List<Binding> BINDINGS = List.of(new Binding());
    public static int saveCalls;

    @Override
    public List<Binding> getKeyBindings() {
        return BINDINGS;
    }

    @Override
    public void save() {
        saveCalls++;
    }

    @Override
    public String getProviderName() {
        return "phase4-external";
    }

    public static final class Binding extends KeyBinding {
        private List<Integer> keys = new ArrayList<>(List.of(66));

        private Binding() {
            super(Component.literal("Phase 4"), Component.literal("External Binding"), 1);
        }

        @Override
        public String getId() {
            return "external-binding";
        }

        @Override
        public List<Integer> getKeyCodes() {
            return List.copyOf(this.keys);
        }

        @Override
        public List<Integer> getModifierKeyCodes() {
            return List.of(341);
        }

        @Override
        public int getOrder() {
            return 42;
        }

        @Override
        public void setBoundKeys(List<InputConstants.Key> keys) {
            this.keys = new ArrayList<>(keys.stream().map(Phase4Provider.Binding::canonical).toList());
        }

        @Override
        public boolean isDefault() {
            return this.keys.equals(List.of(65));
        }

        @Override
        public void resetToDefault() {
            this.keys = new ArrayList<>(List.of(65));
        }

        private static int canonical(InputConstants.Key key) {
            return switch (key.getName()) {
                case "key.keyboard.a" -> 65;
                case "key.keyboard.b" -> 66;
                default -> throw new IllegalArgumentException("Unsupported Phase 4 test key: " + key.getName());
            };
        }
    }
}
