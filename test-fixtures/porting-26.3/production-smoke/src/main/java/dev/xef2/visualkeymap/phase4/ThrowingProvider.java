package dev.xef2.visualkeymap.phase4;

import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;

import java.util.List;

/** A deliberately failing P6 entrypoint; the valid provider must still initialize. */
public final class ThrowingProvider implements VisualKeymapApi<KeyBinding> {
    public ThrowingProvider() {
        throw new IllegalStateException("intentional Phase 4 entrypoint failure");
    }

    @Override
    public List<KeyBinding> getKeyBindings() {
        return List.of();
    }

    @Override
    public void save() {
    }
}
