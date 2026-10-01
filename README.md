# MeowClient R6

基于 Minecraft 1.8.9 MCP、使用 Java 8 开发的 PVP 客户端。当前注册 **38 个功能模块**，分为战斗、移动、玩家、视觉、世界、其他、信息与界面八类。

## 界面与操作

ClickGUI 提供「窗口式」和「面板式」两种布局，共用樱粉、暮紫、奶霜配色。左下角的圆形头像取自当前玩家皮肤；点击头像进入 **ClientSetting**。其中的 **Chinese** 开关会立即切换客户端功能、分类、参数、模式、HUD 列表、通知与命令反馈的显示语言。底部的圆形画笔按钮打开 HUD 编辑器，键盘按钮打开按键管理页。客户端名始终保留 **MeowClient**，内部模块名和配置键也不随语言切换。

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
| 视觉 | ESP、NameTag、FullBright、Zoom、Tracers、Breadcrumbs、JumpEffect、HitParticles、DamageParticles、Trajectories、TNTTimer、DamageIndicator |
| 世界 | AutoTool、Eagle、FastPlace、TimeChanger |
| 其他 | AutoGG、Blatant |
| 信息 | ArrayList、InfoHUD、TargetHUD、ArmorHUD、Tab、Logo |
| 界面 | ClickGUI |

**Blatant 默认关闭。** 它控制 KillAura、AutoClicker、NoClickDelay、Speed、NoSlow、NoJumpDelay、Eagle、FastPlace、AutoTool、Derp、SkinDerp、Twerk 的启用权限。关闭 Blatant 会立即关闭正在运行的上述模块。视觉和 HUD 模块、Sprint、AutoGG 不受此开关影响。

AutoClicker 旧的 “Blatant” 参数现显示为 **BlockHit**；旧配置仍可读取。模块设置保存在 `MeowClient/config/Mod.json`，Chinese 开关与 HUD 布局保存在 `MeowClient/config/Client.json`，路径相对于 Minecraft 数据目录。

## R6 新功能

- **Trajectories / 弹道预览**：显示弓箭、末影珍珠、雪球和鸡蛋的预计轨迹，考虑重力、空气/水中阻力、方块与实体碰撞，并标出预计落点。持弓未蓄力时默认预览满弦；开始蓄力后按实际蓄力时间计算。
- **HUD 编辑器**：在 ClientSetting 点击圆形画笔按钮，编辑 Logo、InfoHUD、ArrayList、ArmorHUD、TargetHUD 与 Tab。左键拖动，滚轮缩放，右键切换显示；Tab 切换选中组件，方向键微调，Shift 吸附到 5 像素间距，Delete 重置选中组件。布局和缩放随 Client.json 保存，位置按可用屏幕空间适配分辨率；点击「完成」或 Esc 返回设置页。
- **TNTTimer / TNT 倒计时**：在附近已点燃的 TNT 上方显示剩余秒数，最后一秒改变颜色；以客户端的引信 tick、每秒 20 tick 换算。
- **按键管理**：在 ClientSetting 点击键盘按钮，搜索功能并查看绑定。左键功能行后按键完成绑定，Esc 取消捕获，Delete/Backspace 或右键清除绑定；点击右侧触发方式切换 Toggle、Hold、Smart。共用按键会标红，并在悬停时列出冲突功能。
- **DamageIndicator / 受击方向**：在准星周围显示渐隐的来源箭头。优先使用已知攻击者或接近玩家的箭矢来源；「推断近战来源」会根据附近挥手且面向玩家的实体判断方向，推断提示亮度较低。未找到来源时不绘制方向。

三个新增视觉模块均不受 Blatant 限制。弹道是原版中心轨迹的预测，实际投射物包含随机散布；受击方向受 1.8.9 客户端可收到的信息限制。

## R6 视觉修复

- 移除 HUD Logo、ClickGUI 标题和客户端设置标题中的猫爪，收紧标题留白。
- **Breadcrumbs** 按游戏 tick 记录移动轨迹，高帧率不会缩短长度；轨迹改为带柔光的色带，新增宽度与彩虹色选项。默认长度 100、淡出时间 4000 ms、宽度 0.3；关闭彩虹色后使用自选颜色。
- **JumpEffect** 在实际起跳位置生成分层扩散圆环，走下台阶或跌落不会误触发。
- **HitParticles** 由实际攻击事件触发，一次攻击生成设置数量的粒子；支持原有六种效果，在原版粒子设为最少时也能显示。
- **Tracers** 从屏幕中心连接目标，适配第三人称和视角晃动，新增线条粗细选项。默认只追踪其他玩家；单人测试时可打开「动物」或「怪物」。
- 修复 **ESP** 的 2D 投影污染和清空世界深度缓冲的问题，恢复各视觉模块的绘制状态，避免同时开启时互相影响。
- **DamageParticles** 由攻击事件触发；显示已观察到的生命值差值，无法确认时以 `~` 标注武器伤害估算值。
- **FullBright** 正确恢复原亮度，夜视模式接入光照与雾颜色计算；不会向玩家添加药水效果。

轨迹、跳跃圆环和伤害数字会在关闭模块、切换世界或更换玩家实体后清除。

## 开发环境

- JDK 8
- IntelliJ IDEA
- Minecraft 1.8.9 MCP 源码与游戏 assets
- 仓库 `lib/` 下的 JAR 依赖

1. 将匹配 R4 的 MCP 源码放入 `src/net/minecraft`。该目录及本地游戏运行目录 `jars/` 被 Git 忽略。
2. **从干净的 R4 MCP 源码开始**，依次执行 `git apply patches/r5-mcp-hooks.patch` 与 `git apply patches/r6-mcp-hooks.patch`。如果源码已经是 R5，只应用 R6 补丁。当前开发工作区中的钩子已经应用，无需重复执行。
3. 在 IntelliJ 中把 `src/`、`resources/`、`test/` 分别设为源码、资源、测试源码目录，SDK 设为 JDK 8，并把 `lib/` 的 JAR 加入类路径。
4. 准备 Minecraft assets 后运行 `test/Start.java`。

在仓库根目录的 PowerShell 中运行完整编译与回归检查。脚本优先使用 `JAVA_HOME`，也支持显式指定 JDK 8 路径：

```powershell
.\test\Run-R6Checks.ps1
# 加上离屏 OpenGL 检查（需要 LWJGL natives 与可用显卡驱动，无需启动游戏窗口）
.\test\Run-R6Checks.ps1 -Render
# 如自动查找不到开发环境，可显式指定路径
.\test\Run-R6Checks.ps1 -JdkPath 'C:\Program Files\Java\jdk1.8.0_202' -Render -NativePath 'jars\versions\1.8.8\1.8.8-natives'
```

离屏检查覆盖轨迹长度与淡出、跳跃圆环、攻击粒子、追踪线、ESP 投影与深度缓冲、姓名标签、伤害数字、夜视光照及新增功能。画笔入口、HUD 拖动、按键捕获与清除使用真实 GUI 方法检查，新页面与 TNT 倒计时使用真实字体绘制；预览图输出到 `out/r6-checks/render-previews/`。联机环境的最终效果仍需进游戏测试。

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
| `patches/r6-mcp-hooks.patch` | 在 R5 基础上补充攻击、跳跃、夜视及 HUD 布局钩子 |
| `src/cn/sux1ng/client/util/RenderState.java` | 视觉效果使用的矩阵与 OpenGL 状态恢复 |

接收包事件在 Netty 线程同步触发；监听器如需修改世界或 GUI，需自行安排到 Minecraft 主线程执行。
