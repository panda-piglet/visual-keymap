# Visual Keymap 26.3 发布决策清单

本文件列出仍需维护者决定的发布策略。Phase 5C 已获准准备公开文档，但仍不修改版本号、发布 metadata、workflow 或渠道，直到维护者逐项批准。

## D-01 版本号与标签

现状：

- `gradle.properties`：`mod_version=1.4.0`。
- artifact：`visual-keymap-26.3-1.4.0.jar`；内部 Fabric version 为 `1.4.0`。
- 已有本地标签：`v1.4.0-26.1`、`v1.4.0-26.2`；上游 Modrinth 已发布 [`1.4.0-26.2`](https://modrinth.com/mod/visual-keymap/version/1.4.0-26.2)。

选项：

1. **沿用上游多 Minecraft 版本命名（推荐用于上游正式发布）**：保持内部 `1.4.0`，标签/发布名使用 `v1.4.0-26.3` / `1.4.0-26.3`。优点是与 26.1/26.2 一致；缺点是不同 Minecraft 构件的内部 mod version 相同。
2. **因两项 bugfix 增量版本**：改为 `1.4.1`，标签 `v1.4.1-26.3`。优点是客户端和日志更易区分；缺点是偏离上游当前跨版本习惯，需要原作者认可。
3. **社区移植预发布标识（当前推荐）**：内部 `1.4.0-community-beta.1`，tag `v1.4.0-26.3-community-beta.1`，发布标题明确 `Community Pre-release 1`。它能同时显示上游版本血统、目标 Minecraft 版本、社区来源和预发布状态；实施后仍需验证 Fabric 版本解析与产物命名。

Decision Required：选择内部版本、标签和平台显示名。不得只创建 `v*` 测试标签，因为现有 workflow 会触发真实发布任务。

## D-02 发布渠道与权限

现状：

- `origin` 是维护者 Fork；`upstream` 是 [`USTC-XeF2/visual-keymap`](https://github.com/USTC-XeF2/visual-keymap)。
- `fabric.mod.json` 的 homepage/source/issues 和作者仍指向上游与 XeF2。
- `.github/workflows/release.yml` 对任何 `v*` tag 自动尝试发布 GitHub、官方 Modrinth 项目 `uJWhARde` 和 CurseForge 项目 `1401807`，依赖对应 secrets。
- 官方 [Modrinth 项目](https://modrinth.com/mod/visual-keymap) 当前兼容列表最高为 26.2。

选项：

1. **上游 PR，由原作者在官方渠道发布（推荐）**：最符合现有 metadata、作者和项目 ID。
2. **Fork GitHub Pre-release**：明确标注“非官方 26.3 移植/测试构建”，保留上游署名和源码链接；不要误用官方 Modrinth/CurseForge 项目 token。
3. **独立平台项目**：需要维护者确认命名、品牌、权限、联系链接和平台规则后再调整 metadata；本阶段不假定已有授权。

Decision Required：谁发布、发布到哪里、由哪个仓库/tag 触发。渠道未定前不要创建 tag。

## D-03 GPL、署名与修改说明

已确认：

- 仓库保留完整 `LICENSE`（GNU GPL v3 文本）。
- Phase 5C 已新增 `NOTICE`，标明 XeF2 原作、上游地址和 2026 年社区修改；尚未打入 JAR。
- `fabric.mod.json` 保留作者 `XeF2`，license 为 `GPL-3.0`，source/issues 指向上游。
- 成品 JAR 当前不包含 `LICENSE` entry；发布 workflow 只选择生产 JAR，不上传 sources JAR，但 GitHub tag 会提供源码归档。
- Modrinth 页面显示 `GPL-3.0-or-later`，与仓库 metadata 的 `GPL-3.0` 文本不完全一致，应由维护者确认预期 SPDX 表达。

发布前建议决定：

1. 是否保持 `GPL-3.0`，还是经原作者确认改为 `GPL-3.0-only` / `GPL-3.0-or-later` 的明确表达。
2. 独立发布时如何显著说明“基于 XeF2 原作、由维护者完成 26.3 移植及 BUG-01/02 修复”，并给出修改日期与源代码地址。
3. 是否把 LICENSE 打入 JAR或至少随 Release 附带，并确保对应源码可从同一发布页面获得。

这些是发布合规/归属决策，Phase 5A 不提供法律意见，也不擅自改作者字段。

## D-04 README、更新日志与已知限制

现状：Phase 5C 已更新英文/中文 README，明确 Minecraft 26.3、Java 25、依赖版本、快照兼容和社区 Pre-release 状态；并已起草 `CHANGELOG.md`、`RELEASE-NOTES.md` 与 `KNOWN-ISSUES.md`。这些文字仍需维护者最终批准。官方 Modrinth 26.2 版本页面没有 changelog。

已准备的公开信息包括：

- 明确支持 Minecraft 26.3 / Fabric / Java 25。
- 列出必要 Fabric Loader/API 和可选 MaLiLib、CommandKeys、Mod Menu 兼容版本。
- 迁移说明：SDL 键码兼容、version 1 保持、BUG-01/BUG-02 修复。
- 已知限制：version 1 modifiers 不应用；CommandKeys 不导出；重复 ID 旧快照需人工消歧；未知 version 保持历史行为；特殊物理键覆盖有限。
- 指向正确源码、问题渠道和许可证。

Decision Required：批准最终措辞，并决定 Release 页面是否直接采用 `RELEASE-NOTES.md`。README 与详细 release docs 已分层，不再需要把开发调查细节加入 README。

## D-05 发布级别

选项：

1. **Pre-release / RC**：适合当前技术状态，但应先完成 `REMAINING-MANUAL-TESTS.md` 的高风险 R-02、R-03、R-10～R-13，或明确接受并披露缺口。
2. **Stable**：要求剩余核心 GUI/触发/持久化测试全部通过，特别是 MaLiLib、CommandKeys、联合保存、外部 Provider 和导出失败路径。

维护者决策：接受已披露的 PARTIAL 风险并进入社区 Pre-release 准备；不把这些测试改为 PASS。根据预发布反馈和剩余人工项决定稳定版。

## D-06 发布工作流门槛

现有 tag workflow 在 fresh checkout 上执行 `./gradlew build`，会运行 JUnit，但不会执行 production-smoke P1–P6，也没有人工批准门。发布前需决定是否：

- 保持 tag 前人工运行矩阵并记录 SHA-256；或
- 后续增加独立 CI/release gate（属于 Phase 5B，需另行授权）。

本阶段建议至少把最终批准的 commit、JAR SHA-256、39/39 JUnit 和 P1/P3/P5/P6 结果写入 release notes，再由维护者手工创建 tag。

## 决策摘要

| 决策 | 发布预发布版前 | 稳定版前 |
| --- | --- | --- |
| 版本/标签 | 必须 | 必须 |
| 渠道与权限 | 必须 | 必须 |
| GPL 表达、署名、源码可得性 | 必须 | 必须 |
| README/发布说明 | 至少 release notes | README + release notes |
| 高风险人工补测 | 必须或明确风险接受 | 必须 PASS |
| production-smoke gate | 手工记录可接受 | 建议固化 |
