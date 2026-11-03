# Visual Keymap 26.3 Community Pre-release Plan

## 1. Release objective

Prepare the first public community-maintained pre-release for Minecraft 26.3. This phase prepares documentation and decisions only; it does not approve a tag or publish an artifact.

Release scope:

- Full Minecraft 26.3 migration of the original Visual Keymap feature set.
- Minecraft, MaLiLib, CommandKeys and Mod Menu integration retained.
- Version 1 snapshot schema and public API retained.
- BUG-01 invalid JSON crash and BUG-02 duplicate binding identity fixed and verified.
- Phase 5B `PARTIAL`, `BLOCKED` and `NOT TESTED` states remain unchanged. The maintainer has accepted the documented residual risk for a pre-release; this is not evidence that those tests passed.

Production code baseline before Phase 5C documentation: `5e69d2383e91b6f2a4a08fe1d6cd065bda1fbad9`. Phase 5C began at `port/mc-26.3@cc8102fc767aada63e01a20a0e87136b1e2c2e41`.

## 2. Current version and upstream convention

- `gradle.properties`: `mod_version=1.4.0`.
- Current artifact: `visual-keymap-26.3-1.4.0.jar`.
- Existing tags use `v<mod version>-<Minecraft version>`, including `v1.4.0-26.1` and `v1.4.0-26.2`.
- `origin` is the community maintainer's fork; `upstream` is `USTC-XeF2/visual-keymap`.
- The current internal `1.4.0` value does not identify the fork or pre-release status.

## 3. Version naming options — maintainer decision required

### Option A — community pre-release suffix (recommended for a fork release)

- Internal mod version: `1.4.0-community-beta.1`
- Git tag: `v1.4.0-26.3-community-beta.1`
- Release title: `Visual Keymap 1.4.0 for Minecraft 26.3 — Community Pre-release 1`
- Platform version type: `beta`

This keeps the upstream 1.4.0 lineage visible while making the binary, logs, tag and player-facing title distinguishable from an upstream release. The exact string must be validated with Fabric Loader and the publishing workflow after approval.

### Option B — upstream-style pre-release

- Internal mod version: `1.4.0-beta.1`
- Git tag: `v1.4.0-26.3-beta.1`
- Release title explicitly includes `Community-maintained`

This is closer to upstream naming but is less distinctive inside the client and may be confused with a future upstream beta.

### Option C — patch increment

- Internal mod version: `1.4.1-beta.1`
- Git tag: `v1.4.1-26.3-community-beta.1`

This makes the bug fixes visible as a patch increment, but it claims a new upstream-style semantic version and may conflict with a future official 1.4.1. It is not recommended for the first fork release without upstream agreement.

No version field has been changed in Phase 5C.

## 4. Release channel — maintainer decision required

Recommended first channel: **GitHub Pre-release on the community fork only**.

Reasons:

- The fork can clearly disclose community-maintained status and attach exact source for the binary.
- The current workflow contains the original Modrinth project ID `uJWhARde` and CurseForge project ID `1401807`; a fork tag must not attempt to publish to those projects without explicit authorization.
- A public `v*` tag immediately triggers `.github/workflows/release.yml`, so no test tag is safe.

Other valid choices:

1. Submit the port upstream and let the original author publish on official channels.
2. Create separate community Modrinth/CurseForge projects after confirming naming, project ownership, metadata links and platform policies.

Before any fork tag, the release workflow needs an approved channel-specific change. For a pre-release, `mc-publish` supports `version-type: beta` and `github-prerelease: true`; the latter otherwise defaults from the parsed version type. GitHub's release API also has an explicit `prerelease` flag. See the [mc-publish inputs](https://github.com/marketplace/actions/mc-publish#inputs) and [GitHub release API](https://docs.github.com/en/rest/releases/releases).

## 5. License and source compliance audit

This is a practical release checklist, not legal advice.

Confirmed:

- The repository retains the complete GNU GPL version 3 text in `LICENSE`.
- Original author `XeF2` and upstream source links remain in `fabric.mod.json`.
- `NOTICE` identifies the upstream project and the 2026 community modifications.
- The current JAR does not contain `LICENSE` or `NOTICE`.
- The release JAR does not bundle MaLiLib, CommandKeys, Mod Menu or their implementations.

