# BUG-01 最终验收与合并记录

## 1. 最终状态

BUG-01 标记为 **FIXED / VERIFIED**。

- 修复分支：`fix/bug01-invalid-json-26.3`
- 目标分支：`port/mc-26.3`
- 目标分支原基线：`7d41278`
- 修复分支人工验收提交：`827a0b2`
- 非 fast-forward 合并提交：`796bb8d4eac16200db8416555d2437f0504562fe`
- 合并父提交：`7d41278f362d4738bb39d24a6d64e59155752a73`、`827a0b2ea70ac2a37a243eb3b7d07eec2ccbd8ed`

合并使用 `--no-ff`，无冲突，保留了 BUG-01 调查、失败测试、生产修复、验证和人工验收的完整历史。修复分支未删除；没有 push、tag、Release 或版本号修改。

## 2. 根因与修复

`KeymapSnapshot.readFromFile()` 原来直接让 Gson 解析用户文件，不检查 null 根节点和必要结构。`VisualKeymapScreen.doImport()` 又只捕获 `IOException`，因此非法 JSON 产生的 `JsonSyntaxException` 或解析后的结构性空值异常会从 SDL 文件对话框回调逃逸到 Minecraft 26.3 客户端主循环，最终导致客户端崩溃。

最小修复包括：

1. `KeymapSnapshot.readFromFile()` 对 null 根、缺少 `bindings`、null binding 和 keys/modifiers 中的 null 元素做最小结构校验，以 `JsonSyntaxException` 安全拒绝；保留 I/O 异常类别。
2. `VisualKeymapScreen.doImport()` 精确区分 `IOException` 与 `JsonParseException`，记录完整开发日志，并显示英/中本地化 `AlertScreen`。
3. 提示关闭后返回同一个 Visual Keymap 屏幕；一次性抑制错误提示切屏引发的 `removed()` 保存，之后正常关闭仍按既有行为保存。
4. 不在 `NativeFileDialog` 捕获通用 consumer 异常，不使用 `catch (Exception)`，不返回伪造空快照。

## 3. 兼容性审查

合并前后审查结果：

- BUG-02 的 `matchBindings()` 分组匹配、歧义选择和一对一约束未修改。
- version 1 record 字段与 JSON schema 未修改；没有 version 2。
- 未知 version 和缺失 version 的既有处理语义由测试锁定并保持不变。
- modifiers 的历史导入语义未修改。
- F25 tombstone 和未知扩展键的 `canApply()` 保护未修改，既有测试继续通过。
- `VisualKeymapApi` 及其他既有 public/protected 签名未修改。新增的 `prepareImport()` 为 package-private 测试边界；匿名 `AlertScreen` 的 `onClose()` 只是覆盖 Minecraft 生命周期方法，不构成新的项目公开 API。
- MaLiLib、CommandKeys、Mod Menu adapter/entrypoint 均无源码修改。
- `NativeFileDialog` 无修改，未增加无差别异常捕获。
- 私人 `baseline-private/` 材料未被 Git 跟踪；既有未跟踪 `docs/bugfixes/EVIDENCE-INVENTORY.md` 保持未修改、未提交。

## 4. 人工验收

维护者报告已在 Minecraft 26.3 实际客户端中完成 `BUG-01-MANUAL-TEST.md` 的全部项目。以下结论是维护者提供的真实客户端人工验收结果，不是 Agent 自行执行的自动化测试；本次反馈没有提供新的截图、录像、日志或哈希，本文不补造这些证据。

| 项目 | 结果 | 维护者确认范围 |
| --- | --- | --- |
| M-01 | **PASS** | 原始非法 JSON 安全拒绝，不崩溃、不改变键位。 |
| M-02 | **PASS** | 按钮与 Esc 正确关闭提示并返回。 |
| M-03 | **PASS** | 连续导入各种非法输入均安全且无键位变化。 |
| M-04 | **PASS** | 取消文件选择不错误触发提示。 |
| M-05 | **PASS** | 失败后立即正常导入有效快照。 |
| M-06 | **PASS** | BUG-02 重复 ID 安全匹配无回归。 |
| M-07 | **PASS** | 完全退出、重启后配置无非预期变化。 |
| M-08 | **PASS** | 英文提示正常。 |
| M-09 | **PASS** | 简体中文提示正常。 |
| M-10 | **PASS** | P1/P5 日志和 Provider 共存正常。 |

人工验收合计：**10/10 PASS**，未发现问题。

## 5. 合并后自动化构建

在合并提交 `796bb8d` 上执行：

```text
.\gradlew.bat clean test build --no-daemon
```

结果：**BUILD SUCCESSFUL in 40s**，9 个任务全部执行。

| 指标 | 结果 |
| --- | ---: |
| JUnit suites | 7 |
| tests | 39 |
| failures | 0 |
| errors | 0 |
| skipped | 0 |

这 39 项同时覆盖 BUG-01 输入矩阵和零副作用流程，以及既有 BUG-02 匹配/选择、规范键码、MaLiLib 转换、F25 tombstone 和未知扩展键保护。

## 6. 合并后生产 JAR

