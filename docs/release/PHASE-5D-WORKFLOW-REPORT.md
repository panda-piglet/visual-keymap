# Phase 5D-A GitHub Actions Workflow Report

## 1. Scope and baseline

This report covers only the GitHub Actions refactoring authorized for Phase 5D-A. No production Java source, version number, `LICENSE`, `NOTICE`, release notes, tag, remote branch, GitHub Release, Modrinth project or CurseForge project was changed.

- Branch: `port/mc-26.3`
- Starting commit: `c45b9fea49059bfc2f6ac03707903235e5c68a3b`
- Planned pre-release version checked by Preflight: `1.4.1-community-beta.1`
- Planned Minecraft version: `26.3`
- Planned tag is documentation only: `v1.4.1-26.3-community-beta.1`
- Existing private/untracked evidence file preserved: `docs/bugfixes/EVIDENCE-INVENTORY.md`

## 2. Original workflow audit

The repository originally had one executable workflow: `.github/workflows/release.yml`.

| Finding | Evidence | Risk |
| --- | --- | --- |
| Every `v*` tag triggered the workflow | `on.push.tags: ['v*']` | Any matching tag could initiate publication without a separate manual approval gate. |
| Workflow had repository write permission | `permissions.contents: write` | More authority than build/test requires and sufficient for GitHub release operations. |
| GitHub publication was enabled | `Kira-NT/mc-publish@v3.3.0` received `github-token` | A matching tag could create a public GitHub release. |
| Original Modrinth and CurseForge projects were targeted | Modrinth ID `uJWhARde`; CurseForge ID `1401807` | A fork build could be uploaded to upstream channels if usable tokens were present. |
| Release secrets were consumed | `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`, `GITHUB_TOKEN` | CI and publication were not separated by responsibility or credentials. |
| Changelog generation participated in tag publication | `requarks/changelog-action@v1` | Publication behavior depended on tag history and the current repository context. |

No additional workflow, reusable workflow, Gradle task or repository script was found that created a tag/release or uploaded to GitHub, Modrinth or CurseForge. No `release`, `workflow_run`, `repository_dispatch`, `create` or `pull_request_target` trigger was present. The dangerous path was the old `v*` tag workflow itself.

## 3. Files changed

| File | Change |
| --- | --- |
| `.github/workflows/release.yml` | Removed from the executable workflow path. This eliminates tag-triggered public publication by design rather than relying on missing secrets. |
| `.github/workflows/ci.yml` | Added read-only build/test CI for branch pushes and pull requests. |
| `.github/workflows/release-preflight.yml` | Added a manual-only, read-only candidate build and verification workflow. |
| `scripts/release-preflight.ps1` | Added deterministic JAR, metadata, hash, license/NOTICE and test-content verification plus report generation. |
| `docs/release/PHASE-5D-WORKFLOW-REPORT.md` | Added this audit and validation record. |

## 4. Continuous Integration workflow

Triggers:

- Branch pushes (`push.branches: ['**']`), including the repository default branch.
- Pull requests (`pull_request`).
- Tag pushes do not match the branch-only push filter.
- `pull_request_target` is deliberately not used.

Execution:

1. Check out source without persisting Git credentials.
2. Install Temurin JDK 25.
3. Configure Gradle and validate the Wrapper.
4. Run `./gradlew clean test build --no-daemon --stacktrace`.
5. Upload JUnit reports only when the job fails.

Security properties:

- Workflow permission is only `contents: read`.
- The Gradle cache is read-only, including for untrusted pull-request code.
- No publishing secret is referenced.
- No tag, release, upload-to-mod-platform or repository mutation step exists.
- Concurrency cancellation affects only redundant CI runs; it does not alter repository state.

## 5. Release Preflight workflow

Trigger:

- Only `workflow_dispatch`.
- It does not listen to branch pushes, tag pushes, GitHub Releases, `create`, `workflow_run` or repository dispatch events.

Execution:

1. Check out the maintainer-selected ref without persisting Git credentials.
2. Install Temurin JDK 25 and validate/configure the Gradle Wrapper.
3. Record repository, ref, complete commit SHA, Java and Gradle information.
4. Run `./gradlew clean test build --no-daemon --stacktrace`.
5. Inspect the production JAR and generate:
   - `SHA256SUMS.txt`
   - `BUILD-METADATA.txt`
   - `RELEASE-VERIFICATION.md`
6. Upload a qualified candidate artifact only if both the Gradle build and every required verification pass.
7. Otherwise upload diagnostics and fail the workflow as **Not Ready**.

The verification gate checks:

- Build/test outcome.
- Minecraft version `26.3`.
- Planned mod version `1.4.1-community-beta.1` in both Gradle properties and JAR metadata.
- Exactly one production JAR.
- Valid `fabric.mod.json` and Minecraft dependency `~26.3`.
- `META-INF/LICENSE` and `META-INF/NOTICE` in the JAR.
- No nested third-party JARs.
- No recognized test probes/providers in the production JAR.
- Java and Gradle environment commands complete successfully.

The workflow has `contents: read`, references no secret and performs no publication. An uploaded Actions artifact is explicitly a candidate for maintainer review; it is not a GitHub Release or public platform upload.

## 6. Removed and impossible automatic publication paths

Static workflow assertions now pass for all of the following:

