# Visual Keymap 26.3 Community Beta — Final Release Approval Review

## 1. Final decision

**READY FOR RELEASE APPROVAL**

No new production, data-integrity, packaging or automated-publication blocker was found. Publishing remains a separate maintainer action and is not performed by this review.

Approved release identity:

| Item | Final value |
| --- | --- |
| Internal version | `1.4.1-community-beta.1` |
| Minecraft | `26.3` |
| Tag | `v1.4.1-26.3-community-beta.1` |
| Release title | `Visual Keymap 1.4.1 for Minecraft 26.3 — Community Beta 1` |
| Release type | GitHub **Pre-release**, community fork only |
| Production JAR | `visual-keymap-26.3-1.4.1-community-beta.1.jar` |
| Size | 120,908 bytes |
| SHA-256 | `CCFB834361257FF580D13A3510ED20EAEEA08B2070BA50D84534280B0BC46A67` |
| Candidate source commit | `fadd4a82b01242cd83c5ac28592c4c65921b1e99` |

## 2. Git and candidate relationship

Review-time branch: `port/mc-26.3`.

The candidate was built and passed Preflight at `fadd4a82b01242cd83c5ac28592c4c65921b1e99`. Subsequent changes before this approval document are documentation only:

- `fead92ad2a90a49a783e1a3936271ac27f2e438b` adds the Phase 5D-C candidate report.
- This final-review change updates only release documentation/checklist material.

A Git comparison confirms no change under `src/main/java`, `src/main/resources`, or the public API package after the candidate source commit. The candidate binary therefore still corresponds to the same production source and build configuration. For maximum traceability, the recommended hosted path is to run manual Preflight on the exact final commit after it reaches the default branch; require the hosted artifact to have the approved hash before tagging that commit. If hosted Preflight is intentionally skipped, tag the already verified candidate commit `fadd4a82…` rather than implying a different commit was tested.

The only pre-existing untracked file is `docs/bugfixes/EVIDENCE-INVENTORY.md`. It remains outside Git, the JAR and release assets.

## 3. GitHub Actions trigger and permission review

Executable workflows:

- `.github/workflows/ci.yml`
- `.github/workflows/release-preflight.yml`

No release/publish workflow exists.

| Question | Answer | Evidence and effect |
| --- | --- | --- |
| Does an ordinary branch push trigger a Release? | **No.** | It triggers `Continuous Integration` only. CI runs `clean test build`; it has no release step or publication credential. |
| Does a Git tag push automatically create a Release? | **No.** | CI's push trigger is branch-filtered and Preflight listens only to `workflow_dispatch`. No workflow listens to tags, `release`, `create`, `workflow_run` or `repository_dispatch`. |
| Is there a manual build entry? | **Yes.** | `Release Preflight` is manual-only (`workflow_dispatch`) and builds/tests/inspects a candidate Actions artifact. It never creates a tag or Release. |
| Can a workflow overwrite an existing Release? | **No.** | No workflow calls a Release API/action or `gh release`; no workflow has repository write permission. |
| Can GitHub-hosted validation occur without public release? | **Yes.** | A branch push runs CI. Once the manual workflow file is present on the default branch, Preflight can be dispatched for a selected ref and stores only an Actions artifact. |
| Do Actions have unnecessary write permission? | **No.** | Both workflows explicitly use `permissions: contents: read`; checkout does not persist credentials; no `secrets.*` reference exists. |

Important operational detail: the fork's recorded default branch is `origin/main`. GitHub requires a `workflow_dispatch` workflow file to exist on the default branch before the **Run workflow** control/API is available. Pushing only `port/mc-26.3` is enough to run its push CI, but manual Release Preflight may not be dispatchable until the reviewed workflow reaches `main`. This is not an auto-release risk; it determines the order of hosted verification.

Official behavior references:

