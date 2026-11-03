# Visual Keymap 26.3 Phase 5A 发布就绪性报告

## 1. 当前基线

| 项目 | 实际状态 |
| --- | --- |
| 分支 | `port/mc-26.3` |
| HEAD | `5e69d2383e91b6f2a4a08fe1d6cd065bda1fbad9`（Phase 5A 开始时） |
| BUG-01 | FIXED / VERIFIED，维护者人工 10/10 PASS |
| BUG-02 | FIXED / VERIFIED，维护者人工 8/8 PASS |
| JUnit | 39/39 PASS |
| 成品 JAR 矩阵 | P1/P3/P5/P6 在 BUG-01 合并后 PASS；Phase 4 的 P1–P6 全组合 PASS |
| 发布 | 未 push、未 tag、未创建 Release |
| 工作树 | tracked 文件干净；存在既有未跟踪 `docs/bugfixes/EVIDENCE-INVENTORY.md`，本阶段未触碰 |

工作区扫描没有发现 BUG-01/02 之后新增的玩家反馈或完整 Phase 4 人工回归记录。不能把缺失记录视为已通过。

## 2. 已完成的发布要求

### 2.1 构建与成品

- Java 25、Gradle 9.8.2、Loom 1.19.3、Minecraft 26.3 环境可复现构建。
- 合并后 `clean test build --no-daemon` 成功，39 项 JUnit 全通过。
- 发布 JAR：`visual-keymap-26.3-1.4.0.jar`，107,834 B，SHA-256 `9C70445275A5AE16AF41788244B5D9C4506746802679BD8576FB788D671BB928`。
- JAR 不含测试探针、测试 Provider、nested JAR 或第三方实现包。
- `fabric.mod.json` 声明 client-only、Loader `>=0.20.1`、Fabric API、Minecraft `~26.3`、client/Mod Menu entrypoint 和 required Mixin。

### 2.2 核心兼容性

- 规范键码、127 个扩展键、鼠标 0–7、碰撞和双向转换已有自动化覆盖。
- Minecraft F8 与 MaLiLib J/F9 的真实 26.2→26.3 version 1 往返通过。
- public API descriptor、`visualkeymap` entrypoint、provider 顺序和 version 1 schema 保持兼容。
- MaLiLib 0.30.2、Tweakeroo 0.30.1、CommandKeys 3.104.1 Fabric、Mod Menu 21.0.0 的构件和基础加载已验证。
- BUG-01 非法 JSON 与 BUG-02 重复 ID 已有自动化、成品 JAR 探针和维护者真实 GUI 验收。

### 2.3 可选依赖和运行边界

- MaLiLib、CommandKeys、Mod Menu 分别缺席时启动通过。
- P1–P6 基础启动、Mixin、provider 枚举、外部 entrypoint 异常隔离通过。
- 测试 source set 与生产 JAR 隔离。

## 3. 仍缺少的重要证据

按风险排序：

1. **高：CommandKeys GUI 与对象身份。** profile 切换、名称/comment、消息搜索、limit+主键 GUI 状态机、实际宏触发，以及多个未绑定宏是否互相影响，尚无完整人工证据。
2. **高：MaLiLib GUI 状态机和实际触发。** adapter、四键数据和重启已通过，但按住/释放、提前释放、GUI Reset/搜索及世界内热键触发未完成。
3. **高：P5 联合编辑与跨 Provider 冲突。** BUG-02 M-08 已证明基本共存、联合保存和重启；跨 Provider 冲突/排序以及三类绑定在同一会话连续编辑仍需收口。
4. **高：导出失败路径。** 正常导出和取消通过；覆盖与不可写目标尚未验证，存在文件完整性风险。
5. **中：原生物理触发与主屏 Esc。** 编辑/保存已通过，但新键在世界中的实际动作和主屏 Esc 返回未单独记录。
6. **中：外部 Provider GUI。** 编译、加载、模型、callback 和快照均通过，实际 GUI/tooltip/编辑/Reset 闭环未完成。
7. **中低：完整主屏语言/Scale、Mod Menu Done/Esc。** 入口和生命周期通过，但视觉与物理返回证据不完整。

最小步骤见 `REMAINING-MANUAL-TESTS.md`。已经充分自动化的编码、算法和 BUG-01/02 不应重复人工穷举。

## 4. 发布技术审计

