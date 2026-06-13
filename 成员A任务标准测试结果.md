# 成员A任务标准测试结果

测试时间：2026-06-12 17:49:53 CST  
测试范围：`/Users/jakie/Project/Happyfarm/code`  
测试结论：通过。成员A已完成模型层、业务逻辑层、文件存档层、排序/工具类层、接口说明和控制台测试交付物，现有内容符合《图形化农场对象管理系统两人开发分工方案》中成员A的任务标准。

## 1. 验收依据

依据文件：`图形化农场对象管理系统两人开发分工方案.md`

成员A标准包括：

- 模型层：`FarmObject`、`Animal`、`Plant`、`ToolItem`，实现抽象照料、多态、保存文本、`toString()`、`equals()`、`hashCode()`。
- 业务逻辑层：`Farm`、`FarmResult`，实现初始化、添加、查询、照料、删除、清空、获取全部对象，并在越界时返回失败结果。
- 文件存档层：`SaveManager`，实现扫描 `.txt`、保存、读取、解析对象，格式错误时抛出异常。
- 排序和工具类：`SortUtil`、`FarmUtils`，实现类别排序、优先级排序、分组、格式化、输入校验、随机数、类型和存档字段处理。
- 交付物：后端核心类源码、存档格式说明、稳定 GUI 调用接口、简单控制台测试结果、至少 2 个 txt 测试存档。

## 2. 静态检查结果

已确认以下成员A负责文件存在：

```text
code/src/happyfarm/model/FarmObject.java
code/src/happyfarm/model/Animal.java
code/src/happyfarm/model/Plant.java
code/src/happyfarm/model/ToolItem.java
code/src/happyfarm/core/Farm.java
code/src/happyfarm/core/FarmResult.java
code/src/happyfarm/io/SaveManager.java
code/src/happyfarm/util/SortUtil.java
code/src/happyfarm/util/FarmUtils.java
```

辅助交付物也存在：

```text
code/src/happyfarm/MemberATest.java
code/成员A接口与存档格式说明.md
code/saves/member-a-demo.txt
code/saves/member-a-loaded-copy.txt
```

源码规模：

```text
121 code/src/happyfarm/core/Farm.java
39  code/src/happyfarm/core/FarmResult.java
90  code/src/happyfarm/util/FarmUtils.java
70  code/src/happyfarm/util/SortUtil.java
188 code/src/happyfarm/io/SaveManager.java
119 code/src/happyfarm/model/FarmObject.java
40  code/src/happyfarm/model/Plant.java
40  code/src/happyfarm/model/ToolItem.java
40  code/src/happyfarm/model/Animal.java
41  code/src/happyfarm/MemberATest.java
```

## 3. 编译测试

环境：

```text
openjdk version "17.0.19" 2026-04-21
javac 17.0.19
```

执行命令：

```bash
mkdir -p code/test-out && javac -encoding UTF-8 -d code/test-out $(find code/src -name '*.java')
```

结果：编译通过，无错误输出。

## 4. 成员A演示测试

执行命令：

```bash
java -cp code/test-out happyfarm.MemberATest
```

关键输出：

```text
添加成功：第 1 行第 0 位 -> 动物-小牛(P2)
添加成功：第 1 行第 1 位 -> 植物-玉米(P1)
添加成功：第 2 行第 0 位 -> 工具-水壶(P3)
已喂养动物 小牛，心情 +4，当前状态：平静
查询“牛”结果数量：1
按类别排序：
1. 动物 小牛 | 类型:ANIMAL | 优先级:2 | 状态:平静 | 照料次数:1 | 心情:54
2. 植物 玉米 | 类型:PLANT | 优先级:1 | 状态:成长中 | 照料次数:0 | 成长值:0
3. 工具 水壶 | 类型:TOOL | 优先级:3 | 状态:可用 | 照料次数:0 | 耐久度:80
按优先级排序：
1. 工具 水壶 | 类型:TOOL | 优先级:3 | 状态:可用 | 照料次数:0 | 耐久度:80
2. 动物 小牛 | 类型:ANIMAL | 优先级:2 | 状态:平静 | 照料次数:1 | 心情:54
3. 植物 玉米 | 类型:PLANT | 优先级:1 | 状态:成长中 | 照料次数:0 | 成长值:0
已保存：/Users/jakie/Project/Happyfarm/code/saves/member-a-demo.txt
读取后再次保存：/Users/jakie/Project/Happyfarm/code/saves/member-a-loaded-copy.txt
当前存档数量：2
```

