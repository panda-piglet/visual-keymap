# Visual Keymap Phase 3 B2.5 验收报告

日期：2026-10-31

分支：`port/mc-26.3`

范围：Loom 测试目录隔离、Gradle 自动化测试、B2 核心 GUI/输入/快照/配置动态回归，以及进入 C 阶段前的依赖复核。未修改 MaLiLib/CommandKeys adapter、public API 或快照 schema。

## 1. 结论

本阶段结论为 **CONDITIONAL GO**。

B2 的核心实施已经达到进入 C 阶段的条件：Minecraft 26.3 客户端使用独立 gameDir 启动；`clean test` 和 `clean build` 均成功且实际执行 B1 编码自检；原版 Minecraft provider 的普通键、鼠标、未绑定、重置、搜索、导入预览、确认应用、保存、重启恢复、SDL 打开/保存/取消和 version 1 语义往返均获得动态证据；F25 tombstone 和未知 extension 均不会应用到 live binding。

原先“缺少真实 26.2 Minecraft provider 快照”的条件已经关闭。维护者补充的原始快照同时包含 Minecraft 与 MaLiLib provider；Minecraft F8 子集已经完成 26.2→26.3 导入、应用、重启和再导出动态往返。

仍有两个受控条件：

1. 同一真实快照中的 MaLiLib 记录，其 live 匹配、应用和再导出必须等待 C1；B2.5 没有修改或调用尚未迁移的 adapter 写入路径。
2. 当前 CommandKeys `3.104.1+26.3` 依赖实际解析为 NeoForge JAR，Fabric Loader 没有加载它。C2 在修正为明确的 Fabric 发布物前为 `BLOCKED`；这不阻止先启动 C1 或 C3。

本报告完成后停止，不进入 C1/C2/C3。

## 2. Git 提交与改动边界

| 提交 | 范围 | 文件 |
| --- | --- | --- |
| `8127205` `test: isolate Minecraft 26.3 client run directory` | 为 Loom client run configuration 设置独立目录 | `build.gradle` |
| `05ae402` `test: run canonical input checks with JUnit` | 用 JUnit 5 包装并执行既有 B1 main 自检；未改自检逻辑 | `build.gradle`、`CanonicalInputCodeTest.java` |
| `34d8fc5` `docs: record Phase 3 B2.5 validation` | 记录首轮 B2.5 证据、状态和 C 阶段门槛 | 本文件 |
| 本次补充提交（哈希见最终交付） | 记录真实混合 provider 原件及 Minecraft F8 跨版本往返 | 本文件、`BASELINE-VALIDATION.md`、`TEST-PLAN.md` |

本阶段没有修改 `src/main`、`MaLiLibIntegration`、`CommandKeysIntegration`、`VisualKeymapApi`、version 1 schema 或任何 Mixin。`runs/mc-26.3-b2.5/`、Gradle 报告和测试中生成的快照均为 Git ignored 本地证据，不纳入提交。

## 3. Loom gameDir 隔离

### 3.1 配置与静态确认

Loom 1.19.3 的 client run configuration 使用：

```groovy
loom {
    runs {
        client {
            runDir = "runs/mc-26.3-b2.5"
        }
    }
}
```

本地 Loom 1.19.3 类检查确认 `RunConfigSettings` 暴露 `getRunDir`、`setRunDir` 和 `runDir(String)`。临时只读 init script 对 `help` 与 `runClient --dry-run` 均打印：

```text
B25_CLIENT_RUN_DIR=runs/mc-26.3-b2.5
```

这修复了 B2 报告中“只设置 JavaExec workingDir、Fabric gameDir 仍是 `run/`”的问题。

### 3.2 运行时证据

第一次启动前目标目录不存在；启动后在以下路径产生运行数据：

```text
runs/mc-26.3-b2.5/.fabric/
runs/mc-26.3-b2.5/config/
runs/mc-26.3-b2.5/data/
runs/mc-26.3-b2.5/downloads/
runs/mc-26.3-b2.5/logs/
runs/mc-26.3-b2.5/mods/
runs/mc-26.3-b2.5/resourcepacks/
runs/mc-26.3-b2.5/resources/
runs/mc-26.3-b2.5/saves/
runs/mc-26.3-b2.5/options.txt
```

运行日志中的 MaLiLib 配置访问路径为：

```text
...\runs\mc-26.3-b2.5\config\malilib.json
```

