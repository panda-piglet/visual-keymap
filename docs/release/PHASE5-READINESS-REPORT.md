# Visual Keymap 26.3 Phase 5 发布就绪性报告（Phase 5B 更新）

## 1. 当前基线

| 项目 | 实际状态 |
| --- | --- |
| 分支 | `port/mc-26.3` |
| HEAD | `5e69d2383e91b6f2a4a08fe1d6cd065bda1fbad9`（Phase 5A 开始时） |
| Phase 5A 文档提交 | `2b0e7ccbc5982744d1817f69f77915ceaaaede1a` |
| BUG-01 | FIXED / VERIFIED，维护者人工 10/10 PASS |
| BUG-02 | FIXED / VERIFIED，维护者人工 8/8 PASS |
| JUnit | 39/39 PASS |
| 成品 JAR 矩阵 | P1/P3/P5/P6 在 BUG-01 合并后 PASS；Phase 4 的 P1–P6 全组合 PASS |
| 发布 | 未 push、未 tag、未创建 Release |
| 工作树 | tracked 文件干净；存在既有未跟踪 `docs/bugfixes/EVIDENCE-INVENTORY.md`，本阶段未触碰 |

Phase 5B 已收到维护者七项真实客户端人工反馈：MaLiLib 四键编辑/使用、代表性冲突、复杂配置多次重启、CommandKeys 快照排除、多个未绑定宏不丢失、外部 Provider 检测/加载，以及覆盖无效旧文件后的导出可读。反馈没有附带截图、日志、配置副本或逐步测试记录，因此只在描述明确的子项内计为人工证据。

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

## 3. Phase 5B 新增证据与剩余缺口

| Phase 5A 重点项 | Phase 5B 新证据 | 当前状态 | 尚缺关键验证 |
| --- | --- | --- | --- |
| R-02 原生搜索/排序/冲突/编辑 | 故意制造热键冲突未见异常；复杂配置多次重启保持 | PARTIAL | 未确认冲突范围；原生搜索、三排序、实际动作、Esc 清空/Reset |
| R-03 导出覆盖/不可写 | 覆盖原有无效文件后新快照可读 | PARTIAL | 不可写目标失败路径 |
| R-10 MaLiLib | 四键组合可编辑并实际使用；复杂配置重启保持 | PARTIAL | category/name/tooltip、内部名搜索、Reset、单/双键与提前释放 |
| R-11 CommandKeys | 不进入快照再次确认；多个未绑定宏未丢失 | PARTIAL | profile、显示、消息搜索、单键/组合编辑、清空/Reset、实际触发 |
| R-12 未绑定宏隔离 | 多个未绑定宏未丢失 | PARTIAL | 只编辑一个时另一个不变；26.2 对照或明确豁免 |
| R-13 多 Provider | 代表性冲突、复杂配置重启、快照排除均无异常 | PARTIAL | 明确跨 Provider 冲突；同一会话三 Provider 联合编辑 |

六项预发布重点测试均获得了新增人工证据，但没有一项按 Phase 5A 的完整定义达到整组 PASS。不能把“本轮没有发现问题”扩展为所有子项已验证。

按风险排序：

1. **高：CommandKeys GUI 与对象隔离。** 未绑定宏不丢失是正面证据，但仍缺 profile、消息搜索、limit+主键编辑/触发，以及只编辑一个未绑定宏时不会改到另一个对象。
2. **高：导出不可写失败路径。** 覆盖无效旧文件已通过；不可写目标是否保持已有文件完整且客户端可继续使用尚未验证。
3. **高：P5 联合编辑与明确的跨 Provider 冲突。** 代表性冲突和多次重启无异常，但反馈没有证明冲突来自不同 Provider，也未证明三种 Provider 在同一会话分别编辑。
4. **中高：MaLiLib 剩余 GUI 边界。** 四键编辑和实际使用已通过；仍缺元数据/内部名搜索、Reset、单/双键和提前释放。
5. **中：原生物理触发、搜索/排序与主屏 Esc。** 编辑/保存和复杂配置重启已有证据，但这些具体交互未单独记录。
6. **中：外部 Provider GUI。** 检测、加载、模型、callback 自动化和快照模型通过，实际 GUI/tooltip/编辑/Reset/save/快照闭环未完成。
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

### READY FOR PRE-RELEASE（技术基础）/ 尚未通过公开预发布门槛

理由：

- 核心构建、加载、快照安全、键码/API 兼容和两项高影响 bug 均有充分证据。
- Phase 5B 没有发现新故障，并补强了 MaLiLib 四键、冲突、重启、CommandKeys 快照/未绑定宏、外部 Provider 加载及导出覆盖证据。
- 没有未解决 FAIL 或已知静默数据损坏缺陷。

按 Phase 5A 已公布标准，六项重点测试目前仍全部为 PARTIAL，因此尚不能仅凭本轮“没有发现问题”宣布公开预发布门槛完成。发布前仍需：

1. 完成 `REMAINING-MANUAL-TESTS.md` 已缩减的 R-02、R-03、R-10～R-13 剩余步骤，或由维护者逐项明确接受并披露风险。优先 R-03 不可写目标和 R-12 单对象编辑隔离。
2. `RELEASE-DECISIONS.md` 的版本、渠道、GPL/署名与发布说明决策。
3. 更新 README 或至少准备完整 RC release notes；重新构建并记录最终 commit/JAR SHA-256。

### NOT YET READY FOR STABLE

稳定版仍缺 CommandKeys 完整 GUI/实际触发、未绑定宏单对象隔离、MaLiLib 剩余 GUI 边界、明确的跨 Provider 联合冲突/编辑、外部 Provider GUI、导出不可写失败路径，以及代表性布局/返回链证据。这些不是算法测试数量或“未发现问题”的概括反馈可以替代的项目。

### 非阻断已知限制

- 缺少 F25、M6～M8、26.3-only 特殊硬件：物理部分可 BLOCKED；数据编码/不可应用保护已 PASS。
- version 1 不导入 modifiers、CommandKeys 不导出、重复 ID 旧快照需人工消歧、未知 version 维持历史行为：应披露，不在 Phase 5A 扩大修复。
- Gradle 10 deprecated-feature 警告是后续维护风险；当前 Gradle 9.8.2 构建成功。

## 6. 维护者下一步

1. 按缩减后的最小清单执行补测，优先 R-12（只编辑一个未绑定宏的隔离）与 R-03（不可写目标）；已通过的覆盖、四键和重启不重复。
2. 决定发布渠道和版本命名；在决定前不要创建 `v*` tag。
3. 决定许可证表达、修改署名和源码交付方式。
4. 准备 26.3 README/更新日志/已知限制。
5. 剩余门槛完成或明确豁免后进入 Phase 5C，处理版本号、README、许可证、更新日志、最终构建、哈希与 tag 前审查。

## 7. 最终结论

当前仍具备预发布技术基础，但按 Phase 5A 原门槛，六项重点测试尚未整组完成，故结论为 **READY FOR PRE-RELEASE（技术基础）/ 尚未通过公开预发布门槛**；同时 **NOT YET READY FOR STABLE**。BUG-01、BUG-02 已关闭，不再列为发布阻碍。建议先完成最小剩余高风险步骤，再批准进入 Phase 5C；若维护者选择接受个别缺口，需逐项记录豁免和发布说明。
