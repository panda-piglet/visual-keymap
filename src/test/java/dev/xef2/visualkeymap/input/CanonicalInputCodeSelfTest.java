package dev.xef2.visualkeymap.input;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CanonicalInputCodeSelfTest {
    private static final String EXPECTED_EXTENSION_SHA256 =
            "0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28";
    private static final Pattern LEGACY_ROW = Pattern.compile("^LEGACY\\s+(\\S+)\\s+(-?\\d+)$");

    private CanonicalInputCodeSelfTest() {
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected legacy probe log and extension TSV paths");
        }

        Path legacyPath = Path.of(args[0]);
        Path extensionPath = Path.of(args[1]);
        Map<String, Integer> legacy = readLegacy(legacyPath);
        Map<String, Integer> extensions = readExtensions(extensionPath);

        check(legacy.size() == 121, "legacy table must contain 121 rows including unknown");
        check(extensions.size() == 127, "extension table must contain 127 rows");
        check(EXPECTED_EXTENSION_SHA256.equals(sha256(extensionPath)), "extension table hash changed");

        Set<Integer> allCanonicalCodes = new HashSet<>();
        for (Map.Entry<String, Integer> entry : legacy.entrySet()) {
            String keyName = entry.getKey();
            int canonicalCode = entry.getValue();
            if ("key.keyboard.unknown".equals(keyName)) {
                check(CanonicalInputCode.encodeKeyboard(keyName).isEmpty(), "unknown must remain unbound");
                continue;
            }

            check(allCanonicalCodes.add(canonicalCode), "duplicate legacy code " + canonicalCode);
            checkEncoded(keyName, canonicalCode);
            CanonicalInputCode.DecodedInput decoded = CanonicalInputCode.decode(canonicalCode);
            if (canonicalCode == CanonicalInputCode.F25_TOMBSTONE) {
                check(decoded.status() == CanonicalInputCode.Status.UNAVAILABLE, "F25 must be unavailable");
                check(!decoded.canApply(), "F25 must not apply to a 26.3 live binding");
            } else {
                checkKeyboardRoundTrip(keyName, canonicalCode, decoded);
            }
        }

        for (Map.Entry<String, Integer> entry : extensions.entrySet()) {
            String keyName = entry.getKey();
            int canonicalCode = entry.getValue();
            check(CanonicalInputCode.isExtensionNamespace(canonicalCode), "extension outside namespace");
            check(allCanonicalCodes.add(canonicalCode), "extension collision " + canonicalCode);
            checkEncoded(keyName, canonicalCode);
            checkKeyboardRoundTrip(keyName, canonicalCode, CanonicalInputCode.decode(canonicalCode));
        }

        check(allCanonicalCodes.size() == 247, "expected 247 assigned keyboard identities");
        checkMouseRoundTrips();
        checkUnsupportedValues();
        checkSdlCollisionBoundary();

        System.out.println("PASS CanonicalInputCodeSelfTest");
        System.out.println("legacy=121 extension=127 assignedKeyboard=247 mouse=8");
        System.out.println("extensionTableSha256=" + EXPECTED_EXTENSION_SHA256);
    }

    private static void checkEncoded(final String keyName, final int expectedCode) {
        OptionalInt encoded = CanonicalInputCode.encodeKeyboard(keyName);
        check(encoded.isPresent(), "missing keyboard name " + keyName);
        check(encoded.getAsInt() == expectedCode, "wrong canonical code for " + keyName);
    }

    private static void checkKeyboardRoundTrip(
            final String keyName,
            final int canonicalCode,
            final CanonicalInputCode.DecodedInput decoded
    ) {
        check(decoded.status() == CanonicalInputCode.Status.KEYBOARD, "not a keyboard: " + keyName);
        check(decoded.canApply(), "known keyboard must be applicable: " + keyName);
        check(keyName.equals(decoded.keyName()), "wrong decoded keyboard name for " + canonicalCode);
        check(decoded.nativeMouseButton() == null, "keyboard returned a mouse button");
        check(CanonicalInputCode.encodeKeyboard(decoded.keyName()).orElseThrow() == canonicalCode,
                "keyboard round trip failed for " + keyName);
    }

    private static void checkMouseRoundTrips() {
        int[] nativeButtons = {1, 3, 2, 4, 5, 6, 7, 8};
        String[] names = {
                "key.mouse.left", "key.mouse.right", "key.mouse.middle", "key.mouse.4",
                "key.mouse.5", "key.mouse.6", "key.mouse.7", "key.mouse.8"
        };
        for (int canonicalCode = 0; canonicalCode < nativeButtons.length; canonicalCode++) {
            int nativeButton = nativeButtons[canonicalCode];
            check(CanonicalInputCode.encodeMouse(nativeButton).orElseThrow() == canonicalCode,
                    "mouse encode failed for native button " + nativeButton);
            CanonicalInputCode.DecodedInput decoded = CanonicalInputCode.decode(canonicalCode);
            check(decoded.status() == CanonicalInputCode.Status.MOUSE, "mouse decode status failed");
            check(decoded.canApply(), "known mouse must be applicable");
            check(decoded.nativeMouseButton() == nativeButton, "mouse native button changed");
            check(names[canonicalCode].equals(decoded.keyName()), "mouse name changed");
        }
        check(CanonicalInputCode.encodeMouse(0).isEmpty(), "native mouse 0 must not be accepted");
        check(CanonicalInputCode.encodeMouse(9).isEmpty(), "native mouse 9 must not be accepted");
    }

    private static void checkUnsupportedValues() {
        int[] unknownExtensions = {
                CanonicalInputCode.EXTENSION_NAMESPACE_START,
                0x01000080,
                CanonicalInputCode.EXTENSION_NAMESPACE_END
        };
        for (int code : unknownExtensions) {
            CanonicalInputCode.DecodedInput decoded = CanonicalInputCode.decode(code);
            check(decoded.status() == CanonicalInputCode.Status.UNSUPPORTED_EXTENSION,
                    "unassigned extension must be unsupported");
            check(!decoded.canApply(), "unassigned extension must not apply");
        }

        int[] unknownLegacy = {-1, 8, 315, 0x01010000, Integer.MAX_VALUE};
        for (int code : unknownLegacy) {
            CanonicalInputCode.DecodedInput decoded = CanonicalInputCode.decode(code);
            check(decoded.status() == CanonicalInputCode.Status.UNSUPPORTED_LEGACY,
                    "unknown legacy integer must not be reinterpreted: " + code);
            check(!decoded.canApply(), "unknown legacy integer must not apply");
        }
        check(CanonicalInputCode.encodeKeyboard("key.keyboard.not.registered").isEmpty(),
                "unknown key name must not acquire a code");
    }

    private static void checkSdlCollisionBoundary() {
        check(CanonicalInputCode.encodeKeyboard("key.keyboard.a").orElseThrow() == 65,
                "A must keep its 26.2 canonical value");
        check(CanonicalInputCode.decode(4).status() == CanonicalInputCode.Status.MOUSE,
                "public code 4 must remain mouse 5, not SDL keyboard A");
        for (String keyName : new String[]{
                "key.keyboard.a", "key.keyboard.b", "key.keyboard.c", "key.keyboard.d", "key.keyboard.e"
        }) {
            int keyboardCode = CanonicalInputCode.encodeKeyboard(keyName).orElseThrow();
            check(keyboardCode < 0 || keyboardCode > 7, "SDL A-E leaked into public mouse range");
        }
    }

    private static Map<String, Integer> readLegacy(final Path path) throws IOException {
        Map<String, Integer> result = new HashMap<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            Matcher matcher = LEGACY_ROW.matcher(line);
            if (matcher.matches()) {
                check(result.put(matcher.group(1), Integer.parseInt(matcher.group(2))) == null,
                        "duplicate legacy name " + matcher.group(1));
            }
        }
        return result;
    }

    private static Map<String, Integer> readExtensions(final Path path) throws IOException {
        Map<String, Integer> result = new HashMap<>();
        int expectedId = 1;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8).subList(1, 128)) {
            String[] columns = line.replace("\"", "").split("\\t", -1);
            int id = Integer.parseInt(columns[0]);
            int canonicalCode = Integer.parseInt(columns[1]);
            String keyName = columns[3];
            check(id == expectedId++, "extension IDs must be frozen in order");
            check(canonicalCode == (CanonicalInputCode.EXTENSION_NAMESPACE_START | id),
                    "extension formula changed for " + keyName);
            check(result.put(keyName, canonicalCode) == null, "duplicate extension name " + keyName);
        }
        return result;
    }

    private static String sha256(final Path path) throws IOException, NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().withUpperCase().formatHex(digest);
    }

    private static void check(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
