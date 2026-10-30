# Porting test fixtures

This directory contains synthetic, non-user data for parser and compatibility tests. It is separate from production resources and is not copied into the mod JAR.

- `snapshots/legacy-v1.json` covers established keyboard, mouse, empty-binding, modifier, and F25 values.
- `snapshots/known-extension-v1.json` covers the first and last frozen 26.3 extension codes.
- `snapshots/unknown-extension-v1.json` covers an unassigned extension code that must be preserved but not applied.
- `config/` contains synthetic Visual Keymap config inputs. They document inputs only; 26.2 runtime results still need to be recorded before they become behavioral golden files.

The provider and binding IDs are deliberately prefixed with `fixture.`. These files are not substitutes for a snapshot exported by a real 26.2 client, because they do not exercise provider enumeration, default filtering, file dialogs, or persistence.

Do not commit real `options.txt`, account/session files, logs, screenshots containing server addresses or chat, or third-party configs before reviewing them for usernames, UUIDs, server addresses, commands, and message text.
