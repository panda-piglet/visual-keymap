# Phase 3 A / B1 阶段报告

> 执行日期：2026-10-30  
> 分支：`port/mc-26.3`  
> 范围：A 构建环境与依赖迁移；获准的最小 B1 集中式键码层。未执行 B2、C1、C2、C3。

## 1. 结果摘要

| 项目 | 状态 | 证据 | 后续阻碍 |
| --- | --- | --- | --- |
| Phase 3 A 版本与坐标 | COMPLETE | Gradle 9.8.2 / Loom 1.19.3 / Minecraft 26.3 / Loader 0.20.1 / FAPI 0.165.0+26.3 / MaLiLib 0.30.2 / CommandKeys 3.104.1+26.3 / Mod Menu 21.0.0 全部解析 | 无依赖解析阻碍 |
| A 阶段元数据 | COMPLETE | `processResources` 成功，输出 `minecraft ~26.3`、`fabricloader >=0.20.1`、mod version 1.4.0 | 无 |
| A 阶段完整编译 | EXPECTED FAILURE | `compileJava` 到达源码并报告 8 个已计划的 26.3 API 错误；没有 artifact 404、variant 或 classpath 错误 | 由 B2 处理；本阶段禁止修复 |
| 127 键冻结表审核 | COMPLETE | 127 个唯一 ID/规范码/key name/SDL 值；源码逐行匹配；零碰撞；SHA-256 固定 | 真实 26.2 应用层证据仍待提供 |
| B1 集中式兼容层 | COMPLETE（单元范围） | 247 个键盘身份、8 个鼠标、unknown、F25、未分配扩展和边界自检通过 | 尚未接入 GUI/provider/快照；这是 B2，不在本次授权内 |
| version 1 合成夹具 | COMPLETE | 合成 JSON 可解析；schema 仍为 version 1；既有、扩展、未知扩展值均有输入样本 | 不能替代真实 26.2 导出与重启往返 |
| 26.2 真实行为资产 | PARTIAL | 已有 26.2 build/runClient 与单值探针；采集步骤已写入 `BASELINE-ASSETS.md` | 阻碍 B2/C/V 的最终行为验收，不阻碍本次 A/B1 检查点 |

## 2. Git 检查点

| 提交 | 内容 | 可回滚边界 |
| --- | --- | --- |
| `d62692f62d5a778b584565064e40cafa0ba1c729` | `build: target Minecraft 26.3 dependencies` | 只含 4 个构建/元数据文件，不含 Java 功能代码 |
| `b88ce4b217449e48b8409a97d7be6e698dc272fe` | `feat: add canonical input code compatibility layer` | 只含集中式兼容层与可重复生成脚本，不改现有调用点 |
| `0f811dd4c743939f858111763e4f52fe6877f3d9` | `test: add keycode compatibility fixtures` | 独立自检、运行脚本和去标识化夹具；不打包进生产 JAR |

未执行 push、merge、tag 或发布。提交使用单次 `Codex <codex@localhost>` 身份，没有修改仓库或全局 Git 配置。

## 3. Phase 3 A 修改清单

- `build.gradle`
  - Loom 从可变 `1.17-SNAPSHOT` 固定到 `1.19.3`。
  - MaLiLib 两处坐标改为官方可解析的 `maven.modrinth:malilib:${project.malilib_version}`；保留 `compileOnly`/`runtimeOnly` 语义。
- `gradle.properties`
  - Minecraft `26.3`、Loader `0.20.1`、Fabric API `0.165.0+26.3`。
  - MaLiLib `0.30.2`、CommandKeys `3.104.1+26.3`、Mod Menu `21.0.0`。
- `gradle/wrapper/gradle-wrapper.properties`
  - Gradle 固定为 `9.8.2-bin`。
- `src/main/resources/fabric.mod.json`
  - Minecraft 依赖范围从 `~26.2` 改为 `~26.3`；Loader 下限继续由占位符展开。

没有本地文件依赖、仓库外 init script、`flatDir` 或个人缓存路径进入项目配置。

## 4. Gradle 命令与结果

所有命令显式使用 GraalVM JDK `25.0.4`。Codex Windows 子进程按 Phase 2.6 证据设置独立 `GRADLE_USER_HOME` 与 `-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp`；这些值没有写入项目。

| 命令 | 结果 |
| --- | --- |
| `gradlew --version` | 成功；Gradle 9.8.2，Launcher JVM 25.0.4，Daemon JVM 指向 GraalVM 25.0.4 |
| `gradlew help --no-daemon --console=plain --stacktrace` | 成功；Loom 1.19.3，`BUILD SUCCESSFUL` |
| `gradlew dependencies --configuration runtimeClasspath --no-daemon --console=plain` | 成功；目标版本全部解析；Mod Menu 的 Loader 0.19.5 传递请求被项目 0.20.1 正常提升 |
| `gradlew clean build --no-daemon --console=plain` | 到达 `compileJava`；预期失败，共 8 个源码错误 |
| `gradlew processResources --no-daemon --console=plain` | 成功；展开后的 `fabric.mod.json` 值正确 |
| B1 后再次 `gradlew clean build` | 仍为相同 8 个错误；B1 没有新增编译错误 |

首次在受限进程中运行 `processResources` 时，Loom 缓存目录出现 `AccessDeniedException`/前进程 lock owner 已丢失；允许访问已建立的外部缓存后，Loom 检测 `ACQUIRED_PREVIOUS_OWNER_DISOWNED`、重建缓存并成功。该错误不来自项目依赖或源码，也未通过修改项目规避。

8 个源码错误分类：

