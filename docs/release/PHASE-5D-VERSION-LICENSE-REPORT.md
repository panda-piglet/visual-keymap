# Phase 5D-B Version Synchronization and License Packaging Report

## 1. Scope and baseline

This phase synchronized the approved Community Beta version, audited the inherited license and attribution evidence, configured license packaging, and attempted a clean candidate build. It did not change production Java code, input/snapshot behavior, dependency versions, workflow permissions/triggers, release notes or changelog content.

- Branch: `port/mc-26.3`
- Starting commit: `cdd20b6f13c0fa35744bdc60b746d0e84f7604a3`
- Approved mod version: `1.4.1-community-beta.1`
- Minecraft target: `26.3`
- Planned tag (not created): `v1.4.1-26.3-community-beta.1`
- Existing private/untracked evidence file preserved: `docs/bugfixes/EVIDENCE-INVENTORY.md`

## 2. Files changed

| File | Change | Scope |
| --- | --- | --- |
| `gradle.properties` | Changed only `mod_version`, from `1.4.0` to `1.4.1-community-beta.1`. | Version configuration |
| `build.gradle` | Added `LICENSE` and `NOTICE` to the normal `jar` task at exact paths `META-INF/LICENSE` and `META-INF/NOTICE`. | Packaging only |
| `NOTICE` | Preserved upstream attribution and source links; clarified community modification categories, non-official status, source availability and that NOTICE does not alter the upstream grant. | Attribution |
| `scripts/release-preflight.ps1` | Strengthened existing checks so the exact JAR entries must hash-match the repository files, rather than merely exist. | Release verification |
| `test-fixtures/porting-26.3/production-smoke/build.gradle` | Removed the active hard-coded `1.4.0` JAR name; the harness now derives archive base, Minecraft version and mod version from root `gradle.properties`. | Test infrastructure |
| `test-fixtures/porting-26.3/production-smoke/README.md` | Documented the derived production JAR name. | Test documentation |
| `docs/release/PHASE-5D-VERSION-LICENSE-REPORT.md` | Added this report. | Documentation |

No historical report was globally rewritten. Active non-documentation references to the old `1.4.0` artifact are now zero; historical evidence continues to identify artifacts and versions that actually existed at the time.

## 3. Version source and synchronization

The project has one production version input:

1. `gradle.properties` defines `mod_version`.
2. `build.gradle` assigns `version = project.mod_version`.
3. `processResources` declares the version input and expands `${version}` in `src/main/resources/fabric.mod.json`.
4. `base.archivesName` is `${archives_base_name}-${minecraft_version}`; Gradle appends `project.version` to the artifact name.

The expected candidate name is therefore:

`visual-keymap-26.3-1.4.1-community-beta.1.jar`

Static synchronization status:

| Item | Result | Evidence |
| --- | --- | --- |
| Gradle project version | PASS | `mod_version=1.4.1-community-beta.1` |
| Resource template version source | PASS | `${version}` still expands from `project.version` |
| Minecraft compatibility | PASS | `minecraft_version=26.3`; template remains `minecraft: ~26.3` |
| Loader/Fabric/optional dependency versions | PASS | Values are unchanged: Loader `0.20.1`, Fabric API `0.165.0+26.3`, MaLiLib `0.30.2`, CommandKeys Curse file `9106946`, Mod Menu `21.0.0` |
| Mod ID, entrypoints and configuration/snapshot schema | PASS | No relevant source or metadata field changed |
| Built JAR metadata | BLOCKED | No new JAR was produced because Gradle failed before task execution |

## 4. GPLv3 audit

This section records repository evidence and is not legal advice.

### 4.1 Upstream evidence

- The root `LICENSE` is byte-for-byte represented by the same Git blob as the file at upstream baseline `053e736c77b15e0727c93abed7c94c3b0fb64618`: `f288702d2fa16d3cdf0035b15a9fcbc552cd88e7`.
- The file is the complete GNU General Public License, version 3, dated 29 June 2007. It was not modified.
- The upstream baseline `fabric.mod.json` says `"license": "GPL-3.0"` and names XeF2 as author.
- The Java source has no separate per-file license header granting “or any later version”.
- Fabric recommends SPDX identifiers for the `license` metadata field. SPDX lists deprecated `GPL-3.0` as the former identifier for **GNU GPL v3.0 only**, while the deprecated “or later” form is `GPL-3.0+` and the current explicit form is `GPL-3.0-or-later`.