SDL 打开/保存对话框的默认目录也是 `runs/mc-26.3-b2.5/config`。这些运行时路径以及独立目录内的 `.fabric`、`options.txt`、`config` 和 `logs` 共同证明 Minecraft/Fabric 实际使用了指定 gameDir，而不只是进程 working directory。

旧 `run/` 在启动前后保持以下指纹，未清理、恢复或覆盖：

| 文件 | 字节 | UTC mtime | SHA-256 |
| --- | ---: | --- | --- |
| `run/config/modmenu.json` | 842 | `2026-10-30T07:02:07.6483057Z` | `E8F1BECEE4D20E11702910E0D3EEDBE35175922C6BA1A98B52EFEB8C9AB3A11D` |
| `run/config/visualkeymap.json` | 90 | `2026-10-30T12:16:15.1648091Z` | `8FDEFEC5CFCDEA534FA85C6726E57D53BFA1FF9D974F5D3893F1932C76429B3D` |
| `run/options.txt` | 5,459 | `2026-10-30T12:17:31.7723523Z` | `07C831DE376FBC9554BFD5486C6D56D7DB87BEF9328825AC4BCFAC68DD4AFD52` |

结论：gameDir 隔离 **PASS**，不再阻碍 B2 动态测试。

## 4. Gradle 自动化测试与构建

### 4.1 测试接入

原 `CanonicalInputCodeSelfTest` 是独立 `main`，Gradle 9.8.2 的 `test` 任务因此报告没有可发现测试。本阶段保留该类和全部断言不变，新增一个 JUnit 5 测试入口调用其 `main`，并固定使用 JUnit `5.13.4` BOM 与 launcher。没有使用 `failOnNoDiscoveredTests=false`、跳过 `test` 或禁用测试。

JUnit XML 实际结果：

```text
tests=1 skipped=0 failures=0 errors=0
PASS CanonicalInputCodeSelfTest
legacy=121 extension=127 assignedKeyboard=247 mouse=8
extensionTableSha256=0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28
```

报告路径：

- `build/reports/tests/test/index.html`
- `build/test-results/test/TEST-dev.xef2.visualkeymap.input.CanonicalInputCodeTest.xml`

### 4.2 实际命令

所有 Gradle 命令使用 GraalVM JDK `25.0.4` 和工作区内 `GRADLE_USER_HOME`。

| 命令 | 结果 |
| --- | --- |
| `.\gradlew.bat clean test --no-daemon --info` | **PASS**；JUnit 发现并执行 1 个测试，0 failure/error。 |
| `.\tools\porting-26.3\Test-CanonicalInputCode.ps1` | **PASS**；与 JUnit 输出和表哈希一致。 |
| `.\gradlew.bat clean build --no-daemon` | **PASS**；`:test` 实际执行，8 个任务完成。 |
| `.\gradlew.bat runClient --no-daemon` | **PASS**；多次进入标题界面并正常退出，最后两次分别 `BUILD SUCCESSFUL in 3m 21s` 和 `1m 38s`。 |

当前 Codex 终端把 `TEMP/TMP` 暴露为 Windows 8.3 短路径 `C:\Users\PANDA_~1\...` 时，GraalVM JDK 25 的 `PipeImpl` 会报 `Invalid argument: connect`，Gradle 表现为 `Unable to establish loopback connection`。仅为命令进程把 `TEMP/TMP` 指向工作区 `.tmp` 后，`gradlew help` 和 `runClient` 恢复成功。该问题没有修改项目配置，属于当前终端环境风险；后续命令应继续使用普通长路径临时目录。

结论：B1 自动化与完整 Gradle build **PASS**。

## 5. 快照分类与真实 26.2 证据边界

### 5.1 A 类：Minecraft provider

维护者补充了两个未修改原件，保存在仓库外的本地私有目录：

| 原件 | 字节 | SHA-256 | Git 处理 |
| --- | ---: | --- | --- |
| `baseline-private/26.2/version 1.json` | 448 | `B3244C2560E54D84D459277AD9C12BCCB19F6AB9D5DF11D2226052FBFD320970` | 私有保留，不提交 |
| `baseline-private/26.2/options.txt` | 5,360 | `8CB130FEF450BD991D55A9C89D48E3891FAF4BA72C1389F85C720D45EC9FBB28` | 个人完整配置，不提交 |

JSON 是 version 1，共 3 条记录，不是纯 Minecraft 快照。Minecraft 记录为：

```text
minecraft|visualkeymap.key.open_keymap|297|
```

