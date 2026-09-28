# MeowClient R5

基于 Minecraft 1.8.9 MCP、使用 Java 8 开发的 PVP 客户端。当前注册 **35 个功能模块**，分为战斗、移动、玩家、视觉、世界、其他、信息与界面八类。

## 界面与操作

ClickGUI 提供「窗口式」和「面板式」两种布局，共用樱粉、暮紫、奶霜配色。左下角的圆形头像取自当前玩家皮肤；点击头像进入 **ClientSetting**。其中的 **Chinese** 开关会立即切换客户端功能、分类、参数、模式、HUD 列表、通知与命令反馈的显示语言。客户端名始终保留 **MeowClient**，内部模块名和配置键也不随语言切换。

| 操作 | 效果 |
| --- | --- |
| 右 Shift | 打开 ClickGUI |
| 窗口式：左键功能 | 开启或关闭 |
| 窗口式：右键功能 | 查看设置 |
| 面板式：左键功能 | 开启或关闭 |
| 面板式：右键功能 | 展开设置 |
| 面板式：中键功能 | 绑定按键 |
| 面板式：滚轮 | 滚动当前设置区域 |
| 面板式：顶部滚轮或 Shift + 滚轮 | 横向浏览分类 |
| 点击文本参数 | 输入内容；Enter 或 Esc 结束 |
| 点击颜色参数 | 左键调整色相，右键调整饱和度，中键调整亮度，Shift + 左键调整透明度 |
| 点击绑定模式 | 在 Toggle、Hold、Smart 间切换 |

默认按键：**C** 按住缩放，**V** 按住疾跑，**X** 按住边缘潜行。Smart 模式短按切换状态，长按时临时启用。

## 功能模块

| 分类 | 模块内部名 |
| --- | --- |
| 战斗 | AutoClicker、KillAura、ClickSound、NoClickDelay |
| 移动 | Sprint、Speed、NoSlow、NoJumpDelay |
| 玩家 | CustomCape、Animations、Derp、SkinDerp、Twerk |
| 视觉 | ESP、NameTag、FullBright、Zoom、Tracers、Breadcrumbs、JumpEffect、HitParticles、DamageParticles |
| 世界 | AutoTool、Eagle、FastPlace、TimeChanger |
| 其他 | AutoGG、Blatant |
| 信息 | ArrayList、InfoHUD、TargetHUD、ArmorHUD、Tab、Logo |
| 界面 | ClickGUI |

**Blatant 默认关闭。** 它控制 KillAura、AutoClicker、NoClickDelay、Speed、NoSlow、NoJumpDelay、Eagle、FastPlace、AutoTool、Derp、SkinDerp、Twerk 的启用权限。关闭 Blatant 会立即关闭正在运行的上述模块。视觉和 HUD 模块、Sprint、AutoGG 不受此开关影响。

AutoClicker 旧的 “Blatant” 参数现显示为 **BlockHit**；旧配置仍可读取。模块设置保存在 `MeowClient/config/Mod.json`，Chinese 开关保存在 `MeowClient/config/Client.json`，路径相对于 Minecraft 数据目录。

## 开发环境

- JDK 8
- IntelliJ IDEA
- Minecraft 1.8.9 MCP 源码与游戏 assets
- 仓库 `lib/` 下的 JAR 依赖

1. 将匹配 R4 的 MCP 源码放入 `src/net/minecraft`。该目录及本地游戏运行目录 `jars/` 被 Git 忽略。
2. **仅在干净的 R4 MCP 源码上**执行 `git apply patches/r5-mcp-hooks.patch`。当前开发工作区中的钩子已经应用，无需重复执行。
3. 在 IntelliJ 中把 `src/`、`resources/`、`test/` 分别设为源码、资源、测试源码目录，SDK 设为 JDK 8，并把 `lib/` 的 JAR 加入类路径。
4. 准备 Minecraft assets 后运行 `test/Start.java`。

在仓库根目录、JDK 8 已加入 `PATH` 的 PowerShell 中运行无窗口回归检查：

```powershell
New-Item -ItemType Directory -Force out/r5 | Out-Null
javac -encoding UTF-8 -source 8 -target 8 -sourcepath src -cp 'lib/*' -d out/r5 test/R5RegressionTest.java test/ClientUiRegressionTest.java
java -cp 'out/r5;lib/*' R5RegressionTest
java -cp 'out/r5;lib/*' ClientUiRegressionTest
```

## 代码入口

| 位置 | 作用 |
| --- | --- |
| `src/cn/sux1ng/client/MeowClient.java` | 启动、关闭及管理器初始化 |
| `src/cn/sux1ng/client/mod/` | 模块注册、按键模式及 Blatant 启用规则 |
| `src/cn/sux1ng/client/events/` | 同步事件分发 |
| `src/cn/sux1ng/client/gui/` | 两套 ClickGUI、圆形皮肤头像与 ClientSetting |
| `src/cn/sux1ng/client/ui/` | 主题、人工中文译名与通知 |
| `src/cn/sux1ng/client/config/` | 模块及客户端设置保存 |
| `patches/r5-mcp-hooks.patch` | 被忽略的 MCP 源码中的事件与渲染钩子改动 |

接收包事件在 Netty 线程同步触发；监听器如需修改世界或 GUI，需自行安排到 Minecraft 主线程执行。