| 项目 | 值 |
| --- | --- |
| 构建提交 | `796bb8d4eac16200db8416555d2437f0504562fe` |
| 文件 | `build/libs/visual-keymap-26.3-1.4.0.jar` |
| 大小 | 107,834 B |
| SHA-256 | `9C70445275A5AE16AF41788244B5D9C4506746802679BD8576FB788D671BB928` |
| JAR entries | 68 |
| 测试类/探针 | 0 |
| nested JAR | 0 |

包内容扫描未发现第三方实现包 `fi/dy/masa/`、CommandKeys 实现包或 `com/terraformersmc/`。JAR 中名称含 MaLiLib、CommandKeys、Mod Menu 的类均为 Visual Keymap 自身的 adapter/entrypoint，属于预期生产代码。

### 6.1 与人工验收候选 JAR 的差异

人工验收候选：107,838 B，SHA-256 `87DCC986995EC9F9CFFC2623403200C7B4CDE515BEE07F1597848B2DD616703B`。

合并后 JAR 的原始哈希不同，因此不能宣称两个文件字节相同。逐 entry 解压比较结果：

- 两者均有 52 个非目录 payload。
- 50 个 payload 字节完全相同，包括全部 class、`fabric.mod.json`、Mixin 配置及其他资源。
- 仅 `assets/visualkeymap/lang/en_us.json` 与 `zh_cn.json` 的原始字节不同。
- 两个语言文件在 CRLF/LF 归一化后逐字符相同，解析后的 JSON 语义相同；候选文件各含 61 个 CRLF，合并构建各含 67 个 CRLF。

所以哈希变化由两项语言资源的换行形式造成，没有发现生产逻辑、metadata 或本地化文本语义差异。

## 7. 合并后成品 JAR 集成回归

四组均使用独立 gameDir，实际从合并后发布 JAR 加载 Visual Keymap；结果文件记录上述新 SHA-256。

| 组合 | 结果 | Provider / bindings | BUG-01 关键断言 |
| --- | --- | --- | --- |
| P1 原生 | **PASS** | `MinecraftImpl` / 63 | invalid snapshot rejected；bindings unchanged；alert lifecycle PASS。 |
| P3 MaLiLib/Tweakeroo | **PASS** | `MinecraftImpl, MaLiLibIntegration` / 280 | 同上。 |
| P5 全依赖 | **PASS** | `MinecraftImpl, MaLiLibIntegration, CommandKeysIntegration` / 282 | 同上；可选 Provider 共存。 |
| P6 外部 Provider | **PASS** | `MinecraftImpl, Phase4Provider` / 64 | 同上；错误提示没有触发外部 Provider 保存。 |

WMI/perflib 和离线 Mojang/更新检查警告仍属于宿主或网络环境，不影响客户端到达 `CLIENT_STARTED` 和探针 PASS。

## 8. BUG-02 无回归

- 维护者在 M-06 中确认重复 ID 安全匹配仍正常。
- 合并后的 39 项 JUnit 包含 BUG-02 重复 identity、重复 keys、枚举顺序变化、部分缺席、歧义默认不应用及一对一选择约束。
- 正常 version 1 导入、F25 tombstone 和未知 extension 不可应用规则继续通过。
- BUG-01 生产差异没有触及 `ImportPreviewScreen`、`ImportSelection` 或 BUG-02 的 `matchBindings()` 实现。

结论：没有发现 BUG-01 修复导致 BUG-02、快照格式、键码保护或 Provider 集成回归。

## 9. 剩余限制与风险

- 未知 version 的历史接受行为被刻意保留；是否在未来拒绝未知 version 是独立格式设计问题，不属于 BUG-01，也未实施 version 2。
- 26.2 原版的 GUI 端到端非法 JSON 行为仍缺少同等级动态证据，但不影响已在 26.3 完成的修复与验收。
- Gradle 仍报告未来 Gradle 10 不兼容的 deprecated-feature 警告；当前 Gradle 9.8.2 构建成功，因此它是后续维护风险，不是当前 26.3 发布阻碍。
- 没有物理 F25 或其他特殊硬件只限制物理触发覆盖；快照数据安全保护已经通过自动化与人工验收，不应单独阻止发布。

## 10. Phase 5 就绪性

BUG-01 与 BUG-02 均已修复、自动化验证、维护者人工验收并合入 `port/mc-26.3`，因此已经具备**恢复 Phase 5 发布就绪性审查**的条件。

仍需由 Phase 5 明确收口的真正发布门槛来自既有 Phase 4 人工回归计划，而不是 BUG-01/BUG-02：

- MaLiLib GUI、搜索/冲突/排序、多键编辑和实际热键触发。
- CommandKeys profile、宏编辑/搜索/保存、重启和本地世界实际触发。
- Mod Menu 实际配置按钮、Done/Esc 与反复打开关闭。
- 外部 Provider 的完整 GUI、回调和快照流程。
- 两种语言、代表性 GUI Scale、SDL 文件对话框其余取消/失败/覆盖路径。
- Minecraft、MaLiLib、CommandKeys 多 Provider 联合编辑、保存和重启。

这些项目如果尚未执行，仍应在正式 Release 前按 `MANUAL-REGRESSION-GUIDE.md` 记录状态。物理 F25/26.3-only 键可以因设备条件标记 BLOCKED，只要已经验证的数据不可错误应用/不可丢失策略保持 PASS，不应自动判定项目不可发布。

最终判断：**BUG-01 FIXED / VERIFIED；可以恢复 Phase 5 发布就绪性审查，但本报告本身不授权发布。**
