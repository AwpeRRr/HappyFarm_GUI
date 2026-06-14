# 快乐农场 HappyFarm_GUI

一个基于 Java Swing 的图形化农场对象管理系统。可以添加、查询、照料、删除农场里的动物、植物和工具，支持存档读写、排序，右侧带 RPG 风格属性卡，底部还有一块会自己溜达的 ASCII 小农场。

## 运行环境

- JDK 8 或更高版本

## 编译

在项目根目录（`HappyFarm_GUI`）执行：

```bash
javac -encoding UTF-8 -d code/out $(find code/src -name '*.java')
```

Windows PowerShell：

```powershell
javac -encoding UTF-8 -d code/out (Get-ChildItem -Recurse code/src -Filter *.java).FullName
```

## 运行

存档目录默认是 `code/saves`（相对项目根目录），所以**必须在项目根目录启动**：

```bash
java -cp code/out happyfarm.Main
```

启动时会扫描 `code/saves`：没有存档进入空界面，恰好一个自动读取，多个则弹框选择。仓库自带 `demo-快乐农场.txt`，首次启动即可看到效果。

## 操作概览

- **左栏**：类型 / 名称 / 优先级 / 行号 / 位置输入，以及初始化、添加、查询、照料、删除、清空、保存、读取、排序等按钮。
- **中栏**：按行展示农场对象，点击对象会回填到左栏并在右上属性卡显示详情。
- **右栏**：上方为属性卡，下方为操作日志。
- **底栏**：ASCII 动态农场，动物心情越好动得越勤。试着把某只满心情的动物再照料一次，看看会发生什么 🌟

## 目录结构

```
code/
├── src/happyfarm/        # 源码（model / core / io / util / gui + Main）
├── saves/                # 存档目录（含 demo 存档）
└── out/                  # 编译产物
```
