# Phase 5D-C Community Beta Candidate Report

## 1. Result

**READY — candidate build and Release Preflight passed.**

The Community Beta candidate was built and verified from:

- Branch: `port/mc-26.3`
- Candidate source commit: `fadd4a82b01242cd83c5ac28592c4c65921b1e99`
- Version: `1.4.1-community-beta.1`
- Minecraft: `26.3`
- Production JAR: `build/libs/visual-keymap-26.3-1.4.1-community-beta.1.jar`

No production source, version, license grant or Preflight acceptance rule was changed after this candidate build. This report is a later documentation-only record; the candidate remains attributed to the commit above.

## 2. Build environment

The maintainer executed the build in an ordinary Windows PowerShell environment outside the restricted Codex command sandbox.

| Component | Actual value |
| --- | --- |
| Operating system | Windows 11 10.0 amd64 |
| Java runtime | GraalVM JDK 25.0.4+7.1, Java 25.0.4 |
| Java compiler | `javac 25.0.4` |
| Gradle Wrapper | 9.8.2, revision `8de7bcf76e785507ad92f0741ab9235a430c95e6` |
| Fabric Loom | 1.19.3 |
| Fabric Loader configuration | 0.20.1 |
| Fabric API configuration | 0.165.0+26.3 |

The build used a dedicated writable TEMP/TMP directory and the configured Java 25 installation. No JDK reinstall, operating-system network/security change or firewall modification was required. The loopback failure was limited to the Codex execution environment and did not reproduce in the normal PowerShell environment.

## 3. Build and tests

Command:

```powershell
.\gradlew.bat clean test build --no-daemon --stacktrace
```

Result:

- Gradle: **BUILD SUCCESSFUL**.
- Tasks: 9 actionable tasks, all executed in the first successful captured run; the final candidate was rebuilt after the Preflight compatibility fixes.
- JUnit XML result files: 7 suites.
- Tests: **39**.
- Failures: **0**.
- Errors: **0**.
- Skipped: **0**.

The test set includes the existing BUG-01 invalid-input regression coverage (`KeymapSnapshotReadTest`, `ImportFailureFlowTest`, localization checks) and BUG-02 duplicate-identity/matching coverage (`KeymapSnapshotMatchingTest`, `ImportSelectionTest`). All passed together with canonical input and MaLiLib conversion tests.

The local XML file's machine-name attribute could not be consumed by Windows PowerShell's direct `[xml]` conversion in the first helper command because the non-ASCII hostname was malformed in that attribute. This did not affect Gradle's execution or HTML report. Counts were independently recovered from all seven suite attributes and agreed with `build/reports/tests/test/index.html`: 39 tests and zero failures. The final count extraction had zero unparsed suites.

Gradle reported deprecated features that will become incompatible with Gradle 10. This is a future toolchain-maintenance warning, not a failure under the pinned Gradle 9.8.2 build.

## 4. Candidate artifact

| Property | Verified value |
| --- | --- |
| Filename | `visual-keymap-26.3-1.4.1-community-beta.1.jar` |
| Size | 120,908 bytes |
| SHA-256 | `CCFB834361257FF580D13A3510ED20EAEEA08B2070BA50D84534280B0BC46A67` |
| Source commit | `fadd4a82b01242cd83c5ac28592c4c65921b1e99` |
| Local path | `build/libs/visual-keymap-26.3-1.4.1-community-beta.1.jar` |

A sources JAR was also generated as `visual-keymap-26.3-1.4.1-community-beta.1-sources.jar`; it is not substituted for the production JAR in any verification result.

The candidate JAR remains in `build/libs/` for maintainer review. It was not uploaded, tagged or published.

## 5. Fabric metadata verification

The generated `fabric.mod.json` inside the production JAR was parsed successfully.

| Field | Result |
| --- | --- |
| Mod ID | `visual-keymap` — PASS |
| Version | `1.4.1-community-beta.1` — PASS |
| Minecraft dependency | `~26.3` — PASS |
| Fabric Loader dependency | `>=0.20.1` — PASS |
| Metadata JSON | Present and valid — PASS |

