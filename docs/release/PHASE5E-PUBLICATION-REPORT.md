# Phase 5E Community Beta 1 Publication Report

## 1. Final status

**PUBLISHED — GitHub Pre-release**

Visual Keymap `1.4.1-community-beta.1` for Minecraft 26.3 was published from the community-maintained fork. It was not marked Stable and was not published to Modrinth or CurseForge.

| Item | Final value |
| --- | --- |
| Repository | `panda-piglet/visual-keymap` |
| Release | [Visual Keymap 1.4.1 — Minecraft 26.3 Community Beta 1](https://github.com/panda-piglet/visual-keymap/releases/tag/v1.4.1-26.3-community-beta.1) |
| Release database ID | `433123891` |
| Release state | Public, `draft=false`, `prerelease=true` |
| Published UTC | `2026-11-03T19:25:50Z` |
| Version | `1.4.1-community-beta.1` |
| Tag | `v1.4.1-26.3-community-beta.1` |
| Annotated tag object | `d4b9e9c1edc0fef10aee8f79a0235e9a2fa79ed2` |
| Tagged commit | `0469e705623ae022893951255f92bb152f4c9dfc` |

Although the GitHub Release API reports `target_commitish: main`, the Release uses an already-existing annotated tag. The tag's peeled target was independently verified as `0469e705623ae022893951255f92bb152f4c9dfc`; source archives and release identity therefore resolve through the approved tag, not the older default-branch head.

## 2. Build and CI evidence

- Windows local build: `clean test build` completed successfully using Java 25 and Gradle 9.8.2.
- JUnit: 39 tests, 0 failures, 0 errors and 0 skipped.
- Release Preflight: **READY** for commit `0469e705623ae022893951255f92bb152f4c9dfc`.
- GitHub-hosted Ubuntu CI: **SUCCESS**.
- CI run: [Continuous Integration #41701156857](https://github.com/panda-piglet/visual-keymap/actions/runs/41701156857).
- CI `Build and run tests` and Gradle `build` tasks completed successfully.

The final commit includes the test-only line-ending normalization required to make the fixed TSV hash check identical on Windows CRLF and Linux LF checkouts. No production Java, keycode table, snapshot schema, public API or adapter behavior changed in that commit.

## 3. Published artifacts

| Asset | Size | SHA-256 |
| --- | ---: | --- |
| [visual-keymap-26.3-1.4.1-community-beta.1.jar](https://github.com/panda-piglet/visual-keymap/releases/download/v1.4.1-26.3-community-beta.1/visual-keymap-26.3-1.4.1-community-beta.1.jar) | 120,908 bytes | `CCFB834361257FF580D13A3510ED20EAEEA08B2070BA50D84534280B0BC46A67` |
| [SHA256SUMS.txt](https://github.com/panda-piglet/visual-keymap/releases/download/v1.4.1-26.3-community-beta.1/SHA256SUMS.txt) | 112 bytes | `052D52B171B6C7D84324F68E62736289F2548CE04BC19F6867B856A70EC53875` |

The JAR was downloaded once while the Release was a draft and again after publication. Both downloaded copies were 120,908 bytes and matched the approved SHA-256 exactly. GitHub's asset digest also reports the same JAR SHA-256.

## 4. Release material verification

- Release title, version, Minecraft 26.3 target and tag are consistent.
- The Release is explicitly identified as a community-maintained, unofficial upstream Pre-release.
- The body records MaLiLib, CommandKeys and Mod Menu support.
- BUG-01 and BUG-02 are identified and described.
- Version 1 snapshot compatibility, duplicate-ID identity ambiguity and CommandKeys snapshot exclusion are disclosed.
- Rare hardware and partially tested edge conditions remain disclosed.
- GPLv3, upstream attribution and community-fork source links are present.
- Tagged `README.md`, `LICENSE`, `NOTICE` and `docs/release/KNOWN-ISSUES.md` are present.
- The production JAR contains matching `META-INF/LICENSE` and `META-INF/NOTICE`, no test probes and no nested third-party JARs, as verified by Preflight.

Before publication, the draft body was corrected to identify BUG-01/BUG-02 explicitly and to record the final tagged source commit rather than the earlier byte-identical candidate-build commit. No version, tag or artifact changed.

## 5. Post-publication checks

| Check | Result |
| --- | --- |
| Public Release page | PASS — unauthenticated HTTP 200 |
| Public JAR URL | PASS — unauthenticated HTTP 200 |
| Pre-release flag | PASS — `true` |
| Draft flag | PASS — `false` |
| Tag target | PASS — approved commit `0469e705…` |
| Matching Release count | PASS — exactly one |
| JAR re-download hash | PASS — approved SHA-256 |
| Private evidence in tagged tree | PASS — 0 matching private entries |
| Stable/Latest promotion | Not performed |
| Modrinth/CurseForge publication | Not performed |

## 6. Remaining maintainer actions

No publication blocker remains. The maintainer may monitor player feedback and GitHub issues for this Community Beta. Promotion to Stable, publication to another platform, or development of a later version requires a separate decision and is outside Phase 5E.