Audit conclusion: the strongest repository evidence supports **GPLv3-only**, not GPLv3-or-later. The existing `GPL-3.0` metadata and unmodified license text were preserved, so this phase does not narrow or expand the upstream grant. If the project later wants to modernize the deprecated metadata to `GPL-3.0-only`, that wording change should receive explicit maintainer/legal review; it is not required for this beta and is recorded as **NEEDS REVIEW**, not silently applied.

Relevant primary references:

- [GNU GPLv3 text](https://www.gnu.org/licenses/gpl-3.0.html): modified-source conditions include prominent modification notices and preserving license notices; object-code distribution requires corresponding-source access under an allowed method.
- [GNU guidance on license notices](https://www.gnu.org/licenses/gpl-howto.en.html): “or any later version” is expressed explicitly in the program notice when intended.
- [SPDX License List](https://spdx.org/licenses/): deprecated `GPL-3.0` maps to GPL-3.0-only; current identifiers distinguish `GPL-3.0-only` and `GPL-3.0-or-later`.
- [Fabric `fabric.mod.json` documentation](https://docs.fabricmc.net/develop/loader/fabric-mod-json): the license field should use SPDX identifiers to aid automated tools.

### 4.2 Attribution and modification notice

- `fabric.mod.json` continues to identify XeF2 as author and links the original project/homepage/issues.
- `NOTICE` identifies Visual Keymap, XeF2 and upstream contributors without inventing contributor names.
- `NOTICE` identifies the upstream repository and the community fork.
- It states that the 2026 community work covers the Minecraft 26.3 port, compatible optional-mod integration updates, snapshot-import safety fixes and release verification tooling.
- It explicitly says community builds are not official releases by the original author.
- It does not claim the file name `NOTICE` is mandated by GPLv3 and does not purport to replace or change the license grant.

Repository file hashes after this phase:

- `LICENSE`: SHA-256 `230184F60BAE2FEAF244F10A8BAC053C8FF33A183BCC365B4D8B876D2B7F4809`, 35,823 bytes.
- `NOTICE`: SHA-256 `2E9AF7EFAC9A04110299F2BAB2CE7C10B37815DF0683511A3518F37ABCE9259A`, 979 bytes.

### 4.3 Corresponding source requirement

The planned GitHub pre-release must identify the exact source commit/tag used for the binary and keep the corresponding source/build scripts available alongside equivalent download access. No tag or public binary was created in this phase. The planned source-availability obligation remains a release checklist item.

## 5. Third-party component audit

Static build evidence:

- No Gradle `include`, shadow or shade configuration embeds external dependency JARs.
- MaLiLib, CommandKeys and Mod Menu are `compileOnly` plus optional `runtimeOnly` development/runtime dependencies.
- Tweakeroo is a development runtime provider only.
- JUnit is test-only.
- Fabric Loader and Fabric API are resolved for development/runtime; the mod metadata requires Fabric API rather than embedding it.
- The original icon and language resources are inherited project assets; no separate license file or contrary resource notice exists in the upstream baseline.
- The previously inspected `1.4.0` production JAR had no nested JARs, but that artifact is not evidence for the newly configured beta package.

Conclusion: no additional third-party binary or license notice is statically expected inside the Visual Keymap JAR. Actual confirmation for the new beta JAR is **BLOCKED** until a successful build; Preflight will reject nested JARs and recognized test probes.

## 6. JAR packaging configuration

The normal Gradle `jar` task now copies the complete root files to:

- `LICENSE` → `META-INF/LICENSE`
- `NOTICE` → `META-INF/NOTICE`

The source files remain at the repository root. No resource template, mod class, entrypoint or runtime behavior was changed. The exact destination paths do not overlap current resources.

Preflight now requires both exact entries and compares each JAR entry SHA-256 with its repository source file. Empty, stale, renamed or altered entries cannot satisfy the check.

Packaging configuration: **PASS (static)**.

Actual JAR contents: **BLOCKED (no new build)**.

## 7. Build and test attempt

Command attempted with the installed GraalVM JDK 25.0.4, Gradle Wrapper and existing user Gradle cache:

```powershell
.\gradlew.bat clean test build --no-daemon --stacktrace
```

Result: **BLOCKED by execution environment**.

Gradle 9.8.2 attempted to start the required single-use daemon and failed before executing `clean`, compilation or tests:

```text
java.io.IOException: Unable to establish loopback connection
Caused by: java.net.SocketException: Invalid argument: connect
```

This is the same host-level loopback failure recorded in Phase 5D-A. It is not a Java compilation, test or packaging failure. In accordance with the task boundary, no repeated daemon/network workaround was attempted.

Consequences:

- `clean test build`: BLOCKED; no current test result.
- JUnit execution: NOT TESTED in this phase.
- New production JAR: NOT PRODUCED.
- New JAR size/SHA-256: NOT AVAILABLE.
- New embedded metadata/LICENSE/NOTICE: NOT TESTED dynamically.
- The existing `visual-keymap-26.3-1.4.0.jar` is explicitly stale and is not a Phase 5D-B artifact.

## 8. Preflight result

The strengthened script parsed successfully and its failure path was executed after the blocked build.

Overall result: **NOT READY** (expected and correct).

| Check | Result | Meaning |
| --- | --- | --- |
| Gradle property version | PASS | Root configuration is `1.4.1-community-beta.1`. |
| Minecraft property | PASS | Target remains `26.3`. |
| Build/test outcome | FAIL | Build was blocked before tasks. |
| JAR metadata version | FAIL | Only stale `1.4.0` JAR exists; it was not accepted. |
| `META-INF/LICENSE` | FAIL | Stale JAR lacks it. |
| `META-INF/NOTICE` | FAIL | Stale JAR lacks it. |
| Environment report | FAIL for Gradle | Wrapper invocation encountered the same host constraint. |

This demonstrates that the gate does not confuse configuration completion or an old artifact with a qualified candidate. No qualified artifact was generated or uploaded.

## 9. Unresolved items and risks

| Item | Status | Release impact |
| --- | --- | --- |
| Fresh Java/Gradle build | BLOCKED locally | Must pass on a functioning local host or GitHub-hosted runner. |
| All JUnit tests under the new version | NOT TESTED | Must pass before candidate approval. |
| Generated `fabric.mod.json` version and Minecraft range | NOT TESTED dynamically | Preflight must confirm `1.4.1-community-beta.1` and `~26.3`. |
| Exact embedded LICENSE/NOTICE bytes | NOT TESTED dynamically | Preflight hash checks must pass. |
| Final JAR filename, size and SHA-256 | NOT AVAILABLE | Must be recorded from the successful candidate build. |
| No nested/test/third-party content in new JAR | PARTIAL (static only) | Must be confirmed from the new JAR. |
| Hosted CI/Preflight | NOT TESTED | Requires a separately authorized push and manual workflow run. |
| Modernizing `GPL-3.0` to `GPL-3.0-only` | NEEDS REVIEW, non-blocking for preservation | Do not change without explicit approval; current upstream expression remains intact. |

No new evidence indicates a production-code, data-integrity or dependency-compatibility regression.

## 10. Next required work

1. On a GitHub-hosted runner or local environment without the loopback defect, run `clean test build` using Java 25.
2. Confirm all JUnit tests execute and pass.
3. Run Release Preflight on the exact commit and require overall **READY**.
4. Inspect/download the Actions candidate artifact and record its exact name, size and SHA-256.
5. Verify the generated `fabric.mod.json`, exact LICENSE/NOTICE bytes, absence of nested/test content and source/commit relationship.
6. Only after maintainer review may a later, separately authorized phase create the planned tag or GitHub Pre-release.

## 11. Acceptance conclusion

**Overall Phase 5D-B result: PARTIAL**

- **Configuration modification completed:** **PASS**. The approved version is synchronized, active test-harness naming derives from the same source, license packaging is configured, NOTICE is reviewed, and Preflight verifies exact file contents.
- **Actual build verification completed:** **BLOCKED**. The host loopback failure occurred before Gradle tasks; no new JAR or current test run exists.
- **Final release preparation completed:** **NO / NOT READY**. Hosted build, tests, dynamic JAR inspection, final SHA-256 and maintainer artifact review remain mandatory.

No tag, release, remote push or external-platform publication was performed.
