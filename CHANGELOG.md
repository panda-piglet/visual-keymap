# Changelog

All notable changes to the community-maintained Minecraft 26.3 port are documented here.

## 1.4.1-community-beta.1 — Minecraft 26.3 Community Beta 1

### Changed

- Ported Visual Keymap from Minecraft 26.2 to Minecraft 26.3, Fabric Loader 0.20.1+, Fabric API 0.165.0+26.3 and Java 25.
- Adapted keyboard input, rendering, file dialogs, Mixins and native Minecraft key bindings to the Minecraft 26.3 APIs.
- Added a stable canonical encoding for Minecraft 26.3 keys that were not representable by the legacy integer set, while retaining the public `List<Integer>` contract and version 1 snapshot schema.
- Updated optional integrations for MaLiLib 0.30.2, CommandKeys 3.104.1+26.3 Fabric and Mod Menu 21.0.0.

### Fixed

- Invalid or structurally damaged JSON snapshots now show a localized error instead of crashing the Minecraft client.
- Snapshot imports no longer create many-to-many matches when different MaLiLib mods use the same provider and binding ID. Ambiguous entries are not applied by default and can be resolved with an explicit one-to-one selection.

### Compatibility

- Version 1 snapshots remain readable and writable without a schema change.
- CommandKeys bindings remain excluded from snapshot exports, matching the original behavior.
- F25's legacy value remains a tombstone, and unknown extension values are preserved as data but cannot be applied to a live binding.

### Pre-release notes

- This is a community-maintained pre-release, not a stable upstream release.
- See `docs/release/KNOWN-ISSUES.md` for accepted test gaps and historical format limitations.
