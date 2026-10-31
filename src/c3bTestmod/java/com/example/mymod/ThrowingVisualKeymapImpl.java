package com.example.mymod;

import dev.xef2.visualkeymap.api.KeyBinding;
import dev.xef2.visualkeymap.api.VisualKeymapApi;

import java.util.List;

/** Entrypoint whose construction failure must not prevent the valid provider from loading. */
public final class ThrowingVisualKeymapImpl implements VisualKeymapApi<KeyBinding> {
    public ThrowingVisualKeymapImpl() {
        throw new IllegalStateException("intentional C3-B entrypoint failure");
    }

    @Override
    public List<KeyBinding> getKeyBindings() {
        return List.of();
    }

    @Override
    public void save() {
    }
}