说明：`care()` 中心情变化使用随机数，所以不同运行中 `+4`、`心情:54` 这类数值可能不同；不影响功能结论。

## 5. 存档文件验证

`code/saves/member-a-demo.txt` 内容：

```text
ROWS|3
ITEM|1|0|ANIMAL|小牛|2|平静|1
ITEM|1|1|PLANT|玉米|1|成长中|0
ITEM|2|0|TOOL|水壶|3|可用|0
```

`code/saves/member-a-loaded-copy.txt` 内容：

```text
ROWS|3
ITEM|1|0|ANIMAL|小牛|2|平静|1
ITEM|1|1|PLANT|玉米|1|成长中|0
ITEM|2|0|TOOL|水壶|3|可用|0
```

结果：读取后再次保存的内容与原始保存内容一致，满足成员A对 txt 保存/读取/恢复对象的要求。

## 6. 补充边界测试

除 `MemberATest` 外，额外使用临时测试程序覆盖以下风险点：

- 添加负行号返回失败。
- 空行位置越界添加返回失败。
- 正常添加对象成功。
- 照料空位置返回失败。
- 删除空位置返回失败。
- 清空后保留原行数。
- 清空后对象列表为空。
- 优先级排序从高到低。
- 排序不改变农场原始位置。
- 照料后的对象状态可保存。
- 保存读取后行数一致。
- 名称包含 `|` 和 `%` 时可转义并还原。
- 照料次数保存读取一致。
- 扫描存档只返回 `.txt` 文件。
- 未知对象类型存档会抛出可读异常。

执行结果：

```text
[PASS] 添加负行号返回失败
[PASS] 空行位置 1 添加返回失败
[PASS] 正常添加植物成功
[PASS] 照料空位置返回失败
[PASS] 删除空位置返回失败
[PASS] 清空返回成功
[PASS] 清空后保留原行数
[PASS] 清空后对象列表为空
[PASS] 优先级排序从高到低
[PASS] 排序不改变农场原位置
[PASS] 照料后对象状态可保存
[PASS] 读取后行数一致
[PASS] 名称转义后可还原
[PASS] 照料次数保存读取一致
[PASS] 扫描仅返回 txt 存档
[PASS] 未知类型存档抛出可读异常
补充测试通过：16
补充测试失败：0
```

## 7. 分项结论

| 分项 | 结果 | 说明 |
|---|---|---|
| 模型层 | 通过 | 三类对象存在，均继承 `FarmObject`，`care()` 行为有差异，支持展示和保存文本。 |
| 业务逻辑层 | 通过 | 增删查照料清空和对象列表获取均可用，越界场景返回失败结果而非崩溃。 |
| 文件存档层 | 通过 | 可扫描、保存、读取、恢复三类对象，错误存档可抛出异常。 |
| 排序和工具类 | 通过 | 类别排序、优先级排序、分组、格式化、输入校验、随机数、转义/反转义均存在并通过测试。 |
| GUI 对接稳定性 | 通过 | 成员A接口文档存在，`Farm` 不依赖 Swing，GUI 可通过公开方法调用。 |
| 控制台测试交付 | 通过 | `MemberATest` 可独立运行并生成两个测试存档。 |

## 8. 注意点

- 本次只验证成员A负责范围；`Main`、`FarmGUI`、Swing 事件、多线程调度属于成员B任务，不在本报告结论内。
- `FarmObject` 构造层允许优先级为 `0`，但 `SaveManager.loadFarm()` 解析存档时使用正整数校验，优先级 `0` 的存档会读取失败。当前测试和接口文档都使用正优先级，因此不影响本次成员A验收；建议 GUI 侧也限制优先级必须大于 0，或者后续统一改成非负整数。

## 9. 总结

成员A任务标准测试通过。当前 `code` 目录下的成员A实现可以作为成员B GUI 集成的稳定后端基础。
