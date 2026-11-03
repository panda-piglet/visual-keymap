# Visual Keymap 26.3 Pre-release Checklist

No item in this checklist authorizes publishing by itself.

## A. Maintainer decisions

- [ ] Approve internal mod version.
- [ ] Approve Git tag and player-facing release title.
- [ ] Select release channel: fork GitHub pre-release, upstream official release, or separate community platform projects.
- [ ] Approve the `beta`/`rc` label.
- [ ] Approve final release notes and known issues.
- [ ] Decide whether `LICENSE` and `NOTICE` are embedded in the JAR (recommended) or distributed beside it.
- [ ] Confirm the GPL expression; do not assume `GPL-3.0-or-later` without evidence from the original licensing grant.

## B. Source and Git

- [x] Work is on `port/mc-26.3`.
- [x] BUG-01 and BUG-02 merge history is present.
- [x] Original author and upstream repository are preserved in metadata and public documentation.
- [x] `baseline-private/` is not tracked.
- [x] Existing untracked `docs/bugfixes/EVIDENCE-INVENTORY.md` remains outside release commits.
- [ ] Working tree contains only approved release changes before the final build.
- [ ] Final release commit is reviewed and recorded.
- [ ] Exact corresponding source is available from the approved public tag/commit.

## C. Version, metadata and workflow

- [ ] Update `mod_version` only after maintainer approval.
- [x] Minecraft dependency is `~26.3`.
- [x] Fabric Loader dependency is `>=0.20.1`.
- [x] Client, Mod Menu and required Mixin declarations are present.
- [ ] Explicitly set publishing `version-type: beta` and `github-prerelease: true`, or use another reviewed pre-release mechanism.
- [ ] Prevent the fork workflow from publishing to the original Modrinth/CurseForge project IDs unless explicitly authorized.
- [ ] Confirm release title, changelog source and target commit.

## D. Public documentation

- [x] README identifies Minecraft 26.3 and community pre-release status.
- [x] Required and optional dependencies are documented.
- [x] Version 1 snapshot ambiguity and CommandKeys exclusion are documented.
- [x] CHANGELOG draft covers the port and BUG-01/BUG-02.
- [x] Player-facing release notes are drafted.
- [x] Accepted pre-release risks are documented without changing test statuses.
- [x] LICENSE is preserved and NOTICE is drafted.

## E. Final build after approved metadata changes

- [ ] Use Java 25.
- [ ] Run `.\gradlew.bat clean test build --no-daemon`.
- [ ] Confirm all JUnit tests execute and pass.
- [ ] Run or reuse approved production P1/P3/P5/P6 checks as required by the changed scope.
- [ ] Inspect the production JAR for test classes, test providers, nested third-party JARs and unwanted dependencies.
- [ ] Confirm the JAR contains the approved LICENSE/NOTICE packaging.
- [ ] Record final filename, size and SHA-256.
- [ ] Confirm `fabric.mod.json` inside the JAR has the approved version and dependency constraints.
- [ ] Confirm the final artifact came from the final release commit.

## F. Pre-release evidence and accepted risk

- [x] 39/39 JUnit previously passed.
- [x] P1–P6 production JAR startup/integration matrix passed.
- [x] BUG-01 is FIXED / VERIFIED with 10/10 maintainer manual tests.
- [x] BUG-02 is FIXED / VERIFIED with 8/8 maintainer manual tests.
- [x] Maintainer accepted the documented Phase 5B residual risks for a pre-release.
- [ ] Verify no new blocker has been discovered since the last accepted evidence.
- [ ] Keep R-03/R-11/R-12/R-13 and other incomplete items labelled `PARTIAL`/`BLOCKED`/`NOT TESTED` in public/internal records.

## G. Actions requiring separate authorization

- [ ] Push release-preparation commits.
- [ ] Create tag.
- [ ] Create GitHub Release and mark it Pre-release.
- [ ] Publish to Modrinth or CurseForge.
- [ ] Promote any build to Stable.

Phase 5C does not authorize any item in section G.
