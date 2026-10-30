package dev.xef2.visualkeymap.input;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

final class CanonicalInputCodeTest {
    private static final Path LEGACY_PROBE = Path.of(
            "docs", "porting-26.3", "evidence", "legacy-extended-key-probe.log"
    );
    private static final Path EXTENSION_MAP = Path.of(
            "docs", "porting-26.3", "evidence", "proposed-26.3-extension-map.tsv"
    );

    @Test
    void validatesCanonicalCodeTablesAndRoundTrips() throws Exception {
        CanonicalInputCodeSelfTest.main(new String[]{
                LEGACY_PROBE.toString(),
                EXTENSION_MAP.toString()
        });
    }
}
