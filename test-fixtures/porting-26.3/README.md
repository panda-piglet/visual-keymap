# Porting test fixtures

This directory contains synthetic, non-user data for parser and compatibility tests. It is separate from production resources and is not copied into the mod JAR.

- `snapshots/legacy-v1.json` covers established keyboard, mouse, empty-binding, modifier, and F25 values.
- `snapshots/known-extension-v1.json` covers the first and last frozen 26.3 extension codes.
- `snapshots/unknown-extension-v1.json` covers an unassigned extension code that must be preserved but not applied.
- `snapshots/real-26.2-malilib-v1.json` is a semantic test copy of a snapshot exported and re-imported by the real 26.2 client. It covers MaLiLib `tweakFreeCamera = J (74)` and `tweakHoldAttack = F9 (298)`. The repository copy adds one terminal LF and therefore is not presented as the original byte stream. The authoritative, unmodified capture has SHA-256 `C883934384A7676649C4222D34C4381472DF5D89F5E34D8455A0ED14D116500F`; see `docs/porting-26.3/BASELINE-VALIDATION.md` for provenance and scope.
- `config/` contains synthetic Visual Keymap config inputs. They document inputs only; 26.2 runtime results still need to be recorded before they become behavioral golden files.

The provider and binding IDs in the synthetic snapshots are deliberately prefixed with `fixture.`. Those synthetic files are not substitutes for a real 26.2 export because they do not exercise provider enumeration, default filtering, file dialogs, or persistence. The separately identified `real-26.2-malilib-v1.json` has real-client provenance but intentionally covers only its two recorded MaLiLib bindings.

Do not commit real `options.txt`, account/session files, logs, screenshots containing server addresses or chat, or third-party configs before reviewing them for usernames, UUIDs, server addresses, commands, and message text.