- Exactly two executable workflows exist: CI and Release Preflight.
- The legacy release workflow is absent.
- No workflow has `contents: write`.
- No workflow references `secrets.*`.
- No workflow invokes `mc-publish` or another release action.
- No workflow listens for tag, release, create, workflow-run or repository-dispatch events.
- No workflow command creates a tag or GitHub release.

Expected event behavior:

| Event | Result |
| --- | --- |
| Ordinary branch push | CI runs; no publication path exists. |
| Pull request | Read-only CI runs; no publication path exists. |
| Manual Release Preflight | Build, verify and retain a candidate/diagnostic Actions artifact; no publication. |
| Ordinary tag push | No workflow is triggered by the tag. |
| Planned beta tag push | No workflow is triggered by the tag. |
| GitHub Release creation | No workflow is triggered by the release. |
| Missing platform tokens | CI and Preflight are unaffected because neither references them. |
| Build or required verification failure | Preflight reports Not Ready, does not upload a qualified candidate and ends in failure. |

## 7. Validation performed

### 7.1 Static workflow validation — PASS

- `actionlint 1.7.12` checked both YAML files and returned exit code 0.
- A separate static policy audit returned 13/13 PASS for workflow count, trigger isolation, read-only permissions, absence of secrets/publish actions and absence of tag/release creation commands.
- PowerShell parsed `scripts/release-preflight.ps1` without syntax errors.
- Current documented major action versions were used: [`actions/checkout@v7`](https://github.com/actions/checkout), [`actions/setup-java@v6`](https://github.com/actions/setup-java), [`gradle/actions/setup-gradle@v6`](https://github.com/gradle/actions), and [`actions/upload-artifact@v4`](https://github.com/actions/upload-artifact).

### 7.2 Preflight negative-path validation — PASS

The script was run against the existing local artifact with an explicit failed build result. It generated all three reports, returned exit code 1 and reported **NOT READY**. Confirmed results included:

- Minecraft `26.3`: PASS.
- Current mod version `1.4.0` versus planned `1.4.1-community-beta.1`: FAIL.
- Current JAR metadata version `1.4.0`: FAIL.
- Missing `META-INF/LICENSE`: FAIL.
- Missing `META-INF/NOTICE`: FAIL.
- No nested JAR: PASS.
- No test probe entry: PASS.
- Existing inspected artifact: `visual-keymap-26.3-1.4.0.jar`, 107,834 bytes, SHA-256 `9C70445275A5AE16AF41788244B5D9C4506746802679BD8576FB788D671BB928`.

This is the intended Phase 5D-A behavior: the workflow is implemented, but it refuses to describe the current artifact as release-ready before the separately authorized version and license-packaging work is complete.

### 7.3 Local clean build — BLOCKED by host environment

The requested clean build was attempted; it did not reach any Gradle task:

1. The restricted local execution environment could not write the configured Gradle Wrapper cache/lock location.
2. An unrestricted retry reached the installed Wrapper but Gradle could not start its single-use daemon because the host rejected its loopback connection (`java.io.IOException: Unable to establish loopback connection`, caused by `java.net.SocketException: Invalid argument: connect`).

This is not a source compilation or test failure. It prevents this turn from supplying a fresh local `clean test build` result. The previously built JAR was used only to validate the inspection and failure-reporting logic; it is not represented as a newly built candidate.

## 8. Verification not yet completed

- No GitHub-hosted CI run has occurred because this task does not authorize pushing the workflow changes.
- No GitHub-hosted manual Preflight run has occurred.
- The positive Preflight path cannot pass until the approved version is synchronized and `LICENSE`/`NOTICE` are packaged in the JAR; those changes are explicitly outside this task.
- Artifact upload, retention and the GitHub UI presentation remain to be confirmed in the first authorized hosted run.
- A fresh local build remains pending resolution of the current host's Gradle daemon loopback failure or execution on GitHub Actions.

These limitations do not restore an automatic publication path. They limit end-to-end evidence for the new hosted workflows.

## 9. Maintainer actions in GitHub

After review and only when separately authorized:

1. Push the workflow commit to the community fork.
2. Verify repository Actions are permitted to use the selected official actions.
3. Confirm the repository default workflow token remains read-only; the workflows also enforce `contents: read` locally.
4. Optionally require the **Continuous Integration / Build and test** check in branch protection.
5. Observe one branch push and one pull request run.
6. After version synchronization and license packaging are completed in a later authorized task, manually run **Release Preflight** against the exact candidate commit.
7. Download and inspect the candidate artifact and all generated reports before separately approving any tag or GitHub Pre-release.

Do not re-enable the removed `v*` publishing workflow. Tag creation and GitHub Release creation remain manual, separately authorized maintenance actions.

## 10. Acceptance result

**PARTIAL**

The workflow design and static validation satisfy the safety objective:

- CI and Release Preflight have separate responsibilities.
- Neither workflow has automatic publication capability.
- The old tag-triggered GitHub/Modrinth/CurseForge publication path is removed.
- Permissions and credential usage are minimized.
- Preflight correctly fails closed on the known version and license-packaging gaps.

The result is not marked PASS because a fresh local Gradle build was blocked before task execution and the workflows have not yet been exercised on a GitHub-hosted runner. No tag, release, remote push or external upload was performed.
