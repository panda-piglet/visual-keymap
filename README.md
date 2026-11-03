# Visual Keymap

English | [简体中文](docs/README.zh-CN.md)

> **Minecraft 26.3 community pre-release:** this branch is a community-maintained port of [XeF2's original Visual Keymap](https://github.com/USTC-XeF2/visual-keymap). It is not a stable release yet.

![Screenshot of Visual Keymap Screen](docs/screenshot.png)

This mod allows you to visually view the currently bound key positions in the game through keyboard layout, and change the key position bindings from various mods in a unified configuration page.

You can open the key map interface through the `Controls - Visual Keymap` menu, or enter the configuration screen of this mod in ModMenu. This mod currently provides integration between MaLiLib and CommandKeys.

## Supported platform

- Minecraft 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API for Minecraft 26.3 (tested with 0.165.0+26.3)
- Java 25

## Installation

1. Install Minecraft 26.3 with Fabric Loader and Fabric API.
2. Put the Visual Keymap JAR in the client's `mods` directory.
3. Open it from `Options - Controls - Visual Keymap`.

Optional integrations tested for this port:

- Mod Menu 21.0.0: opens Visual Keymap from the mod configuration button.
- MaLiLib 0.30.2: exposes hotkeys registered by compatible MaLiLib mods. Tweakeroo 0.30.1 was used for integration testing.
- CommandKeys 3.104.1+26.3 **Fabric**: exposes bindings from the active CommandKeys profile.

These optional mods are not bundled in the Visual Keymap JAR.

## Snapshot compatibility

- The existing version 1 snapshot format remains supported.
- Old version 1 snapshots cannot uniquely identify bindings when different mods use the same provider and binding ID. Visual Keymap shows an ambiguity warning and requires the user to select the intended target; ambiguous entries are not applied by default.
- CommandKeys macros are intentionally not included in Visual Keymap snapshot exports, matching the original behavior.
- Invalid or damaged JSON snapshots are rejected with an in-game error instead of crashing the client.

This is a pre-release. Rare hardware keys and some file-system failure paths have not been fully tested. See [Known Issues](docs/release/KNOWN-ISSUES.md) before testing, and report community-port issues on the [fork issue tracker](https://github.com/panda-piglet/visual-keymap/issues).

## Integration

All mods with independent key binding systems can use this feature by setting entrypoints.

```json
"entrypoints": {
  "visualkeymap": [
    "com.example.mymod.MyModVisualKeymapImpl"
  ]
}
```

The entry point class needs to implement the `dev.xef2.visualkeymap.api.VisualKeymapApi` interface. The specific implementation method can refer to the `dev.xef2.visualkeymap.api.MinecraftImpl` class.

## License and credits

Visual Keymap is licensed under the GNU General Public License version 3. The original project and authorship remain credited to XeF2. This repository contains community-maintained Minecraft 26.3 changes; see [NOTICE](NOTICE) and [LICENSE](LICENSE).
