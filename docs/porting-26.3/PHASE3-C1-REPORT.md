# Phase 3 C1：MaLiLib 兼容性迁移报告

日期：2026-10-31  
分支：`port/mc-26.3`  
结论：**CONDITIONAL GO**（C1 实现与可自动验证部分完成，可进入 C2；GUI 可见行为和世界内实际触发仍需人工回归）

## 1. 范围与结论

本阶段只修改了 MaLiLib adapter、其集中式输入转换和必要测试基础设施。未修改 CommandKeys/Mod Menu 业务适配、`VisualKeymapApi`、version 1 schema、Minecraft provider 或已通过的 B1/B2 语义。

实际结果：

- MaLiLib 0.30.2 与 Tweakeroo 0.30.1 在 Minecraft 26.3 中真实加载。
- `MaLiLibIntegration` 枚举 217 个真实注册绑定，保留 provider `malilib`、`IHotkey.getName()` binding ID 和原遍历顺序。
- 真实 26.2 混合快照中的 `tweakFreeCamera`/`tweakHoldAttack` 被唯一匹配，J=74、F9=298 应用、保存、重启和再导出语义一致。
- 单键、四键组合、鼠标、未绑定、重置、非鼠标负值保留和 MaLiLib 缺席启动均取得运行证据。
- 当前 Windows computer-use 层不能枚举 SDL 创建的 Minecraft 窗口，因此 GUI 可见行为和世界内实际热键触发没有被冒充为 PASS。

## 2. 依赖与真实加载证据

| 项目 | 实际版本/构件 | 证据 |
| --- | --- | --- |
| Minecraft | 26.3 | `runs/mc-26.3-c1/logs/latest.log` |
| Fabric Loader | 0.20.1 | 同上实际 mod 列表 |
| Fabric API | `0.165.0+26.3` | 同上实际 mod 列表 |
| MaLiLib | `maven.modrinth:malilib:0.30.2` | JAR SHA-256 `ABD59F642ABD891586BFEC2CD1937771DED925081EBE3870D152CA96356B5119` |
| Tweakeroo 测试 provider | Modrinth version ID `fWxKATn6`，模组版本 0.30.1 | JAR SHA-256 `E26C316B2CB336EBD8439A29B48E75D98DF2E20F205EFD57E0F29197F28EEC56`；metadata 要求 MC `>=26.3 <26.4`、MaLiLib `>=0.30.2- <0.31.0-` |

