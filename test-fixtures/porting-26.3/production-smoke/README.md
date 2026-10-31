# Phase 4 production-JAR smoke harness

This standalone Loom project loads `build/libs/visual-keymap-26.3-1.4.0.jar` as an external Fabric mod. It deliberately does not include the repository's `sourceSets.main` output.

Run a matrix profile from the repository root after building the release JAR:

```powershell
.\gradlew.bat -p test-fixtures/porting-26.3/production-smoke runClient -Pphase4Profile=P1 --no-daemon
```

Profiles P1 through P6 correspond to `docs/porting-26.3/TEST-PLAN.md`. Each profile writes `runs/pN/phase4-production-smoke.txt` below this harness and then stops the client.

Add `-Pphase4Manual=true` to leave the client open after the startup assertions for manual GUI and physical-input regression testing.
