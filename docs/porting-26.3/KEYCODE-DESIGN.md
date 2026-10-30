# Visual Keymap 26.3 扩展键码设计提案

> 状态：Phase 2.6 设计提案，待维护者批准
> 范围：只定义规范编码和兼容行为，不实现输入兼容层
> 证据日期：2026-10-30

## 1. 目标和不变条件

Visual Keymap 的 `KeyBinding#getKeyCodes()`、modifier、冲突判断、排序、筛选和 version 1 快照都共享无类型的 `List<Integer>`。Minecraft 26.3 改用 SDL scancode 后，原生键盘值会与既有鼠标 `0..7` 冲突，因此原生 SDL 数字不能直接进入这些接口。

本提案固定以下不变条件：

- 26.2 已有键盘规范整数逐个保持原义；包括 F25 的 `314`。
- 鼠标仍为 `0..7`，分别表示左、右、中、M4–M8 的既有 Visual Keymap 语义。
- 未绑定继续由空列表表示；不得把 26.3 的 SDL unknown `0` 暴露为公开键码。
- `List<Integer>`、entrypoint、provider/ID 和 version 1 JSON 字段不变。
- SDL scancode 只存在于兼容层的 native boundary；公共接口、GUI 业务、冲突/排序和快照只能看到规范整数。

## 2. 已验证的键集合

通过未修改的 26.2 runtime classpath 反射 `InputConstants.Key.NAME_MAP`，并与 26.3 反编译源码 `InputConstants.Type` 的注册表比较：

| 集合 | 数量 |
| --- | ---: |
| 26.2 键盘名称（含 unknown） | 121 |
| 26.3 键盘名称（含 unknown） | 247 |
| 同名键 | 120 |
| 26.3 新增键 | 127 |
| 26.3 缺少的旧键 | 1：`key.keyboard.f25` |

同名键继续用 26.2 GLFW 整数作为规范值，通过稳定 key name 与 26.3 SDL key 相互映射。F25 保留规范值 `314`，但 26.3 没有对应物理键，必须按“可保留数据、不可伪装为其他键”的 tombstone 处理。

## 3. 具体整数布局

### 3.1 既有区

| 范围/值 | 含义 |
| --- | --- |
| 空 `List<Integer>` | 未绑定 |
| `0..7` | 既有鼠标编码，语义不变 |
| 26.2 `InputConstants.Type.KEYSYM` 的已注册整数 | 既有键盘规范编码，语义不变 |
| `314` | F25 tombstone；不重新分配 |

### 3.2 26.3 扩展键盘区

- 扩展键盘 namespace：`0x01000000..0x0100FFFF`。
- `0x01000000` 保留为无效/未分配标记，不代表任何键。
- 有效编码公式：`canonical = 0x01000000 | extensionId`。
- `extensionId` 有效范围：`1..65535`。
- 首批 127 个 26.3-only 键使用 ID `1..127`，因此实际范围为：
  - 最小：`0x01000001`，十进制 `16777217`；
  - 最大：`0x0100007F`，十进制 `16777343`。
- `0x01000100..0x0100FFFF` 暂不分配，供未来按相同规则追加。
- `0x01010000..0x01FFFFFF` 保留，不在本次定义任何语义。

首批 ID 由“26.3 键名集合减去 26.2 键名集合”后按 key name 字典序生成一次，并立即冻结。以后新增键只能使用当前最大 ID 后的新 ID；删除或改名的键保留 tombstone/alias，ID 永不回收、永不因重新排序而改变。映射依据是稳定 key name，不是 SDL 数值或注册顺序。

完整首批表保存在 [`evidence/proposed-26.3-extension-map.tsv`](evidence/proposed-26.3-extension-map.tsv)。生成结果为 127 个唯一 ID、127 个唯一规范整数，与 121 个 26.2 键盘值及鼠标 `0..7` 的交集均为空。

## 4. 编解码规则

### 4.1 26.3 native key → 规范整数

1. `Type.MOUSE`：按按钮语义映射到既有 `0..7`，不得用统一数值偏移推导右键/中键。
2. `Type.KEYBOARD` 且 key name 存在于 26.2 表：返回该名称的 26.2 GLFW 规范整数。
3. `Type.KEYBOARD` 且 key name 存在于冻结扩展表：返回 `0x01000000 | extensionId`。
4. unknown：返回“未绑定”，即业务层空列表，不返回 `0`。
5. 不在任何表中的 native key：不得把 SDL 值回退到公共接口；停止写入并记录明确的 unsupported-key 诊断。

### 4.2 规范整数 → 26.3 native key