Tweakeroo 0.30.1 的目标版本发布物也可由 [CurseForge 文件页](https://www.curseforge.com/minecraft/mc-mods/tweakeroo/files/9100472) 交叉确认。测试依赖使用精确 Modrinth version ID，不依赖个人缓存中的临时坐标。

## 3. MaLiLib 26.2 → 26.3 API 对应

对 MaLiLib 0.30.2 JAR 做 `javap` 签名检查，并在真实客户端调用：

| 26.2/0.29 行为 | 26.3/0.30.2 | 处理 |
| --- | --- | --- |
| `InputEventHandler.getKeybindManager().getKeybindCategories()` | 签名保留 | `MaLiLibIntegration#getKeyBindings()` 架构不变。 |
| `KeybindCategory#getModName()/getHotkeys()` | 签名保留 | 类别名、hotkey 遍历与顺序不变。 |
| `IHotkey#getName()/getConfigGuiDisplayName()/getComment()/getKeybind()` | 签名保留 | ID、显示名、comment 语义不变。 |
| `IKeybind#getKeys()/clearKeys()/addKey()/isModified()/resetToDefault()` | 签名保留 | 只在键码边界增加转换和非支持值保留。 |
| `ConfigManager.saveAllConfigs()` + `IKeybindManager.updateUsedKeys()` | 签名保留 | `MaLiLibIntegration#save()` 未改变。 |
| `KeybindMulti.keyCodes` 语义 | 0.30.2 内部字段为 `scanCodes` | 键盘正数现在是 SDL scancode，不能直接暴露为 Visual Keymap 规范整数。 |
| 鼠标为负数 | 已确认仅 `-99..-92` 对应八个鼠标键 | 不再使用原版“所有 `< -1` 都加 100”的宽泛规则。 |
| 无 gamepad 范围冲突 | 0.30.2 有 button `-200..`、axis `-300..` | 不把它们转换成鼠标；编辑支持输入时保留原始负值。 |

## 4. 代码修改

### 4.1 生产代码

- `src/main/java/dev/xef2/visualkeymap/integration/MaLiLibInputCode.java`
  - `toCanonicalCode(s)`：`-99..-92` 构造 typed mouse key；正数构造 typed SDL keyboard key；统一经 `MinecraftInputCode` 转换成规范整数。
  - `toScanCode()`：规范键先由集中式 B1/B2 层验证；键盘只在 MaLiLib 边界写 SDL scancode，鼠标写 `-99..-92`。
  - `replaceSupportedScanCodes()`：替换键盘/鼠标槽位时，未支持的 gamepad/其他负值保持原值与相对位置；清空支持键不会顺带删除它们。
- `src/main/java/dev/xef2/visualkeymap/integration/MaLiLibIntegration.java`
  - `MaLiLibKeyBinding#getKeyCodes()` 改为调用上述集中层。
  - `setBoundKeys()` 改为非破坏性替换后再调用 MaLiLib `clearKeys/addKey`。
  - `getKeyBindings()`、`save()`、provider/ID、显示名、comment、搜索、默认判断和 reset 未改。

未支持的 gamepad/其他负值不会进入现有 `public List<Integer>` 或 version 1 快照，因为项目没有获批的 gamepad 规范命名空间；它们在 MaLiLib live binding 中被保留，避免用户编辑键盘/鼠标时静默损坏原值。这不是新增 gamepad 功能。

### 4.2 测试基础设施

- `MaLiLibInputCodeTest`：6 个 JUnit 用例，直接引用 MaLiLib 0.30.2 `ScanCodes` 常量。
- `src/c1Testmod/`：仅开发运行的 Fabric client probe，使用真实 Tweakeroo 注册对象完成多阶段保存/重启测试。
- `src/optionalDependencyTestmod/`：验证 MaLiLib/Tweakeroo 缺席时 Visual Keymap 能到达 `CLIENT_STARTED`。
- `build.gradle`：两个独立 source set 和运行配置；`excludeMaLiLibRuntime` 只控制开发测试运行时。

最终发布 JAR `build/libs/visual-keymap-26.3-1.4.0.jar` 已检查，不含 `c1test` 类或测试 `fabric.mod.json`。

## 5. 执行命令与结果

所有 Gradle 命令使用 GraalVM JDK 25.0.4、隔离的 `GRADLE_USER_HOME`，并把 TEMP/TMP 指向工作区 `.tmp`。

| 命令 | 结果 |
| --- | --- |
| `gradlew clean test build compileC1TestmodJava compileOptionalDependencyTestmodJava --no-daemon --offline` | **PASS**；最终生产 build 与两个探针均编译。 |
| `gradlew runC1IntegrationClient ... -Pc1Stage=apply -Pc1Snapshot=<真实私有快照>` | **PASS**；真实混合快照匹配两个 MaLiLib ID。 |
| `gradlew runC1IntegrationClient ... -Pc1Stage=exercise` | **PASS**；单键、组合、鼠标、负值保留、清空、重置、导出并保存组合。 |
| `gradlew runC1IntegrationClient ... -Pc1Stage=verifyCombo` | **PASS**；四键组合跨重启保持，随后恢复 J/F9 并保存。 |
| `gradlew runC1IntegrationClient ... -Pc1Stage=verify` | **PASS**；恢复后的 J/F9 再次跨重启保持并导出。 |
| `gradlew runNoMaLiLibClient ... -PexcludeMaLiLibRuntime` | **PASS**；Visual Keymap 加载，MaLiLib/Tweakeroo 均缺席。 |

JUnit XML：`CanonicalInputCodeTest` 1/1、`MaLiLibInputCodeTest` 6/6，failure/error/skipped 均为 0。

Mod Menu update checker 因沙箱网络权限记录 `ConnectException`，不影响本地 mod 加载、探针断言或构建结果；这不是 MaLiLib adapter 失败。

## 6. 动态功能结果

| 功能 | 状态 | 证据 |
| --- | --- | --- |
| 枚举真实 MaLiLib/Tweakeroo 热键 | **PASS** | `registeredBindings=217`；两个基线 ID 均存在。 |
| 单键盘读取/修改 | **PASS** | J：规范 74 ↔ SDL 13；F9：规范 298 ↔ SDL 66。 |
| 多键/键鼠组合保存重启 | **PASS** | `[74,298,0,1]` ↔ `[13,66,-99,-97]`，完整重启后保持。 |
| 八个鼠标码边界 | **PASS** | JUnit 覆盖全部 `-99..-92`；真实对象另验证左/中/右/M8 为 `[0,2,1,7]`。 |
| 未绑定 | **PASS（adapter）** | 支持键清空后真实 `IKeybind#getKeys()` 为空。 |
| 重置 | **PASS（adapter）** | `resetToDefault()` 后 `isDefault=true`。 |
| gamepad/其他负值 | **PASS（非破坏策略）** | 真实对象 `[13,-200,-300]` 编辑键盘后为 `[66,-200,-300]`；公开规范列表只含 298。 |
| `saveAllConfigs()`/重启持久化 | **PASS** | `tweakeroo.json` 落盘为 J/F9；两次后续启动保持；组合也单独跨重启通过。 |
| MaLiLib provider 快照匹配/再导出 | **PASS（语义）** | 见下一节。 |
| MaLiLib 缺席可选依赖 | **PASS** | probe：`visual-keymap=true`、`malilib=false`、`tweakeroo=false`。 |
| GUI 显示、搜索、冲突、排序、按钮状态 | **BLOCKED（自动化环境）** | SDL Minecraft 窗口未被 computer-use 枚举；未进行盲坐标操作。 |
| 世界内实际热键触发 | **NOT TESTED** | 无可自动化窗口/测试世界；不能仅凭配置持久化宣称触发通过。 |

## 7. 真实 26.2 快照往返

原件：工作区私有 `baseline-private/26.2/version 1.json`，SHA-256：

`B3244C2560E54D84D459277AD9C12BCCB19F6AB9D5DF11D2226052FBFD320970`

探针读取的是该未修改混合快照，MaLiLib provider 动态匹配结果：

| provider | ID | 26.2 规范 keys | 26.3 MaLiLib raw | 重启后再导出 |
| --- | --- | --- | --- | --- |
| `malilib` | `tweakFreeCamera` | `[74]` | `[13]`（SDL J） | `[74]` |
| `malilib` | `tweakHoldAttack` | `[298]` | `[66]`（SDL F9） | `[298]` |

两份 26.3 MaLiLib-only 再导出文件的 SHA-256 均为：

`C883934384A7676649C4222D34C4381472DF5D89F5E34D8455A0ED14D116500F`

二者均保持 `version=1`、provider、ID、规范 keys 和空 modifiers。输出是明确的 26.3 派生结果，不冒充原版完整混合快照；字节哈希不要求与混合原件相同。

## 8. Git 提交

| 提交 | 范围 |
| --- | --- |
| `40412fb` `fix: migrate MaLiLib input codes` | 生产 adapter 与集中式 MaLiLib 输入转换。 |
| `5678b3d` `test: cover MaLiLib input conversion` | JUnit、精确 Tweakeroo 测试依赖。 |
| `9439a41` `test: add MaLiLib integration probes` | 真实 Fabric/Tweakeroo 多阶段探针与无 MaLiLib 启动探针。 |
| 本次文档提交 | `TEST-PLAN.md` 状态与本报告。 |

## 9. 剩余问题和进入 C2 建议

### 不阻碍 C2 的受控风险

1. GUI 中 MaLiLib 类别/显示名/comment、内部名搜索、冲突与排序需要人工观察。
2. GUI 按住 2–4 键、首键提前释放、鼠标点击编辑、重置按钮状态需要人工操作。
3. Free Camera/Hold Attack 在测试世界内的实际触发尚未验证。
4. gamepad-only binding 在 GUI 中会表现为无可表示的键；现阶段只保证其 live 原值不被键盘/鼠标编辑静默破坏，不新增公共 gamepad 语义。

### 决策

建议 **CONDITIONAL GO 进入 C2**。理由是 C1 的生产适配、真实依赖加载、真实 provider、保存/重启、快照和缺席矩阵均通过；剩余项目是可独立人工回归的用户可见/实际触发证据，不要求改变 C1 架构或公共接口。进入 C2 不得把这些项目改写为 PASS，完整回归阶段仍须补测。
