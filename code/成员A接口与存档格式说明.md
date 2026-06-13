# 成员A接口与存档格式说明

## 负责范围

成员A已实现模型层、业务逻辑层、文件存档层和核心工具类：

- `happyfarm.model`：`FarmObject`、`Animal`、`Plant`、`ToolItem`
- `happyfarm.core`：`Farm`、`FarmResult`
- `happyfarm.io`：`SaveManager`
- `happyfarm.util`：`SortUtil`、`FarmUtils`
- `happyfarm.MemberATest`：控制台验证入口

## GUI 对接方式

GUI 不直接修改 `Farm` 内部容器，应调用以下方法：

```java
Farm farm = new Farm(3);
farm.addObject(0, 0, new Animal("小牛", 2));
farm.queryByName("牛");
farm.careObject(0, 0);
farm.removeObject(0, 0);
farm.clearFarm();
farm.getRows();
farm.getAllObjects();
```

`rowIndex` 是内部下标，从 `0` 开始；存档文件中的行号从 `1` 开始；位置统一从 `0` 开始。

## 存档格式

固定使用竖线分隔：

```text
ROWS|行数
ITEM|行号|位置|类型|名称|优先级|状态|照料次数
```

示例：

```text
ROWS|3
ITEM|1|0|ANIMAL|小牛|2|开心|1
ITEM|1|1|PLANT|玉米|1|成长中|0
ITEM|2|0|TOOL|水壶|3|可用|0
```

类型统一为 `ANIMAL`、`PLANT`、`TOOL`。名称和状态中的 `|`、换行、百分号会在保存时转义，读取时自动还原。格式错误会由 `SaveManager.loadFarm` 抛出 `IOException`，GUI 捕获后显示错误信息即可。

## 编译和运行验证

在项目根目录执行：

```bash
javac -encoding UTF-8 -d code/out $(find code/src -name '*.java')
java -cp code/out happyfarm.MemberATest
```

运行后会在 `code/saves` 下生成两个测试存档：`member-a-demo.txt` 和 `member-a-loaded-copy.txt`。