- `VisualKeymap.java`：GLFW package/`GLFW_KEY_UNKNOWN` 不存在，2 个；
- `VisualKeymapScreen.java`：tinyfd package/两个调用不存在，3 个；
- `KeyBinding.java`、`ImportPreviewScreen.java`、`KeymapSnapshot.java`：`InputConstants.Type.KEYSYM` 不存在，各 1 个。

这些都是 `MIGRATION-PLAN.md` 已识别的 B2/API 适配项。A 阶段未修改它们。

## 5. B1 编码表与实现证据

### 5.1 冻结表审核

审核对象为 `evidence/proposed-26.3-extension-map.tsv` 和本地 26.3 `InputConstants.java`：

- 表行数 127；四列唯一数均为 127；ID 精确为 `1..127`。
- 规范码逐行满足 `0x01000000 | extensionId`，实际为 `0x01000001..0x0100007F`。
- 26.2 legacy 记录 121 行（含 unknown 与 F25）；26.3 keyboard 注册 247 行（含 unknown）。
- 26.3-only 差集恰好 127 行，名称集合与冻结表完全相等。
- 每行 `SdlValue` 与 26.3 注册源码一致，错配数 0。
- 与 legacy 值及鼠标 `0..7` 的碰撞数 0。
- F25 legacy 值仍为 `314`，且目标注册表不存在 F25。
- TSV SHA-256：`0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28`。

### 5.2 集中式层

`src/main/java/dev/xef2/visualkeymap/input/CanonicalInputCode.java` 是本阶段唯一新增生产类：

- 以稳定 key name 对 26.2 规范值和 127 个扩展值编码；不接受 SDL keyboard 整数。
- 显式映射原生鼠标 `1,3,2,4..8` 到公开 `0..7`，避免右/中键交换。
- unknown 返回空编码，维持空列表语义。
- F25 `314` 解码为 `UNAVAILABLE`，不能应用到 26.3 live binding。
- 未分配 namespace 值返回 `UNSUPPORTED_EXTENSION`；其他未知整数返回 `UNSUPPORTED_LEGACY`；两者均 `canApply() == false`。
- 生成脚本只从扩展 TSV 读取 `ExtensionId`、`CanonicalCode`、`KeyName`；`SdlValue` 不进入生成代码。

现有 `KeyBinding#getKeyCodes()`、`VisualKeymapApi`、entrypoint、provider/ID、快照类和 schema 都没有改动。该层尚未被调用；调用点接入属于未授权的 B2/C 单元。

### 5.3 自动化验证

执行：

```powershell
$env:JAVA_HOME='E:\02_Development\05_Environment\graalvm-jdk-25.0.4+7.1'
.\tools\porting-26.3\Generate-CanonicalInputCode.ps1
.\tools\porting-26.3\Test-CanonicalInputCode.ps1
```

结果：

```text
PASS CanonicalInputCodeSelfTest
legacy=121 extension=127 assignedKeyboard=247 mouse=8
extensionTableSha256=0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28
```

另以 `javac --release 25 -Xlint:all` 单独编译兼容层和自检，无错误、无警告。测试覆盖固定表逐行往返、三类唯一性/碰撞、A–E 与鼠标公共码边界、8 个鼠标按钮、unknown、F25、未分配扩展、namespace 边界、负数与 `Integer.MAX_VALUE`。

## 6. version 1 与 26.2 证据边界

已证实：

- 生产 `KeymapSnapshot` schema 没有修改，仍写 `version = 1`。
- Phase 2 的未修改 26.2 runtime 探针证明 `16777217` 经过 `InputConstants`、Gson snapshot、`ImportMatch` 和 mock binding 后原值往返。
- 合成 version 1 夹具覆盖 legacy、mouse、empty、modifier、F25、首尾已知扩展和未知扩展；所有应有效的 JSON 均已解析检查。

尚未证实：

- 26.2 GUI 对扩展值的显示与编辑后行为；
- `options.txt` 重启往返；
- MaLiLib/CommandKeys 真实配置读取、编辑、保存；
- 扩展值在 26.3 经过导入预览、编辑、保存、导出的完整应用链。

后四项需要真实材料及 B2/C 调用点后才能验证。当前 B1 没有静默丢弃路径，但也没有接入应用层，因此不能以单元测试替代端到端结论。

## 7. 26.2 测试资产状态

自动材料已完成并与生产资源分离；真实客户端采集步骤、代表绑定、准确文件路径、MaLiLib/CommandKeys 获取方法、录屏重点和隐私检查见 [`BASELINE-ASSETS.md`](BASELINE-ASSETS.md)。最小仍缺材料：

- 真实 `visualkeymap-26.2-golden.json`；
- 脱敏的 `options.txt` key mapping、`visualkeymap.json`；
- 实际变化的 MaLiLib 所属模组配置；
- 脱敏 `commandkeys.json`；
- 入口、编辑、导入预览、离开/重启往返录屏。

## 8. 结论与停止点

- Phase 3 A：**完成**。依赖组合可复现解析，元数据正确，源码编译反馈已建立。
- Phase 3 B1：**集中式层与单元验证完成；应用层行为验收仍为 PARTIAL**。冻结编码表本身已通过完整静态、碰撞和往返审核；26.2“保留但不可触发”的实际 GUI/provider 安全性仍等待真实材料。
- 后续状态：**CONDITIONAL GO for B2**，条件是维护者明确授权 B2，并接受真实 26.2 材料可在 B2 验收前补齐；缺失材料不得被视为已通过。

本次在 B1 检查点停止。未修改 B2 GUI/provider/快照调用点，未修改 MaLiLib/CommandKeys/Mod Menu 集成，未启动目标客户端，未合并或发布。