原始 `options.txt` 同时包含：

```text
key_visualkeymap.key.open_keymap:key.keyboard.f8
```

因此 provider、稳定 ID、F8 规范整数 `297` 和空 modifiers 与 Minecraft 26.2 实际持久化文本交叉一致。

为避免调用尚未迁移的 MaLiLib adapter，测试从原件按 `provider == "minecraft"` 过滤生成 ignored 派生文件：

```text
runs/mc-26.3-b2.5/config/b25-derived-from-real-26.2-minecraft-only.json
SHA-256 D76F01E5EAC78436CED81C5ECE53FF68001413CE1650822F12F215FDB2A58127
```

该文件明确是测试派生物，不是 26.2 原始导出。原件测试前后 SHA-256 未改变。

实际动态结果：

1. 26.3 初始 live binding 为 `Not Bound`。
2. 导入派生副本后，预览显示 `1 keybindings changes` 和 `Not Bound → F8`，默认勾选。
3. `Import Selected` 后，原版 Key Binds 立即显示 F8，隔离实例 `options.txt` 写入 `key.keyboard.f8`。
4. 完全退出并重启客户端后，原版 Key Binds 仍显示 F8。
5. 重启后导出 `b25-real-26.2-minecraft-roundtrip.json`，SHA-256 为 `15E948A1D8BCDB0A7DCA913F91DA7D32D01AE0211CFEBB73B2B75D3596E3D662`。
6. 再导出文件仍为 version 1，Minecraft 记录仍为 `minecraft|visualkeymap.key.open_keymap|297|`，语义比较为 `True`。

结论：S-05 的“真实 26.2 Minecraft F8 provider 跨版本往返”子项 **PASS**；“缺少真实 Minecraft provider 快照”遗留问题已关闭。S-05 对 0–7 鼠标、unknown/空绑定及非空 modifiers 的完整覆盖仍未完成。

### 5.2 B 类：MaLiLib provider

新增的真实 26.2 原件是混合 provider 快照，其中 MaLiLib 部分为：

| provider | ID | keys | modifiers |
| --- | --- | --- | --- |
| `malilib` | `tweakFreeCamera` | `[74]`（J） | `[]` |
| `malilib` | `tweakHoldAttack` | `[298]`（F9） | `[]` |

version 1 结构和规范码与 B1 表一致，静态解析 **PASS**。原有 `test-fixtures/porting-26.3/snapshots/real-26.2-malilib-v1.json` 仍是这两条 MaLiLib 记录的仓库语义副本。C1 尚未迁移，因此其 live binding 匹配、应用、保存和再导出为 **BLOCKED**，不能作为 B2 硬性失败。

### 5.3 C 类：静态/合成专项

- F25 tombstone 合成输入使用 `314`。
- 未知 extension 合成输入使用 `16777344`（`0x01000080`，位于批准命名空间内但未分配）。
- 两者预览均显示 1 个变化，但 checkbox 禁用且未选中；F25 显示“不可用于当前 Minecraft”，未知值显示“不支持的键码”。取消后 live binding 仍为未绑定，未映射到其他有效键。
- 鼠标中键实际导出为 `keys:[2]`；没有泄漏 SDL keyboard integer。

这些合成文件只保存在隔离 gameDir，明确标记为合成数据。

## 6. B2 核心动态回归