| 项目 | 结果 | 结论 |
| --- | --- | --- |
| Git 历史 | Phase 3、Phase 4、BUG-01/02 均为可追溯独立提交/merge | PASS |
| 生产 JAR 与测试版本 | 最终 JAR 哈希已记录；与 BUG-01 人工候选仅语言资源换行不同，语义一致 | PASS，发布时重新记录最终哈希 |
| Metadata | 26.3、Loader、Fabric、entrypoint、Mixin 正确 | PASS |
| API / version 1 | 签名与 schema 保持；未知 version、modifiers 历史语义锁定 | PASS |
| 测试隔离 | JAR 68 entries，无测试类、探针、nested JAR | PASS |
| 第三方打包 | MaLiLib/CommandKeys/Mod Menu 实现未嵌入 | PASS |
| LICENSE/署名 | 根目录 GPL v3 文本和 XeF2 作者保留；JAR 不含 LICENSE；Modrinth 显示 `GPL-3.0-or-later` 而 metadata 为 `GPL-3.0` | DECISION REQUIRED |
| README | 英中 README 仅称“1.21.9 及以上”，未明确 26.3、依赖、迁移/bugfix/限制 | NOT READY FOR PUBLICATION |
| 旧链接 | README 无下载链接；metadata 指向官方上游/Modrinth，链接有效但若由 Fork 独立发布可能产生归属混淆 | DECISION REQUIRED |
| 版本号 | 内部仍为 `1.4.0`，与 26.1/26.2 系列共用；tag 可按 `v1.4.0-26.3` 延续上游习惯 | DECISION REQUIRED |
| 发布说明 | 无专门 26.3 changelog/迁移说明；workflow 自动 changelog 只筛 `feat,fix` | NOT READY FOR PUBLICATION |
| Release workflow | 任意 `v*` tag 自动尝试 GitHub + 官方 Modrinth/CurseForge 发布 | 必须先决定渠道和 secrets/权限 |

官方上游仍在 [GitHub](https://github.com/USTC-XeF2/visual-keymap)，官方 [Modrinth 项目](https://modrinth.com/mod/visual-keymap) 当前公开兼容性最高为 26.2；既有 26.2 发布名为 [`1.4.0-26.2`](https://modrinth.com/mod/visual-keymap/version/1.4.0-26.2)。

## 5. 发布就绪性分类

### READY FOR PRE-RELEASE（技术条件，带发布前条件）

理由：

- 核心构建、加载、快照安全、键码/API 兼容和两项高影响 bug 均有充分证据。
- 现存缺口主要是第三方 GUI/实际触发和边缘文件失败路径，适合通过 RC/预发布扩大真实环境覆盖。
- 没有未解决 FAIL 或已知静默数据损坏缺陷。

但“现在立即公开发布”仍需先完成：

1. `REMAINING-MANUAL-TESTS.md` 的高风险 R-02、R-03、R-10～R-13，或由维护者逐项明确接受并披露风险。
2. `RELEASE-DECISIONS.md` 的版本、渠道、GPL/署名与发布说明决策。
3. 更新 README 或至少准备完整 RC release notes；重新构建并记录最终 commit/JAR SHA-256。

### NOT YET READY FOR STABLE

稳定版仍缺 MaLiLib/CommandKeys 实际触发和 GUI 状态机、多个未绑定 CommandKeys 宏、跨 Provider 联合冲突/保存、外部 Provider GUI、导出失败路径，以及代表性布局/返回链证据。这些不是算法测试数量可以替代的项目。

### 非阻断已知限制

- 缺少 F25、M6～M8、26.3-only 特殊硬件：物理部分可 BLOCKED；数据编码/不可应用保护已 PASS。
- version 1 不导入 modifiers、CommandKeys 不导出、重复 ID 旧快照需人工消歧、未知 version 维持历史行为：应披露，不在 Phase 5A 扩大修复。
- Gradle 10 deprecated-feature 警告是后续维护风险；当前 Gradle 9.8.2 构建成功。

## 6. 维护者下一步

1. 按最小清单执行高风险补测，优先 R-12（多个未绑定宏）与 R-03（导出失败路径）。
2. 决定发布渠道和版本命名；在决定前不要创建 `v*` tag。
3. 决定许可证表达、修改署名和源码交付方式。
4. 准备 26.3 README/更新日志/已知限制。
5. 完成后再做 Phase 5B 最终构建、哈希、release artifact 与 tag 前审查。

## 7. 最终结论

当前已具备继续发布准备和 RC 验证的技术基础，分类为 **READY FOR PRE-RELEASE（有前置条件）**；尚不满足 **READY FOR STABLE**。BUG-01、BUG-02 已关闭，不再列为发布阻碍。
