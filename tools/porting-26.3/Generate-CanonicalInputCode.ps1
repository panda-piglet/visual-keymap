param(
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
)

$ErrorActionPreference = 'Stop'

$evidenceDirectory = Join-Path $RepositoryRoot 'docs\porting-26.3\evidence'
$legacyPath = Join-Path $evidenceDirectory 'legacy-extended-key-probe.log'
$extensionPath = Join-Path $evidenceDirectory 'proposed-26.3-extension-map.tsv'
$outputPath = Join-Path $RepositoryRoot 'src\main\java\dev\xef2\visualkeymap\input\CanonicalInputCode.java'
$expectedExtensionHash = '0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28'

$legacy = [ordered]@{}
Get-Content $legacyPath | ForEach-Object {
    if ($_ -match '^LEGACY\s+(\S+)\s+(-?\d+)$') {
        $legacy[$matches[1]] = [int]$matches[2]
    }
}

$extensions = @(Import-Csv -Delimiter "`t" $extensionPath)
$actualExtensionHash = (Get-FileHash -Algorithm SHA256 $extensionPath).Hash

if ($legacy.Count -ne 121) {
    throw "Expected 121 legacy keyboard rows including unknown, found $($legacy.Count)."
}
if ($extensions.Count -ne 127) {
    throw "Expected 127 extension rows, found $($extensions.Count)."
}
if ($actualExtensionHash -ne $expectedExtensionHash) {
    throw "Frozen extension table hash changed: $actualExtensionHash"
}

$expectedIds = 1..127
$actualIds = @($extensions | ForEach-Object { [int]$_.ExtensionId })
if (@(Compare-Object $expectedIds $actualIds).Count -ne 0) {
    throw 'Extension IDs must be exactly 1..127.'
}

$entries = [System.Collections.Generic.List[object]]::new()
foreach ($item in $legacy.GetEnumerator()) {
    if ($item.Key -ne 'key.keyboard.unknown') {
        $entries.Add([pscustomobject]@{ Name = $item.Key; Code = [int]$item.Value })
    }
}
foreach ($item in $extensions) {
    $expectedCode = 0x01000000 -bor [int]$item.ExtensionId
    if ([int]$item.CanonicalCode -ne $expectedCode) {
        throw "Extension code mismatch for $($item.KeyName)."
    }
    $entries.Add([pscustomobject]@{ Name = $item.KeyName; Code = $expectedCode })
}

$duplicateNames = @($entries | Group-Object Name | Where-Object Count -ne 1)
$duplicateCodes = @($entries | Group-Object Code | Where-Object Count -ne 1)
$legacyValues = @($legacy.Values)
$extensionCollisions = @($extensions | Where-Object {
    $code = [int]$_.CanonicalCode
    $code -in $legacyValues -or ($code -ge 0 -and $code -le 7)
})
if ($duplicateNames.Count -ne 0 -or $duplicateCodes.Count -ne 0 -or $extensionCollisions.Count -ne 0) {
    throw 'Canonical key table contains a duplicate or collision.'
}

$entryLines = $entries |
    Sort-Object Name |
    ForEach-Object { '            Map.entry("{0}", {1})' -f $_.Name, $_.Code }
$entryBlock = $entryLines -join ",`n"

$template = @'
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
__KEYBOARD_ENTRIES__
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
'@

$content = $template.Replace('__KEYBOARD_ENTRIES__', $entryBlock) -replace "`r`n", "`n"
$outputDirectory = Split-Path -Parent $outputPath
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
[System.IO.File]::WriteAllText($outputPath, $content, [System.Text.UTF8Encoding]::new($false))

Write-Output "Generated $outputPath"
Write-Output "Legacy rows: $($legacy.Count); extension rows: $($extensions.Count); table SHA-256: $actualExtensionHash"
