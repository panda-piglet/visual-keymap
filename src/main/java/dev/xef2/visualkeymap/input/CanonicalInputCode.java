package dev.xef2.visualkeymap.input;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Converts between Visual Keymap's stable public integer codes and native input identities.
 *
 * <p>Keyboard conversion is name based. SDL keyboard integers must never be passed to this
 * class or exposed through Visual Keymap's public {@code List<Integer>} contract.</p>
 */
public final class CanonicalInputCode {
    public static final int EXTENSION_NAMESPACE_START = 0x01000000;
    public static final int EXTENSION_NAMESPACE_END = 0x0100FFFF;
    public static final int F25_TOMBSTONE = 314;

    private static final Map<String, Integer> KEYBOARD_TO_CANONICAL = Map.ofEntries(
            Map.entry("key.keyboard.0", 48),
            Map.entry("key.keyboard.1", 49),
            Map.entry("key.keyboard.2", 50),
            Map.entry("key.keyboard.3", 51),
            Map.entry("key.keyboard.4", 52),
            Map.entry("key.keyboard.5", 53),
            Map.entry("key.keyboard.6", 54),
            Map.entry("key.keyboard.7", 55),
            Map.entry("key.keyboard.8", 56),
            Map.entry("key.keyboard.9", 57),
            Map.entry("key.keyboard.a", 65),
            Map.entry("key.keyboard.ac.back", 16777217),
            Map.entry("key.keyboard.ac.bookmarks", 16777218),
            Map.entry("key.keyboard.ac.close", 16777219),
            Map.entry("key.keyboard.ac.exit", 16777220),
            Map.entry("key.keyboard.ac.forward", 16777221),
            Map.entry("key.keyboard.ac.home", 16777222),
            Map.entry("key.keyboard.ac.new", 16777223),
            Map.entry("key.keyboard.ac.open", 16777224),
            Map.entry("key.keyboard.ac.print", 16777225),
            Map.entry("key.keyboard.ac.properties", 16777226),
            Map.entry("key.keyboard.ac.refresh", 16777227),
            Map.entry("key.keyboard.ac.save", 16777228),
            Map.entry("key.keyboard.ac.search", 16777229),
            Map.entry("key.keyboard.ac.stop", 16777230),
            Map.entry("key.keyboard.again", 16777231),
            Map.entry("key.keyboard.alternate.erase", 16777232),
            Map.entry("key.keyboard.apostrophe", 39),
            Map.entry("key.keyboard.application", 16777233),
            Map.entry("key.keyboard.b", 66),
            Map.entry("key.keyboard.backslash", 92),
            Map.entry("key.keyboard.backspace", 259),
            Map.entry("key.keyboard.c", 67),
            Map.entry("key.keyboard.call", 16777234),
            Map.entry("key.keyboard.cancel", 16777235),
            Map.entry("key.keyboard.caps.lock", 280),
            Map.entry("key.keyboard.channel.down", 16777236),
            Map.entry("key.keyboard.channel.up", 16777237),
            Map.entry("key.keyboard.clear", 16777238),
            Map.entry("key.keyboard.clear.again", 16777239),
            Map.entry("key.keyboard.comma", 44),
            Map.entry("key.keyboard.copy", 16777240),
            Map.entry("key.keyboard.crsel", 16777241),
            Map.entry("key.keyboard.currency.subunit", 16777242),
            Map.entry("key.keyboard.currency.unit", 16777243),
            Map.entry("key.keyboard.cut", 16777244),
            Map.entry("key.keyboard.d", 68),
            Map.entry("key.keyboard.decimal.separator", 16777245),
            Map.entry("key.keyboard.delete", 261),
            Map.entry("key.keyboard.down", 264),
            Map.entry("key.keyboard.e", 69),
            Map.entry("key.keyboard.end", 269),
            Map.entry("key.keyboard.end.call", 16777246),
            Map.entry("key.keyboard.enter", 257),
            Map.entry("key.keyboard.enter2", 16777247),
            Map.entry("key.keyboard.equal", 61),
            Map.entry("key.keyboard.escape", 256),
            Map.entry("key.keyboard.execute", 16777248),
            Map.entry("key.keyboard.exsel", 16777249),
            Map.entry("key.keyboard.f", 70),
            Map.entry("key.keyboard.f1", 290),
            Map.entry("key.keyboard.f10", 299),
            Map.entry("key.keyboard.f11", 300),
            Map.entry("key.keyboard.f12", 301),
            Map.entry("key.keyboard.f13", 302),
            Map.entry("key.keyboard.f14", 303),
            Map.entry("key.keyboard.f15", 304),
            Map.entry("key.keyboard.f16", 305),
            Map.entry("key.keyboard.f17", 306),
            Map.entry("key.keyboard.f18", 307),
            Map.entry("key.keyboard.f19", 308),
            Map.entry("key.keyboard.f2", 291),
            Map.entry("key.keyboard.f20", 309),
            Map.entry("key.keyboard.f21", 310),
            Map.entry("key.keyboard.f22", 311),
            Map.entry("key.keyboard.f23", 312),
            Map.entry("key.keyboard.f24", 313),
            Map.entry("key.keyboard.f25", 314),
            Map.entry("key.keyboard.f3", 292),
            Map.entry("key.keyboard.f4", 293),
            Map.entry("key.keyboard.f5", 294),
            Map.entry("key.keyboard.f6", 295),
            Map.entry("key.keyboard.f7", 296),
            Map.entry("key.keyboard.f8", 297),
            Map.entry("key.keyboard.f9", 298),
            Map.entry("key.keyboard.find", 16777250),
            Map.entry("key.keyboard.g", 71),
            Map.entry("key.keyboard.grave.accent", 96),
            Map.entry("key.keyboard.h", 72),
            Map.entry("key.keyboard.help", 16777251),
            Map.entry("key.keyboard.home", 268),
            Map.entry("key.keyboard.i", 73),
            Map.entry("key.keyboard.insert", 260),
            Map.entry("key.keyboard.international1", 16777252),
            Map.entry("key.keyboard.international2", 16777253),
            Map.entry("key.keyboard.international3", 16777254),
            Map.entry("key.keyboard.international4", 16777255),
            Map.entry("key.keyboard.international5", 16777256),
            Map.entry("key.keyboard.international6", 16777257),
            Map.entry("key.keyboard.international7", 16777258),
            Map.entry("key.keyboard.international8", 16777259),
            Map.entry("key.keyboard.international9", 16777260),
            Map.entry("key.keyboard.j", 74),
            Map.entry("key.keyboard.k", 75),
            Map.entry("key.keyboard.keypad.0", 320),
            Map.entry("key.keyboard.keypad.00", 16777261),
            Map.entry("key.keyboard.keypad.000", 16777262),
            Map.entry("key.keyboard.keypad.1", 321),
            Map.entry("key.keyboard.keypad.2", 322),
            Map.entry("key.keyboard.keypad.3", 323),
            Map.entry("key.keyboard.keypad.4", 324),
            Map.entry("key.keyboard.keypad.5", 325),
            Map.entry("key.keyboard.keypad.6", 326),
            Map.entry("key.keyboard.keypad.7", 327),
            Map.entry("key.keyboard.keypad.8", 328),
            Map.entry("key.keyboard.keypad.9", 329),
            Map.entry("key.keyboard.keypad.a", 16777263),
            Map.entry("key.keyboard.keypad.add", 334),
            Map.entry("key.keyboard.keypad.ampersand", 16777264),
            Map.entry("key.keyboard.keypad.at", 16777265),
            Map.entry("key.keyboard.keypad.b", 16777266),
            Map.entry("key.keyboard.keypad.backspace", 16777267),
            Map.entry("key.keyboard.keypad.binary", 16777268),
            Map.entry("key.keyboard.keypad.c", 16777269),
            Map.entry("key.keyboard.keypad.clear", 16777270),
            Map.entry("key.keyboard.keypad.clear.entry", 16777271),
            Map.entry("key.keyboard.keypad.colon", 16777272),
            Map.entry("key.keyboard.keypad.comma", 16777273),
            Map.entry("key.keyboard.keypad.d", 16777274),
            Map.entry("key.keyboard.keypad.decimal", 330),
            Map.entry("key.keyboard.keypad.divide", 331),
            Map.entry("key.keyboard.keypad.double.ampersand", 16777275),
            Map.entry("key.keyboard.keypad.double.vertical.bar", 16777276),
            Map.entry("key.keyboard.keypad.e", 16777277),
            Map.entry("key.keyboard.keypad.enter", 335),
            Map.entry("key.keyboard.keypad.equal", 336),
            Map.entry("key.keyboard.keypad.equals.as400", 16777278),
            Map.entry("key.keyboard.keypad.exclamation", 16777279),
            Map.entry("key.keyboard.keypad.f", 16777280),
            Map.entry("key.keyboard.keypad.greater", 16777281),
            Map.entry("key.keyboard.keypad.hash", 16777282),
            Map.entry("key.keyboard.keypad.hexadecimal", 16777283),
            Map.entry("key.keyboard.keypad.left.brace", 16777284),
            Map.entry("key.keyboard.keypad.left.parenthesis", 16777285),
            Map.entry("key.keyboard.keypad.less", 16777286),
            Map.entry("key.keyboard.keypad.memory.add", 16777287),
            Map.entry("key.keyboard.keypad.memory.clear", 16777288),
            Map.entry("key.keyboard.keypad.memory.divide", 16777289),
            Map.entry("key.keyboard.keypad.memory.multiply", 16777290),
            Map.entry("key.keyboard.keypad.memory.recall", 16777291),
            Map.entry("key.keyboard.keypad.memory.store", 16777292),
            Map.entry("key.keyboard.keypad.memory.subtract", 16777293),
            Map.entry("key.keyboard.keypad.multiply", 332),
            Map.entry("key.keyboard.keypad.octal", 16777294),
            Map.entry("key.keyboard.keypad.percent", 16777295),
            Map.entry("key.keyboard.keypad.period", 16777296),
            Map.entry("key.keyboard.keypad.plus.minus", 16777297),
            Map.entry("key.keyboard.keypad.power", 16777298),
            Map.entry("key.keyboard.keypad.right.brace", 16777299),
            Map.entry("key.keyboard.keypad.right.parenthesis", 16777300),
            Map.entry("key.keyboard.keypad.space", 16777301),
            Map.entry("key.keyboard.keypad.subtract", 333),
            Map.entry("key.keyboard.keypad.tab", 16777302),
            Map.entry("key.keyboard.keypad.vertical.bar", 16777303),
            Map.entry("key.keyboard.keypad.xor", 16777304),
            Map.entry("key.keyboard.l", 76),
            Map.entry("key.keyboard.lang1", 16777305),
            Map.entry("key.keyboard.lang2", 16777306),
            Map.entry("key.keyboard.lang3", 16777307),
            Map.entry("key.keyboard.lang4", 16777308),
            Map.entry("key.keyboard.lang5", 16777309),
            Map.entry("key.keyboard.lang6", 16777310),
            Map.entry("key.keyboard.lang7", 16777311),
            Map.entry("key.keyboard.lang8", 16777312),
            Map.entry("key.keyboard.lang9", 16777313),
            Map.entry("key.keyboard.left", 263),
            Map.entry("key.keyboard.left.alt", 342),
            Map.entry("key.keyboard.left.bracket", 91),
            Map.entry("key.keyboard.left.control", 341),
            Map.entry("key.keyboard.left.shift", 340),
            Map.entry("key.keyboard.left.win", 343),
            Map.entry("key.keyboard.m", 77),
            Map.entry("key.keyboard.media.eject", 16777314),
            Map.entry("key.keyboard.media.fast.forward", 16777315),
            Map.entry("key.keyboard.media.next.track", 16777316),
            Map.entry("key.keyboard.media.pause", 16777317),
            Map.entry("key.keyboard.media.play", 16777318),
            Map.entry("key.keyboard.media.play.pause", 16777319),
            Map.entry("key.keyboard.media.previous.track", 16777320),
            Map.entry("key.keyboard.media.record", 16777321),
            Map.entry("key.keyboard.media.rewind", 16777322),
            Map.entry("key.keyboard.media.select", 16777323),
            Map.entry("key.keyboard.media.stop", 16777324),
            Map.entry("key.keyboard.menu", 348),
            Map.entry("key.keyboard.minus", 45),
            Map.entry("key.keyboard.mode", 16777325),
            Map.entry("key.keyboard.mute", 16777326),
            Map.entry("key.keyboard.n", 78),
            Map.entry("key.keyboard.num.lock", 282),
            Map.entry("key.keyboard.o", 79),
            Map.entry("key.keyboard.oper", 16777327),
            Map.entry("key.keyboard.out", 16777328),
            Map.entry("key.keyboard.p", 80),
            Map.entry("key.keyboard.page.down", 267),
            Map.entry("key.keyboard.page.up", 266),
            Map.entry("key.keyboard.paste", 16777329),
            Map.entry("key.keyboard.pause", 284),
            Map.entry("key.keyboard.period", 46),
            Map.entry("key.keyboard.power", 16777330),
            Map.entry("key.keyboard.print.screen", 283),
            Map.entry("key.keyboard.prior", 16777331),
            Map.entry("key.keyboard.q", 81),
            Map.entry("key.keyboard.r", 82),
            Map.entry("key.keyboard.right", 262),
            Map.entry("key.keyboard.right.alt", 346),
            Map.entry("key.keyboard.right.bracket", 93),
            Map.entry("key.keyboard.right.control", 345),
            Map.entry("key.keyboard.right.shift", 344),
            Map.entry("key.keyboard.right.win", 347),
            Map.entry("key.keyboard.s", 83),
            Map.entry("key.keyboard.scroll.lock", 281),
            Map.entry("key.keyboard.select", 16777332),
            Map.entry("key.keyboard.semicolon", 59),
            Map.entry("key.keyboard.separator", 16777333),
            Map.entry("key.keyboard.slash", 47),
            Map.entry("key.keyboard.sleep", 16777334),
            Map.entry("key.keyboard.soft.left", 16777335),
            Map.entry("key.keyboard.soft.right", 16777336),
            Map.entry("key.keyboard.space", 32),
            Map.entry("key.keyboard.stop", 16777337),
            Map.entry("key.keyboard.sys.req", 16777338),
            Map.entry("key.keyboard.t", 84),
            Map.entry("key.keyboard.tab", 258),
            Map.entry("key.keyboard.thousands.separator", 16777339),
            Map.entry("key.keyboard.u", 85),
            Map.entry("key.keyboard.undo", 16777340),
            Map.entry("key.keyboard.up", 265),
            Map.entry("key.keyboard.v", 86),
            Map.entry("key.keyboard.volume.down", 16777341),
            Map.entry("key.keyboard.volume.up", 16777342),
            Map.entry("key.keyboard.w", 87),
            Map.entry("key.keyboard.wake", 16777343),
            Map.entry("key.keyboard.world.1", 161),
            Map.entry("key.keyboard.world.2", 162),
            Map.entry("key.keyboard.x", 88),
            Map.entry("key.keyboard.y", 89),
            Map.entry("key.keyboard.z", 90)
    );
    private static final Map<Integer, String> CANONICAL_TO_KEYBOARD = reverseKeyboardMap();

