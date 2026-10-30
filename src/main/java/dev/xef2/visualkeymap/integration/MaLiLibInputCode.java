package dev.xef2.visualkeymap.integration;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xef2.visualkeymap.input.MinecraftInputCode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Converts MaLiLib 0.30.x scan codes at the adapter boundary.
 *
 * <p>Only the confirmed {@code -99..-92} mouse range is interpreted as mouse input. Other
 * values which cannot be represented by Visual Keymap's public canonical integer model are
 * omitted from that model and retained verbatim when the user edits the supported part of the
 * same MaLiLib keybind.</p>
 */
final class MaLiLibInputCode {
    static final int MOUSE_CODE_MIN = -99;
    static final int MOUSE_CODE_MAX = -92;

    private MaLiLibInputCode() {
    }

    static List<Integer> toCanonicalCodes(final List<Integer> scanCodes) {
        List<Integer> result = new ArrayList<>(scanCodes.size());
        for (int scanCode : scanCodes) {
            OptionalInt canonicalCode = toCanonicalCode(scanCode);
            canonicalCode.ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    static OptionalInt toCanonicalCode(final int scanCode) {
        if (isMouseCode(scanCode)) {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(scanCode + 100);
            return MinecraftInputCode.encode(key);
        }
        if (scanCode > 0) {
            InputConstants.Key key = InputConstants.Type.KEYBOARD.getOrCreate(scanCode);
            return MinecraftInputCode.encode(key);
        }
        return OptionalInt.empty();
    }

    static List<Integer> replaceSupportedScanCodes(
            final List<Integer> currentScanCodes,
            final List<InputConstants.Key> newKeys
    ) {
        ArrayDeque<Integer> replacements = new ArrayDeque<>(
                newKeys.stream().map(MaLiLibInputCode::toScanCode).toList()
        );
        List<Integer> result = new ArrayList<>(currentScanCodes.size() + replacements.size());

        for (int currentScanCode : currentScanCodes) {
            if (toCanonicalCode(currentScanCode).isPresent()) {
                if (!replacements.isEmpty()) {
                    result.add(replacements.removeFirst());
                }
            } else {
                result.add(currentScanCode);
            }
        }
        result.addAll(replacements);
        return List.copyOf(result);
    }

    static int toScanCode(final InputConstants.Key key) {
        return switch (key.getType()) {
            case KEYBOARD -> {
                MinecraftInputCode.requireCanonical(key);
                yield key.getValue();
            }
            case MOUSE -> {
                int nativeButton = key.getValue();
                if (nativeButton < 1 || nativeButton > 8) {
                    throw new IllegalArgumentException("Unsupported MaLiLib mouse button: " + nativeButton);
                }
                yield nativeButton - 100;
            }
        };
    }

    private static boolean isMouseCode(final int scanCode) {
        return scanCode >= MOUSE_CODE_MIN && scanCode <= MOUSE_CODE_MAX;
    }
}