- [GitHub manual workflow documentation](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow) documents `workflow_dispatch`, the default-branch requirement and selecting a branch/ref.
- [GitHub release documentation](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository) documents draft releases, asset upload and the Pre-release flag.
- [GitHub CLI `gh release create`](https://cli.github.com/manual/gh_release_create) documents `--draft`, `--prerelease`, `--latest=false` and `--verify-tag`. `--verify-tag` is required in the plan below so the CLI cannot silently create a tag at the wrong commit.

## 4. Candidate artifact re-verification

The local candidate still exists and was re-read during this review.

| Check | Result |
| --- | --- |
| Filename | PASS — `visual-keymap-26.3-1.4.1-community-beta.1.jar` |
| Size | PASS — 120,908 bytes |
| SHA-256 | PASS — `CCFB834361257FF580D13A3510ED20EAEEA08B2070BA50D84534280B0BC46A67` |
| Mod ID | PASS — `visual-keymap` |
| JAR version | PASS — `1.4.1-community-beta.1` |
| Minecraft dependency | PASS — `~26.3` |
| Loader dependency | PASS — `>=0.20.1` |
| `META-INF/LICENSE` | PASS — exact SHA-256 match with root `LICENSE` |
| `META-INF/NOTICE` | PASS — exact SHA-256 match with root `NOTICE` |
| Nested JARs | PASS — 0 |
| Test/testmod/provider-probe entries | PASS — 0 |
| JUnit | PASS — 39 tests, 0 failures/errors/skips |
| Release Preflight | **READY**, no unmet conditions |

The JAR was not rebuilt during this final document review because no production source, resources, Gradle configuration, version or packaged license file changed after the successful candidate build.

## 5. Release material review

### Player-facing materials

- `docs/release/RELEASE-NOTES.md` now identifies the exact version, tag, Community Beta title, JAR, size and SHA-256.
- It describes the Minecraft 26.3 port, installation requirements, optional MaLiLib/CommandKeys/Mod Menu integrations, BUG-01 and BUG-02 fixes, snapshot compatibility and accepted limits in player-facing language.
- It clearly states that this is a community-maintained Pre-release and not an official/stable upstream release.
- The known-issues link targets the approved tag path in the community fork.
- `CHANGELOG.md` names `1.4.1-community-beta.1` rather than `Unreleased`.
- `README.md` accurately states Minecraft 26.3, Java 25, required Fabric components, optional integration versions, snapshot behavior and community-fork status.

### License and source

- The upstream GPLv3 text is unchanged.
- XeF2, upstream contributors and the original repository remain credited.
- `NOTICE` identifies the community modifications and disclaims official upstream status.
- LICENSE and NOTICE are embedded in the JAR.
- GitHub automatically provides source archives for the commit identified by a release tag; the public tag must therefore point to the reviewed source commit, and the release page should retain the community repository/source link.

### Known limitations

- The accepted Phase 5B `PARTIAL`, `BLOCKED` and `NOT TESTED` scopes remain disclosed; none was changed to PASS without evidence.
- CommandKeys snapshot exclusion and version 1 duplicate-ID ambiguity are explicitly documented.
- No new confirmed crash, irreversible configuration loss or general data-corruption defect is known.
- The Release must be marked **Pre-release** and must not be marked Stable.

## 6. Safe release sequence

The commands below are a plan only. They were not executed by this review.

### A. Push and hosted CI — requires maintainer authorization

```powershell
Set-Location 'E:\02_Development\01_Workspace\VisualKeyMap26.3\VisualKeyMap26.3-repo'
git checkout port/mc-26.3
git status --short
git log --oneline --decorate -8
git push origin port/mc-26.3
```

Expected effect: branch Push CI only. No tag or Release is created.

Open a Pull Request from `port/mc-26.3` to the fork's `main`, review the complete migration/release diff, and require CI success. Merging the PR is a separately authorized repository change.

### B. Hosted Release Preflight — after the workflow exists on `main`

After the approved merge, record the exact target commit:

```powershell
git fetch origin --prune
git checkout main
git pull --ff-only origin main
$releaseCommit = (git rev-parse HEAD).Trim()
Write-Host "Release commit: $releaseCommit"
```

Run the manual workflow using the GitHub Actions UI, selecting `main`, or with GitHub CLI:

```powershell
gh workflow run release-preflight.yml --ref main --repo panda-piglet/visual-keymap
```

Require a successful run whose head SHA equals `$releaseCommit`. Download its qualified artifact and compare the production JAR hash with the approved value:

```powershell
$runId = gh run list `
    --repo panda-piglet/visual-keymap `
    --workflow release-preflight.yml `
    --branch main `
    --limit 1 `
    --json databaseId,headSha,conclusion `
    --jq '.[0].databaseId'

gh run watch $runId --repo panda-piglet/visual-keymap

$artifactName = "visual-keymap-26.3-preflight-$releaseCommit"
gh run download $runId `
    --repo panda-piglet/visual-keymap `
    --name $artifactName `
    --dir '.\release-candidate-hosted'

Get-FileHash `
    '.\release-candidate-hosted\visual-keymap-26.3-1.4.1-community-beta.1.jar' `
    -Algorithm SHA256
```

Stop if the run SHA is wrong, the workflow is not READY, or the JAR hash differs from `CCFB834361257FF580D13A3510ED20EAEEA08B2070BA50D84534280B0BC46A67`. Use the hosted verified JAR for release if this path is completed.

### C. Create and push the approved tag — requires explicit authorization

Only after selecting the exact verified commit:

```powershell
$tag = 'v1.4.1-26.3-community-beta.1'
$title = 'Visual Keymap 1.4.1 for Minecraft 26.3 — Community Beta 1'

git tag --list $tag
git show --no-patch --format=fuller $releaseCommit
git tag -a $tag $releaseCommit -m $title
git show --no-patch --decorate $tag
git push origin "refs/tags/$tag"
```

The empty `git tag --list` result is required before creation. Tag push triggers no current workflow and creates no Release.

### D. Create a reviewed GitHub draft Pre-release — requires explicit authorization

First verify that no Release already exists for the tag. Then create a draft with the verified JAR and checksum file:

```powershell
gh release view $tag --repo panda-piglet/visual-keymap
# The command above should report that no Release exists. If one exists, stop.

gh release create $tag `
    '.\release-candidate-hosted\visual-keymap-26.3-1.4.1-community-beta.1.jar' `
    '.\release-candidate-hosted\SHA256SUMS.txt' `
    --repo panda-piglet/visual-keymap `
    --verify-tag `
    --draft `
    --prerelease `
    --latest=false `
    --title $title `
    --notes-file '.\docs\release\RELEASE-NOTES.md'
```

Do not use `--clobber`. `--verify-tag` prevents GitHub CLI from creating an unreviewed tag automatically. Creating a draft allows the title, body, assets, target tag, Pre-release state and checksums to be reviewed before publication.

### E. Final human review and publication — requires explicit authorization

Review in the GitHub web UI:

1. Tag and target commit are exact.
2. Title is `Visual Keymap 1.4.1 for Minecraft 26.3 — Community Beta 1`.
3. **This is a pre-release** is selected; Latest/Stable is not selected.
4. JAR filename, size and SHA-256 match this report.
5. Release Notes and Known Issues are readable.
6. Source archive exposes the tag's exact corresponding source.
7. No Modrinth or CurseForge upload occurred.

After a final explicit publish authorization:

```powershell
gh release edit $tag `
    --repo panda-piglet/visual-keymap `
    --draft=false `
    --prerelease `
    --latest=false

gh release view $tag `
    --repo panda-piglet/visual-keymap `
    --json tagName,name,isDraft,isPrerelease,isLatest,targetCommitish,assets,url
```

Download the published asset to a fresh directory and verify the SHA-256 again. Do not upload to Modrinth or CurseForge in this release phase.

## 7. True remaining blockers

No technical release blocker is known.

The only remaining gates are intentional maintainer approvals and remote-state operations:

1. Approve/push the reviewed branch.
2. Decide whether to require the optional-but-recommended GitHub-hosted Preflight before tagging.
3. Approve the exact release commit/tag.
4. Approve draft creation and then public Pre-release publication.

If hosted Preflight produces a different hash or any new CI failure, that becomes a blocker and publication must stop. Otherwise, no further test expansion is recommended for this Community Beta.

## 8. Approval recommendation

**READY FOR RELEASE APPROVAL.**

The project is ready for the maintainer to authorize the safe push/hosted-validation/tag/draft sequence above. This report itself does not authorize or perform those actions.