GPLv3 section 5 requires a modified version to carry prominent modification notices and remain under the GPL; section 6 requires corresponding source when conveying object code. The GNU FAQ also emphasizes that source must correspond to the released binary. See the [GNU GPLv3 text](https://www.gnu.org/licenses/gpl-3.0.html) and [GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.html#DistributeExtendedBinary).

Recommended distribution method:

1. Include `LICENSE` and `NOTICE` inside the release JAR under `META-INF/`.
2. Keep the exact tagged source publicly available in the same GitHub release context.
3. Link the exact tag/commit next to every binary download, including any third-party platform page.
4. Preserve original attribution and clearly mark the build as modified/community-maintained.

Including files inside the JAR requires a small packaging configuration/resource change and therefore a rebuild. This has not been performed pending maintainer approval. Attaching the files only beside the JAR is possible, but embedding them is safer for copies redistributed away from the release page.

The repository metadata says `GPL-3.0`, while the existing Modrinth page has displayed `GPL-3.0-or-later`. Do not choose either the SPDX `-only` or `-or-later` interpretation without confirming the original licensing grant. Until resolved, preserve the repository wording “GNU GPL version 3” and the existing license text rather than changing the grant.

## 6. Artifact and metadata audit

Current already-tested artifact, before any version or packaging change:

- Path: `build/libs/visual-keymap-26.3-1.4.0.jar`
- Size: 107,834 bytes
- SHA-256: `9C70445275A5AE16AF41788244B5D9C4506746802679BD8576FB788D671BB928`
- Fabric metadata: client-only; Minecraft `~26.3`; Fabric Loader `>=0.20.1`; Fabric API required; client and Mod Menu entrypoints present; required Mixin configured.
- Contents audit: no test classes, test provider, nested third-party JAR, MaLiLib, CommandKeys or Mod Menu implementation.

The JAR predates the Phase 5A–5C documentation commits, but production code and metadata have not changed since it was tested. It is evidence for readiness, not yet the final release artifact.

Recorded build command:

```powershell
.\gradlew.bat clean test build --no-daemon
```

Last verified result: 39/39 JUnit PASS, with P1–P6 integration evidence recorded in the Phase 4 and bug acceptance reports.

No rebuild is necessary for documentation-only changes. After approving and changing `mod_version`, release metadata, workflow or JAR license packaging, run the clean build and production matrix again and record a new SHA-256.

## 7. Accepted pre-release risks

The maintainer has authorized Phase 5C while preserving the original test statuses. The following are accepted pre-release risks, not PASS results:

- R-03: export to an unwritable target not dynamically verified; successful overwrite of an invalid file is verified.
- R-11: CommandKeys profile, message-search, complete GUI editing and actual macro-trigger matrix remain partial.
- R-12: multiple unbound macros were retained, but single-object edit isolation and the 26.2 comparison remain partial.
- R-13: representative conflicts, complex restart persistence and coexistence passed, but a specifically documented cross-provider joint-edit matrix remains partial.
- R-01/R-14/R-20 and rare hardware cases remain as listed in `KNOWN-ISSUES.md` and `TEST-EVIDENCE-MATRIX.md`.

Any newly confirmed general crash, irreversible configuration loss or severe reproducible data corruption would still stop release preparation.

## 8. Final approval sequence

1. Maintainer selects version, tag and release title.
2. Maintainer selects channel: fork GitHub pre-release, upstream release, or separate platform projects.
3. Maintainer approves license packaging (`META-INF/LICENSE` and `META-INF/NOTICE` recommended).
4. Apply only the approved version/metadata/workflow/packaging changes.
5. Run clean test/build and production JAR audit; record final commit, size and SHA-256.
6. Review and approve `RELEASE-NOTES.md` and `KNOWN-ISSUES.md`.
7. Confirm no private files are tracked and the tag points to the approved commit.
8. Only then create the tag and publish; these actions require a separate authorization.

## 9. Decisions still required

- Version number and exact tag.
- Release channel and whether official platform IDs may be used.
- Whether LICENSE/NOTICE are embedded in the JAR or distributed beside it.
- Final wording of the release notes and community-maintained attribution.
- Whether the first pre-release is called `beta.1`, `rc.1` or another approved label.