The active dependency configuration remains unchanged from Phase 5D-B. A Git comparison from the BUG-01 final-acceptance production baseline `5e69d2383e91b6f2a4a08fe1d6cd065bda1fbad9` through the candidate commit found no change under `src/main/java` or `src/main/resources`. The public API package has no diff. Version/build/report changes therefore did not alter existing API signatures, adapters, snapshot format or runtime feature logic.

## 6. LICENSE and NOTICE verification

Both required exact entries are present and byte-equivalent to their repository sources:

| Entry | JAR SHA-256 | Repository SHA-256 | Result |
| --- | --- | --- | --- |
| `META-INF/LICENSE` | `230184F60BAE2FEAF244F10A8BAC053C8FF33A183BCC365B4D8B876D2B7F4809` | Same | PASS |
| `META-INF/NOTICE` | `2E9AF7EFAC9A04110299F2BAB2CE7C10B37815DF0683511A3518F37ABCE9259A` | Same | PASS |

The production JAR contains:

- 0 nested third-party JARs.
- 0 entries matching the configured test/testmod/provider-probe checks.
- Complete, non-empty LICENSE and NOTICE files rather than placeholder resources.

## 7. Release Preflight

Command executed in Windows PowerShell:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
    -File '.\scripts\release-preflight.ps1' `
    -BuildResult success `
    -Repository 'panda-piglet/visual-keymap' `
    -Commit 'fadd4a82b01242cd83c5ac28592c4c65921b1e99'
```

Final Preflight result: **READY**.

Every configured check passed:

- Gradle build/test outcome.
- Minecraft and planned mod versions.
- Exactly one production JAR.
- JAR metadata and dependency range.
- Exact LICENSE and NOTICE content hashes.
- No nested third-party JARs.
- No test probes in the production JAR.
- Java and Gradle environment reporting.

Generated verification files:

- `build/release-preflight/RELEASE-VERIFICATION.md`
- `build/release-preflight/BUILD-METADATA.txt`
- `build/release-preflight/SHA256SUMS.txt`

Preflight reported no unmet release conditions.

## 8. Git and workspace state

At candidate build and Preflight time:

- Branch: `port/mc-26.3`.
- HEAD: `fadd4a82b01242cd83c5ac28592c4c65921b1e99`.
- Only pre-existing untracked item: `docs/bugfixes/EVIDENCE-INVENTORY.md`.
- The private evidence file remains untracked and was not included in the build or candidate JAR.
- Build outputs remain ignored artifacts under `build/`.

The two Windows PowerShell compatibility fixes to Preflight were committed separately:

- `2301bc68ea2a4e3412fd26276f305b74748462f3` — path handling without `.NET Path.GetRelativePath()`.
- `fadd4a82b01242cd83c5ac28592c4c65921b1e99` — native stderr capture and UTF-8-no-BOM report output compatible with Windows PowerShell 5.1.

Neither commit changes production Java code or the candidate acceptance criteria.

## 9. Remaining risks and release blockers

### Confirmed release blockers

**None found in the candidate build, automated tests, metadata, packaging or Preflight checks.**

### Non-blocking items

- GitHub-hosted CI and manual Preflight have not yet been exercised because this phase does not authorize a remote push or workflow dispatch. The equivalent local clean build and Preflight are READY.
- Previously accepted Phase 5B partial/manual coverage and rare-hardware limits remain documented; no new failure was found.
- Gradle 10 compatibility will require future maintenance due to current deprecation warnings; the approved build uses Gradle 9.8.2.
- The first helper's direct Windows PowerShell XML conversion issue is a local report-consumption quirk, not a failed JUnit test or product defect.

The remaining actions—remote push, optional hosted workflow confirmation, tag creation and GitHub Pre-release creation—are governance/release approvals, not unresolved candidate defects.

## 10. Recommendation

**Recommend entering final Release approval.**

The exact candidate approved for review is:

```text
Commit: fadd4a82b01242cd83c5ac28592c4c65921b1e99
File: visual-keymap-26.3-1.4.1-community-beta.1.jar
Size: 120908 bytes
SHA-256: CCFB834361257FF580D13A3510ED20EAEEA08B2070BA50D84534280B0BC46A67
Preflight: READY
JUnit: 39 passed, 0 failed, 0 errors, 0 skipped
```

This recommendation does not authorize a push, tag, GitHub Release, Modrinth/CurseForge upload or Stable designation. Those actions still require explicit maintainer approval.