    private CanonicalInputCode() {
    }

    /**
     * Encodes a registered keyboard translation key. Unknown or unsupported names are empty.
     */
    public static OptionalInt encodeKeyboard(final String keyName) {
        if ("key.keyboard.unknown".equals(keyName)) {
            return OptionalInt.empty();
        }

        Integer canonicalCode = KEYBOARD_TO_CANONICAL.get(keyName);
        return canonicalCode == null ? OptionalInt.empty() : OptionalInt.of(canonicalCode);
    }

    /**
     * Encodes Minecraft 26.3's native mouse button numbers into the existing public 0..7 range.
     */
    public static OptionalInt encodeMouse(final int nativeButton) {
        return switch (nativeButton) {
            case 1 -> OptionalInt.of(0);
            case 3 -> OptionalInt.of(1);
            case 2 -> OptionalInt.of(2);
            case 4, 5, 6, 7, 8 -> OptionalInt.of(nativeButton - 1);
            default -> OptionalInt.empty();
        };
    }

    /**
     * Decodes a public integer without constructing a Minecraft key or interpreting it as SDL.
     */
    public static DecodedInput decode(final int canonicalCode) {
        if (canonicalCode >= 0 && canonicalCode <= 7) {
            return decodeMouse(canonicalCode);
        }

        String keyName = CANONICAL_TO_KEYBOARD.get(canonicalCode);
        if (canonicalCode == F25_TOMBSTONE) {
            return new DecodedInput(Status.UNAVAILABLE, canonicalCode, keyName, null);
        }
        if (keyName != null) {
            return new DecodedInput(Status.KEYBOARD, canonicalCode, keyName, null);
        }
        if (isExtensionNamespace(canonicalCode)) {
            return new DecodedInput(Status.UNSUPPORTED_EXTENSION, canonicalCode, null, null);
        }
        return new DecodedInput(Status.UNSUPPORTED_LEGACY, canonicalCode, null, null);
    }

