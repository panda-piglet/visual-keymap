# Known Issues and Accepted Pre-release Risks

This document distinguishes confirmed limitations from test coverage gaps. A `PARTIAL`, `BLOCKED` or `NOT TESTED` item is not automatically a defect, and no such item has been changed to `PASS` for release preparation.

## Snapshot format limitations

- Version 1 identifies a binding by provider and binding ID. Different MaLiLib mods can use the same pair, so an old snapshot may be ambiguous. Visual Keymap now defaults ambiguous entries to not applied and requires an explicit one-to-one selection.
- Version 1 serializes modifiers but historically does not compare or apply them during import. This behavior is preserved for compatibility.
- CommandKeys macros are not included in Visual Keymap snapshot exports. This is original behavior, not a migration regression.
- Unknown snapshot versions retain the original acceptance behavior. A new schema/version policy is outside this release.
- F25's old integer value `314` is retained as a tombstone and is not remapped. Unknown extension values cannot be applied to live bindings.

## Accepted manual-test gaps

- **R-03:** normal export, cancellation and overwriting an invalid file are verified. Export to a deliberately unwritable target has not been dynamically verified.
- **R-11:** CommandKeys object-level behavior and persistence probes passed, and snapshot exclusion is verified. The full matrix of active-profile UI, message search, limit+main-key editing and actual macro triggering remains partial.
- **R-12:** multiple unbound CommandKeys macros were retained without observed loss. Editing only one while proving isolation from every other unbound macro, plus a Minecraft 26.2 comparison, remains partial.
- **R-13:** multi-mod coexistence, representative conflicts and complex configurations across repeated restarts passed. A separately documented cross-provider conflict and three-provider joint-edit session remains partial.
- **R-01/R-14:** Controls and Mod Menu entrypoints work, but every language, GUI scale, Done/Esc and repeated-parent-screen combination has not been exhausted.
- **R-20:** an external `VisualKeymapApi` provider compiles, is discovered and loads; model/callback tests pass. Its complete GUI tooltip/edit/Reset/save/snapshot callback cycle has not received a separate manual pass.

## Hardware coverage

- F25, M6–M8 and every Minecraft 26.3-only physical key were not tested on matching hardware.
- Canonical encoding, collision checks, round trips, data preservation and non-applicability protections are automated and passed. Missing hardware coverage does not imply an encoding failure.

## Not known issues

- BUG-01 (invalid JSON import crash) is fixed and verified with 39/39 JUnit coverage plus ten maintainer-executed client tests.
- BUG-02 (duplicate binding ID many-to-many snapshot matching) is fixed and verified with automated tests plus eight maintainer-executed client tests.
- MaLiLib four-key editing and use, representative hotkey conflicts, complex configurations across multiple full restarts, CommandKeys snapshot exclusion, retention of multiple unbound macros, external provider loading and export overwrite were all exercised without a reported failure.

If a general crash, irreversible configuration loss or reproducible data corruption is discovered, treat it as a pre-release blocker and include logs, affected configuration copies and exact steps in the report.