| 场景 | 状态 | 运行证据与边界 |
| --- | --- | --- |
| N-01 原生入口 | **PASS** | `Options → Controls → Visual Keymap...` 存在并打开完整主界面。 |
| N-02 返回链 | **PASS（Done）/ NOT TESTED（Esc）** | Done 依次返回 Controls、Options、标题界面；本轮未单独测 Esc 父屏路径。 |
| N-04 默认绑定 | **PASS** | 打开键初始显示 `Not Bound`。 |
| L-04 搜索 | **PASS（代表样本）** | 搜索 `Visual Keymap` 后匹配项置顶/高亮，清空后恢复；多词、大小写和空格完整矩阵仍未执行。中文 IME 对低层按键注入产生候选框，但直接文本输入正常，不属于模组回归。 |
| E-01 普通键编辑 | **PASS（读写/保存）** | Visual Keymap 内改为 F8，快照写 `297`；原版 Key Binds 中改为 F7，导入恢复 F8。实际游戏动作触发未单独测试。 |
| E-03 鼠标 | **PASS（左/右/中）/ NOT TESTED（M4–M8）** | 左、右、中键均立即显示；左右键出现与攻击/使用的冲突提示；中键快照写 `2`。自动化覆盖 mouse 0–7 往返，但当前控制工具不能注入侧键。 |
| E-06 未绑定/重置 | **PASS** | Reset 后显示 `Not Bound`，`options.txt` 写 `key.keyboard.unknown`。 |
| E-07 持久化 | **PASS** | F8 写入 `options.txt` 为 `key.keyboard.f8`；完全退出并重启后原版 Key Binds 仍显示 F8。 |
| E-08 即时刷新 | **PASS** | F8、F7、鼠标和 Not Bound 均在操作后立即刷新。 |
| C-02/C-03 配置 | **PASS（showNumpad）/ NOT TESTED（其余两项）** | `showNumpad=false` 立即写入 JSON，返回后数字键区消失；完全退出重启后仍隐藏；再恢复 `true` 后布局和 JSON 同步恢复。排序模式、冲突置顶的重启矩阵未执行。 |
| X-01 对话框/取消 | **PASS** | 保存标题 `Export Keybindings`、默认名 `visualkeymap-export.json`、JSON 过滤、隔离 config 默认目录；取消后返回且未创建目标文件。打开对话框同样使用 JSON 过滤和隔离目录。 |
| X-02/X-03 实际导出 | **PASS（Minecraft 样本）** | version 1、provider/ID、规范码和 modifiers 正确；再导出语义相等。 |
| X-06/X-07 预览/确认 | **PASS（全选主路径）** | 预览标题显示 1 个变化，F7→F8，默认选中；确认后 live binding 和再导出均为 F8。部分选择和 Esc 取消未测。 |
| X-09 键盘/鼠标/空值 | **PASS（F8、中键、空绑定）/ NOT TESTED（全部 0–7）** | F8=297、中键=2、Reset=unknown；M4–M8 动态缺口同 E-03。 |
| S-01/S-02 | **PASS** | 121 legacy、127 extension、247 keyboard、8 mouse 的碰撞和往返自动化通过。 |
| S-04 集中边界 | **STATICALLY VERIFIED** | B2 核心 GUI/provider/快照仍只通过集中兼容层；C1/C2 adapter 留待各阶段审计。 |
| S-05 真实 26.2 往返 | **PASS（Minecraft F8）/ BLOCKED（MaLiLib live）/ NOT TESTED（鼠标等）** | 真实混合快照的 Minecraft 子集完成导入、应用、重启和 version 1 再导出，规范语义相等；MaLiLib 等待 C1；0–7 鼠标、unknown/空绑定和非空 modifiers 不在该原件中。 |
| S-06 F25 | **PASS（不可应用策略）/ NOT TESTED（物理触发）** | 314 在预览中明确不可用且禁用，不改变 live binding；没有可用物理 F25 事件。 |
| S-07 未知扩展值 | **PASS（不可应用策略）** | 未分配 extension 显示 unsupported、不可勾选、不可应用且不覆盖原绑定。 |

没有发现需要修改 B2 生产代码的迁移回归。

## 7. SDL 文件对话框与失败路径

实际完成：

- 打开与保存对话框均成功显示并由 Minecraft 窗口拥有。
- 默认目录、默认文件名和 JSON 类型过滤正确。
- 用户取消后返回 Visual Keymap，游戏继续响应。
- 实际保存、实际选择文件、导入预览和确认均成功。
- 完全退出重启后配置和原版绑定仍在隔离 gameDir。

无效 JSON 测试使用隔离目录内的合成文件，客户端在 `KeymapSnapshot.readFromFile:47` 抛出 `JsonSyntaxException` 并生成：

```text
runs/mc-26.3-b2.5/crash-reports/crash-2026-10-30_22.03.47-client.txt
```

状态为 **FAIL（会崩溃）**，但不是 B2 迁移回归。基线提交 `053e736` 的 `VisualKeymapScreen#doImport` 同样只捕获 `IOException`，而 `KeymapSnapshot.readFromFile` 同样直接调用 Gson；TEST-PLAN X-10 要求记录并保持 26.2 实际行为，而不是默认修复。由于没有真实 26.2 无效 JSON 运行材料，精确跨版本结论仍为 `STATICALLY VERIFIED`。本阶段遵守范围限制，没有修复该历史行为。

不可写输出、不可读文件、未知 version、null 字段和覆盖已有文件仍为 `NOT TESTED`。

