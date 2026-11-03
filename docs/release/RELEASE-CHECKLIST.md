# Visual Keymap 26.3 Pre-release Checklist

No item in this checklist authorizes publishing by itself.

## A. Maintainer decisions

- [x] Approve internal mod version: `1.4.1-community-beta.1`.
- [x] Approve Git tag `v1.4.1-26.3-community-beta.1` and player-facing title `Visual Keymap 1.4.1 for Minecraft 26.3 — Community Beta 1`.
- [x] Select release channel: community fork GitHub Pre-release only.
- [x] Approve the `community-beta.1` label.
- [ ] Approve final release notes and known issues.
- [x] Embed complete `LICENSE` and `NOTICE` files in the JAR under `META-INF/`.
- [x] Preserve the upstream `GPL-3.0` expression and complete GPLv3 text; do not change it to `-or-later` without an upstream grant.

## B. Source and Git

- [x] Work is on `port/mc-26.3`.
- [x] BUG-01 and BUG-02 merge history is present.
- [x] Original author and upstream repository are preserved in metadata and public documentation.
- [x] `baseline-private/` is not tracked.
- [x] Existing untracked `docs/bugfixes/EVIDENCE-INVENTORY.md` remains outside release commits.
- [x] Working tree contained only approved release changes before the candidate build; the private evidence index remained untracked.
- [x] Candidate source commit is reviewed and recorded as `fadd4a82b01242cd83c5ac28592c4c65921b1e99`.
- [ ] Exact corresponding source is available from the approved public tag/commit.

## C. Version, metadata and workflow

- [x] `mod_version` is `1.4.1-community-beta.1`, updated after maintainer approval.
- [x] Minecraft dependency is `~26.3`.
- [x] Fabric Loader dependency is `>=0.20.1`.
- [x] Client, Mod Menu and required Mixin declarations are present.
- [ ] Create the GitHub Release manually with the **Pre-release** flag; no publishing workflow is enabled.
- [x] The fork workflows contain no Modrinth/CurseForge publication path or token use.
- [x] Release title, changelog version and candidate source commit are recorded.

## D. Public documentation

- [x] README identifies Minecraft 26.3 and community pre-release status.
- [x] Required and optional dependencies are documented.
- [x] Version 1 snapshot ambiguity and CommandKeys exclusion are documented.
- [x] CHANGELOG draft covers the port and BUG-01/BUG-02.
- [x] Player-facing release notes are drafted.
- [x] Accepted pre-release risks are documented without changing test statuses.
- [x] LICENSE is preserved and NOTICE is drafted.

## E. Final build after approved metadata changes

- [x] Use Java 25 (GraalVM JDK 25.0.4).
- [x] Run `.\gradlew.bat clean test build --no-daemon --stacktrace`.
- [x] Confirm all 39 JUnit tests execute and pass.
- [x] Reuse approved production P1/P3/P5/P6 checks; Phase 5D changed no production Java/resource/API logic.
- [x] Inspect the production JAR for test classes, test providers, nested third-party JARs and unwanted dependencies.
- [x] Confirm the JAR contains exact approved LICENSE/NOTICE content.
- [x] Record filename, 120,908-byte size and SHA-256 `CCFB834361257FF580D13A3510ED20EAEEA08B2070BA50D84534280B0BC46A67`.
- [x] Confirm `fabric.mod.json` has version `1.4.1-community-beta.1`, Minecraft `~26.3` and Loader `>=0.20.1`.
- [x] Confirm the candidate artifact came from `fadd4a82b01242cd83c5ac28592c4c65921b1e99`; later commits before this checklist update were documentation only.

## F. Pre-release evidence and accepted risk

- [x] 39/39 JUnit previously passed.
- [x] P1–P6 production JAR startup/integration matrix passed.
- [x] BUG-01 is FIXED / VERIFIED with 10/10 maintainer manual tests.
- [x] BUG-02 is FIXED / VERIFIED with 8/8 maintainer manual tests.
- [x] Maintainer accepted the documented Phase 5B residual risks for a pre-release.
- [x] Verify no new blocker has been discovered since the last accepted evidence.
- [x] Keep R-03/R-11/R-12/R-13 and other incomplete items labelled `PARTIAL`/`BLOCKED`/`NOT TESTED` in public/internal records.

## G. Actions requiring separate authorization

- [ ] Push release-preparation commits.
- [ ] Create tag.
- [ ] Create GitHub Release and mark it Pre-release.
- [ ] Publish to Modrinth or CurseForge.
- [ ] Promote any build to Stable.

No completed review or checklist item authorizes an action in section G; each still requires explicit maintainer approval.