    public static boolean isExtensionNamespace(final int canonicalCode) {
        return canonicalCode >= EXTENSION_NAMESPACE_START && canonicalCode <= EXTENSION_NAMESPACE_END;
    }

    private static DecodedInput decodeMouse(final int canonicalCode) {
        return switch (canonicalCode) {
            case 0 -> new DecodedInput(Status.MOUSE, canonicalCode, "key.mouse.left", 1);
            case 1 -> new DecodedInput(Status.MOUSE, canonicalCode, "key.mouse.right", 3);
            case 2 -> new DecodedInput(Status.MOUSE, canonicalCode, "key.mouse.middle", 2);
            case 3, 4, 5, 6, 7 -> new DecodedInput(
                    Status.MOUSE,
                    canonicalCode,
                    "key.mouse." + (canonicalCode + 1),
                    canonicalCode + 1
            );
            default -> throw new IllegalArgumentException("Not a canonical mouse code: " + canonicalCode);
        };
    }

    private static Map<Integer, String> reverseKeyboardMap() {
        Map<Integer, String> result = new HashMap<>();
        KEYBOARD_TO_CANONICAL.forEach((name, code) -> {
            String previous = result.put(code, name);
            if (previous != null) {
                throw new IllegalStateException("Duplicate canonical keyboard code " + code);
            }
        });
        return Map.copyOf(result);
    }

    public enum Status {
        KEYBOARD,
        MOUSE,
        UNAVAILABLE,
        UNSUPPORTED_EXTENSION,
        UNSUPPORTED_LEGACY
    }

    /**
     * A decoded identity. {@code nativeMouseButton} is present only for {@link Status#MOUSE};
     * {@code keyName} is absent only for unsupported integers.
     */
    public record DecodedInput(
            Status status,
            int canonicalCode,
            String keyName,
            Integer nativeMouseButton
    ) {
        public boolean canApply() {
            return this.status == Status.KEYBOARD || this.status == Status.MOUSE;
        }
    }
}