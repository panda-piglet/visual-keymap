# Visual Keymap for Minecraft 26.3 — Community Pre-release

> Version and release tag are pending maintainer approval. This is a pre-release build, not a stable upstream release.

Visual Keymap has been ported to Minecraft 26.3 while preserving its keyboard visualization, unified binding editor, configuration behavior, public provider API and version 1 snapshot format.

## Highlights

- Supports Minecraft 26.3 with Fabric.
- Retains native Minecraft key binding visualization and editing.
- Retains integrations with MaLiLib, CommandKeys and Mod Menu.
- Supports single-key, mouse and multi-key MaLiLib bindings, including four-key combinations.
- Keeps the existing `visualkeymap` entrypoint and `VisualKeymapApi` provider contract.
- Keeps version 1 snapshot import and export compatibility.

## Important fixes

- Importing invalid or damaged JSON now displays a localized error instead of crashing the client.
- Snapshot import no longer incorrectly cross-matches every binding when different MaLiLib mods use the same provider and binding ID.
- Ambiguous old snapshot entries are not applied by default. The preview identifies the candidates and allows an explicit one-to-one selection.
- Unsupported extension values and the legacy F25 tombstone cannot be applied to an unrelated live key binding.

## Installation

Required:

- Minecraft 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API for Minecraft 26.3; tested with 0.165.0+26.3
- Java 25

Optional integrations tested:

- Mod Menu 21.0.0
- MaLiLib 0.30.2; Tweakeroo 0.30.1 was used as a real hotkey provider
- CommandKeys 3.104.1+26.3 Fabric

Optional mods are not bundled. Install only the integrations you use.

## Snapshot compatibility

- The JSON schema remains version 1.
- Snapshots made by Minecraft 26.2 remain supported for the tested Minecraft and MaLiLib bindings.
- Version 1 does not store enough source identity to automatically distinguish bindings from different mods when they share the same provider and binding ID. Review the ambiguity preview and choose the intended target.
- CommandKeys macros are intentionally excluded from Visual Keymap snapshots, matching the original behavior.
- Snapshot modifiers retain their historical version 1 semantics and are not newly applied by this port.

## Pre-release limitations

- Rare physical keys such as F25 and some extra mouse buttons were verified at the data-safety level but not on every physical device.
- CommandKeys profile/UI/trigger combinations and some external provider GUI callbacks have partial rather than exhaustive manual coverage.
- Export to an unwritable file-system target has not been dynamically verified; normal export and overwriting an invalid file were verified.
- Some language, GUI scale and Mod Menu return-path combinations remain non-exhaustive.

See [KNOWN-ISSUES.md](KNOWN-ISSUES.md) for the precise scope. No unresolved confirmed crash, irreversible configuration loss or general data-corruption defect is currently known.

## Feedback and source

This build is based on [Visual Keymap by XeF2](https://github.com/USTC-XeF2/visual-keymap) and maintained for Minecraft 26.3 in the [community fork](https://github.com/panda-piglet/visual-keymap). Please include the Minecraft/Fabric versions, installed optional mods, logs and reproduction steps when reporting a problem.

Licensed under GNU GPL version 3. See the repository `LICENSE` and `NOTICE` files.
