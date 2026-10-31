package dev.xef2.visualkeymap.integration;

import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.util.input.ScanCodes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaLiLibInputCodeTest {
    @Test
    void convertsKeyboardScanCodesThroughTheCanonicalLayer() {
        InputConstants.Key a = InputConstants.getKey("key.keyboard.a");
        InputConstants.Key j = InputConstants.getKey("key.keyboard.j");
        InputConstants.Key f9 = InputConstants.getKey("key.keyboard.f9");

        assertEquals(4, a.getValue());
        assertEquals(List.of(65, 74, 298), MaLiLibInputCode.toCanonicalCodes(List.of(
                a.getValue(), j.getValue(), f9.getValue()
        )));
    }

    @Test
    void convertsOnlyTheConfirmedEightMouseCodes() {
        assertEquals(-99, ScanCodes.MOUSE_LEFT);
        assertEquals(-98, ScanCodes.MOUSE_MIDDLE);
        assertEquals(-97, ScanCodes.MOUSE_RIGHT);
        assertEquals(-92, ScanCodes.MOUSE_EXTRA_5);

        assertEquals(List.of(0, 2, 1, 3, 4, 5, 6, 7), MaLiLibInputCode.toCanonicalCodes(List.of(
                -99, -98, -97, -96, -95, -94, -93, -92
        )));
        assertEquals(List.of(), MaLiLibInputCode.toCanonicalCodes(List.of(-100, -91, -1)));
    }

    @Test
    void doesNotInterpretGamepadOrOtherNegativeCodesAsMouseInput() {
        assertEquals(-200, ScanCodes.GAMEPAD_SOUTH);
        assertEquals(-300, ScanCodes.GAMEPAD_LEFT_X);

        assertEquals(List.of(), MaLiLibInputCode.toCanonicalCodes(List.of(
                -101, ScanCodes.GAMEPAD_SOUTH, -199, ScanCodes.GAMEPAD_LEFT_X, -299
        )));
    }

    @Test
    void writesNativeKeyboardAndMouseCodesForMaLiLib() {
        assertEquals(13, MaLiLibInputCode.toScanCode(InputConstants.getKey("key.keyboard.j")));
        assertEquals(66, MaLiLibInputCode.toScanCode(InputConstants.getKey("key.keyboard.f9")));
        assertEquals(-99, MaLiLibInputCode.toScanCode(InputConstants.Type.MOUSE.getOrCreate(1)));
        assertEquals(-98, MaLiLibInputCode.toScanCode(InputConstants.Type.MOUSE.getOrCreate(2)));
        assertEquals(-97, MaLiLibInputCode.toScanCode(InputConstants.Type.MOUSE.getOrCreate(3)));
        assertEquals(-92, MaLiLibInputCode.toScanCode(InputConstants.Type.MOUSE.getOrCreate(8)));
    }

    @Test
    void preservesUnsupportedCodesWhenEditingSupportedInputs() {
        int j = InputConstants.getKey("key.keyboard.j").getValue();
        int f9 = InputConstants.getKey("key.keyboard.f9").getValue();
        List<Integer> original = List.of(j, ScanCodes.GAMEPAD_SOUTH, ScanCodes.MOUSE_MIDDLE,
                ScanCodes.GAMEPAD_LEFT_X);

        List<Integer> replaced = MaLiLibInputCode.replaceSupportedScanCodes(original, List.of(
                InputConstants.getKey("key.keyboard.f9"),
                InputConstants.Type.MOUSE.getOrCreate(3)
        ));

        assertEquals(List.of(f9, ScanCodes.GAMEPAD_SOUTH, ScanCodes.MOUSE_RIGHT,
                ScanCodes.GAMEPAD_LEFT_X), replaced);
        assertEquals(List.of(j, ScanCodes.GAMEPAD_SOUTH, ScanCodes.MOUSE_MIDDLE,
                ScanCodes.GAMEPAD_LEFT_X), original);
    }

    @Test
    void clearsSupportedCodesButRetainsUnsupportedCodes() {
        int j = InputConstants.getKey("key.keyboard.j").getValue();
        List<Integer> replaced = MaLiLibInputCode.replaceSupportedScanCodes(
                List.of(j, ScanCodes.GAMEPAD_SOUTH, ScanCodes.MOUSE_LEFT),
                List.of()
        );

        assertEquals(List.of(ScanCodes.GAMEPAD_SOUTH), replaced);
        assertTrue(MaLiLibInputCode.toCanonicalCodes(replaced).isEmpty());
    }
}