1. `0..7`：按显式鼠标表恢复 26.3 mouse button。
2. 既有 26.2 键盘整数：先查 26.2 value→key name，再以 key name 查找 26.3 key；不能把 GLFW 整数直接交给 `Type.KEYBOARD.getOrCreate()`。
3. 已分配扩展整数：由 extension ID 查冻结 key name，再查 26.3 key。
4. F25 `314`：返回“目标端无物理映射”，保留 canonical 数据；不得映射到 SDL 314 或其他键。
5. 位于扩展 namespace 但未分配的值：视为未知扩展码，不修改当前 live binding，不按 payload 构造 SDL key。
6. 其他未知旧整数：保持 26.2 version 1 的旧读取路径；在兼容层之外不得扩大解释范围。若不能安全生成 native key，导入预览应标记不可应用，而不是覆盖为 unknown。

## 5. version 1 快照策略

### 5.1 写入

- schema 仍为 `version: 1`，字段名和已有值不变。
- 既有键写既有规范整数；鼠标写 `0..7`；26.3-only 键写已分配扩展整数。
- 不写原始 SDL 值。
- 未知/未分配扩展键不得静默写成另一个键；应停止该条写入并给出诊断。

### 5.2 26.3 读取

- 旧 version 1 文件按原规则读取；所有既有整数得到与 26.2 相同的键。
- 已知扩展整数按冻结表恢复 26.3 key。
- 未知扩展整数不应用到 live binding，避免数据破坏；预览需要保留原整数并显示“不支持的扩展键码”。
- `modifiers` 的现有忽略行为不在本迁移中改变。

### 5.3 26.2 读取扩展整数

已在未修改的 26.2 runtime 上动态验证 `16777217`：

- `InputConstants.Type.KEYSYM.getOrCreate(16777217)` 创建 `key.keyboard.16777217`；
- `InputConstants.getKey("key.keyboard.16777217")` 恢复同一整数；
- `KeymapSnapshot` version 1 JSON 写入/读取保持 `16777217`；
- `ImportMatch#getKeysFromEntry()` 将它作为 `KEYSYM`，测试 binding 写回后仍为 `16777217`。

因此旧版不会把扩展值误认成鼠标，也不会在核心 JSON/Key 对象路径中截断或改写。预期安全行为是：数据可保留，但 26.2 的 GLFW 不会产生该高位 keycode，所以绑定不可物理触发；旧 UI 可能显示 `key.keyboard.16777217` 一类 fallback 名称。

尚未完成的动态验证：26.2 GUI 导入预览显示、Minecraft `options.txt` 完整重启往返、MaLiLib/CommandKeys 真实配置往返。源码/字节码表明这些 provider 对正整数不会执行 MaLiLib 的负数鼠标偏移，但在取得真实客户端夹具前仍标为待确认。

## 6. 碰撞和稳定性检查

B1 实现前后必须自动执行：

1. 旧键 value→name 和 name→value 均为单射；旧表固定为 26.2 golden 数据。
2. 扩展 ID、规范整数、key name 三列各自唯一。
3. 扩展整数全部位于 `0x01000001..0x0100FFFF`。
4. 扩展整数与鼠标 `0..7`、全部旧键盘整数、unknown/tombstone 集合无交集。
5. 127 个首批键逐个满足 native→canonical→native 相等。
6. 对所有共同键逐个满足 26.2 canonical→26.3 native→canonical 相等。
7. SDL A–E 的 `4..8` 与鼠标规范 `4..7` 在 canonical 层不相等。
8. 未分配扩展值、namespace 边界、`Integer.MAX_VALUE` 和负值不会被解释为已知扩展键。
9. 映射表以 golden file 固定；新增项只能追加，删除项只能 tombstone，CI 检查既有行不变。
10. version 1 golden files覆盖旧键、鼠标、F25、已知扩展和未知扩展。

## 7. public API 和第三方 provider

`KeyBinding#getKeyCodes()` 与 `getModifierKeyCodes()` 继续返回规范整数。现有方法签名不变。为降低外部 provider 误用 SDL 值的风险，Phase 3 可增加非破坏性的公共转换 helper，但不能增加新的抽象方法或要求旧实现改变类层次。

内置 Minecraft、MaLiLib、CommandKeys adapter 必须在自己的 native boundary 调用统一转换层。外部 26.3 provider 若直接返回 `InputConstants.Key#getValue()`，会泄漏 SDL 值；迁移文档必须明确禁止这种写法，并提供 helper/示例。该限制无法从无类型 `List<Integer>` 自动推断，因此 API-01/P6 是必需验收项。

## 8. 待维护者批准的决策

本提案请求批准：

1. 使用 `0x01000000` namespace 和首批 `1..127` 的冻结 ID 表。
2. version 1 继续承载扩展整数，不升级 schema。
3. 旧 26.2 的安全策略为“保留但不可触发”，不把新增键替换成近似键。
4. 未知扩展值在 26.3 中不应用到 live binding，并在预览中明确显示不支持。
5. F25 `314` 作为数据 tombstone 保留，绝不映射到无关 SDL 键；物理触发能力单独标记为上游限制。

批准前不得实现 B1 输入兼容层。