## 8. 第三方依赖进入 C 阶段前复核

### 8.1 MaLiLib

MaLiLib `0.30.2` 被 Fabric Loader 实际加载，日志中可见其 mixin 和初始化。B2 核心不依赖其 adapter 行为；C1 可以开始，验收时应使用真实 26.2 MaLiLib golden file 完成 live 往返。

状态：**READY FOR C1**。

### 8.2 Mod Menu

Mod Menu `21.0.0` 被 Fabric Loader 实际加载，B2 客户端可进入标题界面。配置入口/父屏返回属于 C3，尚未在本阶段执行。

状态：**READY FOR C3**。

### 8.3 CommandKeys

当前声明为 `maven.modrinth:commandkeys:3.104.1+26.3`。缓存 JAR 的 manifest 明确写入：

```text
Implementation-Title: neoforge
Implementation-Version: 3.104.1+26.3
```

JAR 含 `META-INF/neoforge.mods.toml` 和 `CommandKeysNeoForge`，不含 `fabric.mod.json`。Fabric debug log只显示它被加入 Java classpath，启动时的 loaded-mod 列表没有 `commandkeys`；因此 `FabricLoader.isModLoaded("commandkeys")` 为 false，当前运行不能验证 C2。

官方 Modrinth 页面另有明确标为 Fabric/26.3 的 `3.104.0+26.3` 发布，版本 ID `D2aoVoRh`，页面给出的可复现坐标是 `maven.modrinth:65UyswbY:D2aoVoRh`：[Command Keys 3.104.0+26.3 Fabric](https://modrinth.com/mod/commandkeys/version/D2aoVoRh)。

建议的最小前置修复是：在 C2 实施前单独修正依赖选择，固定到明确的 Fabric version ID（或作者提供的等价 Fabric 专用 Maven 坐标），然后用 JAR 内 `fabric.mod.json`、Fabric loaded-mod 列表和 `isModLoaded("commandkeys")` 三重验证。不得只依靠类可编译或 classpath 中存在 JAR。

状态：**BLOCKED FOR C2**。本阶段没有修改依赖或 adapter，等待维护者批准后处理。

## 9. 尚未完成的测试

| 项目 | 状态 | 原因/后续 |
| --- | --- | --- |
| 真实 26.2 Minecraft provider F8 跨版本往返 | **PASS** | 原件与 options 交叉一致；26.3 导入、应用、重启和再导出语义一致，见第 5.1 节。 |
| 真实 26.2 MaLiLib live 往返 | **BLOCKED** | C1 adapter 未迁移；C1 完成后执行。 |
| CommandKeys 运行与 adapter | **BLOCKED** | 当前依赖是 NeoForge JAR；先修正 Fabric 依赖选择，再进入 C2。 |
| M4–M8 动态鼠标测试 | **NOT TESTED** | 当前 UI 控制工具只能可靠注入左/右/中；B1 静态往返已覆盖 0–7。 |
| F25/新增键物理输入 | **NOT TESTED** | 无可用物理事件来源；数据不可应用策略已验证。 |
| 配置的冲突置顶/三种排序重启矩阵 | **NOT TESTED** | 本轮只验证 `showNumpad`。 |
| 无效/不可读/不可写/覆盖完整错误矩阵 | **PARTIAL** | 无效 JSON 历史崩溃已记录；其余未执行。 |
| Mod Menu 入口和返回 | **NOT TESTED** | 属于 C3。 |
| 多语言、多 GUI 缩放和完整搜索矩阵 | **NOT TESTED** | 不阻碍 C 阶段启动，最终回归需补齐。 |

## 10. 进入 C1/C2/C3 的建议

- **C1：可开始。** 首批验收必须包括 MaLiLib 单键/多键、鼠标、reset、配置保存和真实 26.2 golden live 往返。
- **C2：暂不可开始 adapter 实施。** 先建立一个仅修正 CommandKeys Fabric 发布物选择的独立构建提交，并验证 Fabric Loader 实际加载；随后再迁移 adapter。
- **C3：可开始。** 使用当前 Mod Menu 21.0.0 验证配置按钮入口、父屏返回和不安装 Mod Menu 的启动组合。
- 真实 26.2 Minecraft provider F8 golden 的 26.3 动态往返已经完成；最终发布前仍需按 TEST-PLAN 补齐鼠标、unknown/空绑定和非空 modifiers 的 S-05 完整覆盖。

最终判定：**CONDITIONAL GO**。
