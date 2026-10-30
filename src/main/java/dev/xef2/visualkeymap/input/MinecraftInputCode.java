package dev.xef2.visualkeymap.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Minecraft 26.3 boundary for Visual Keymap's stable canonical input codes.
 *
 * <p>Native SDL keyboard integers must not leave this class. Keyboard identities cross the
 * boundary by their stable Minecraft key name; mouse buttons use the explicit mapping in
 * {@link CanonicalInputCode}.</p>
 */
@Environment(EnvType.CLIENT)
public final class MinecraftInputCode {
    private MinecraftInputCode() {
    }

    public static OptionalInt encode(final InputConstants.Key key) {
        if (key.equals(InputConstants.UNKNOWN)) {
            return OptionalInt.empty();
        }

        return switch (key.getType()) {
            case KEYBOARD -> CanonicalInputCode.encodeKeyboard(key.getName());
            case MOUSE -> CanonicalInputCode.encodeMouse(key.getValue());
        };
    }

    public static int requireCanonical(final InputConstants.Key key) {
        return encode(key).orElseThrow(() -> new IllegalArgumentException(
                "Unsupported Minecraft input identity: " + key.getName()
        ));
    }

    public static Optional<InputConstants.Key> decode(final int canonicalCode) {
        CanonicalInputCode.DecodedInput decoded = CanonicalInputCode.decode(canonicalCode);
        return switch (decoded.status()) {
            case KEYBOARD -> Optional.of(InputConstants.getKey(decoded.keyName()));
            case MOUSE -> Optional.of(InputConstants.Type.MOUSE.getOrCreate(decoded.nativeMouseButton()));
            case UNAVAILABLE, UNSUPPORTED_EXTENSION, UNSUPPORTED_LEGACY -> Optional.empty();
        };
    }

    public static boolean canApply(final int canonicalCode) {
        return CanonicalInputCode.decode(canonicalCode).canApply();
    }

    public static Component getDisplayName(final int canonicalCode) {
        CanonicalInputCode.DecodedInput decoded = CanonicalInputCode.decode(canonicalCode);
        return switch (decoded.status()) {
            case KEYBOARD, MOUSE -> decode(canonicalCode).orElseThrow().getDisplayName();
            case UNAVAILABLE -> Component.translatable(
                    "visualkeymap.gui.key_unavailable",
                    Component.translatable(decoded.keyName())
            );
            case UNSUPPORTED_EXTENSION, UNSUPPORTED_LEGACY -> Component.translatable(
                    "visualkeymap.gui.unsupported_key_code",
                    canonicalCode
            );
        };
    }
}
