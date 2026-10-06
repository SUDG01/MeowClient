# MeowClient R7

基于 Minecraft 1.8.9 MCP、使用 Java 8 开发的 PVP 客户端。当前注册 **42 个功能模块**，分为战斗、移动、玩家、视觉、世界、其他、信息与界面八类。

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
| 战斗 | AutoClicker、KillAura、Aimbot、ClickSound、NoClickDelay |
| 移动 | Sprint、Speed、NoSlow、NoJumpDelay |
| 玩家 | CustomCape、Animations、Derp、SkinDerp、Twerk |
| 视觉 | ESP、NameTag、FullBright、Zoom、Tracers、Breadcrumbs、JumpEffect、HitParticles、DamageParticles、Trajectories、TNTTimer、DamageIndicator |
| 世界 | AutoTool、Eagle、FastPlace、TimeChanger |
| 其他 | AutoGG、Blatant、Target、AntiBot、Disabler |
| 信息 | ArrayList、InfoHUD、TargetHUD、ArmorHUD、Tab、Logo |
| 界面 | ClickGUI |

**Blatant 默认关闭。** 它控制 KillAura、Aimbot、AutoClicker、NoClickDelay、Speed、NoSlow、NoJumpDelay、Disabler、Eagle、FastPlace、AutoTool、Derp、SkinDerp、Twerk 的启用权限。关闭 Blatant 会立即关闭正在运行的上述模块。视觉和 HUD 模块、Sprint、AutoGG 不受此开关影响。

AutoClicker 旧的 “Blatant” 参数现显示为 **BlockHit**；旧配置仍可读取。模块设置保存在 `MeowClient/config/Mod.json`，Chinese 开关与 HUD 布局保存在 `MeowClient/config/Client.json`，路径相对于 Minecraft 数据目录。

## R7 移动、物品减速与 Disabler

Speed 使用本次新读取的输入决定跳跃，再由原版执行起跳；按住跳跃键不会叠加一次自动跳跃。水平速度在实际 MoveEvent 中调整，正常加速度不会再次叠加到目标速度上。关闭模块不会重置其他功能的计时器。默认在使用物品、潜行、液体、梯子、飞行、碰墙时暂停；收到回弹后默认暂停 1000 ms，受击速度与爆炸后暂停 500 ms。

| Speed 模式 | 行为 |
| --- | --- |
| AutoJump | 按移动输入自动起跳，保留原版水平物理 |
| Legit | 仅向前疾跑时自动起跳，保留原版水平物理 |
| Vanilla | 自动跳跃，以「速度」设置控制水平移动量 |
| Ground | 只在地面调整水平速度，保留玩家的手动跳跃 |
| Strafe | 根据输入方向重定向现有水平移动，保留手动跳跃 |
| BHop | 自动连续跳跃，地面增速、空中逐渐调整 |
| LowHop | 使用可调整的较低起跳高度，适合单人测试与允许该行为的环境 |
| NCP | 分阶段的跳跃增速，使用已完成移动的实际距离衔接空中阶段 |
| Grim | 只在地面调整转向及速度，空中保留原版物理 |

「自动疾跑」默认开启，只在向前、食物充足且未使用物品时开始疾跑。Vanilla/Ground 的新配置默认速度为 0.35 格/tick，范围 0.1–2；旧参数名保留，超出新范围的旧值会收敛到范围内。AutoJump 与 Legit 不额外改写水平速度。

NoSlow 在原版物品使用的 0.2 输入乘子处补偿，补偿强度 0 完全保留原版减速，1 恢复完整输入；后续加速度、摩擦与方向处理交给原版。避免了旧实现一边保留慢输入、一边突然放大 motion 的问题。

| NoSlow 模式 | 行为 |
| --- | --- |
| Vanilla | 对所选剑格挡、吃喝、拉弓应用输入补偿 |
| Ground / Air | 分别只在地面或空中补偿 |
| Adaptive | 地面应用设置强度，空中使用一半补偿强度 |
| NCP | 剑格挡时在移动 PRE 释放、POST 恢复使用 |
| SwitchItem | 剑格挡时短暂切换并恢复服务器槽位，POST 恢复使用 |
| Grim | 剑格挡时短暂缓冲移动与攻击包；周期结束后切换槽位、按原顺序释放包并恢复格挡 |

剑格挡发包策略仅处理剑，不会用相同流程重置吃喝和拉弓计时；在 KillAura 接管物品使用时让出发包。Grim 默认缓冲 150 ms，队列上限 64；服务器修正或强制换槽时丢弃旧移动，并暂停补偿 1000 ms。发包策略可能改变服务器格挡的时机，适配结果需结合实际插件版本测试。

**Disabler / 协议调整** 位于 Misc，默认关闭，需先开启 Blatant：

- **Basic** 清理重复的疾跑、潜行与持物槽位状态，保留实际切换。
- **Grim** 在有上限的 FIFO 中延迟窗口 0、负序号的确认回应，默认 150 ms；普通物品栏确认和默认心跳直接通过。
- **NCP** 清理重复状态，合并静止移动报告；保留姿态变化和每 20 tick 一次的静止报告，实际移动与视角包继续发送。
- **Custom** 自行选择确认/心跳延迟、重复状态、静止报告与方块交互兼容。方块交互兼容默认关闭，用等价的原版方向编码尝试适配相关协议检查。

确认队列默认上限 128，可设 16–256，延迟最多 750 ms；超量、超时、物品栏确认、服务器修正、关闭模块时会释放当前连接的确认包。独立截止计时确保游戏 tick 暂时不运行时也能回应；更换连接时不会把旧确认送进新服务器。服务器位置修正始终接收，不使用积压的位置包覆盖回弹。

参考 [Wurst 跳跃处理](https://github.com/Wurst-Imperium/Wurst7/blob/master/src/main/java/net/wurstclient/hacks/BunnyHopHack.java)、[FDP NoSlow](https://github.com/SkidderMC/FDPClient/blob/main/src/main/java/net/ccbluex/liquidbounce/features/module/modules/movement/NoSlow.kt) 与 [FDP GrimBHop](https://github.com/SkidderMC/FDPClient/blob/main/src/main/java/net/ccbluex/liquidbounce/features/module/modules/movement/speedmodes/grim/GrimBHop.kt) 的输入、物品分组与地面/空中处理思路，事件和队列由本项目实现。Grim/NCP 是行为配置名称，不能保证未知版本的服务器检查免 VL；截图中的 SurvivalFly、NoSlow 和 Simulation 最终需要对应环境验证。建议先对照 AutoJump/Legit 的正常移动，再逐项启用需要测试的策略。

## R7 统一目标与 AntiBot

Misc / 其他中的 **Target / 目标筛选** 和 **AntiBot / 机器人过滤** 默认开启，不需要 Blatant。

Target 提供玩家、怪物、友善生物、隐身目标四个开关，默认只选玩家。隐身目标还需要所属类别开启；怪物包括恶魂、史莱姆等，友善生物包括动物、村民、傀儡、蝙蝠和鱿鱼。自己、旁观者和失效世界的实体不会成为目标。

- 开启 Target 后，KillAura、Aimbot、AutoClicker、ESP、NameTag、Tracers、TargetHUD、HitParticles 和 DamageParticles 共用类别筛选。ESP 和姓名标签支持选中的非玩家生物，目标 HUD 也支持 Aimbot 的当前目标。
- KillAura、Aimbot 与 Tracers 中重复的类别选项会隐藏，原有配置键和值保留；关闭 Target 后恢复各模块原来的类别设置。距离、视角、瞄准条件与 Aimbot 的队伍判断仍在对应模块中设置。
- KillAura 在 PRE 与 POST 都检查筛选条件，过程中关闭目标类别会停止待执行攻击。AutoClicker 对被排除的准星实体暂停点击，Aimbot 停止跟随被排除的目标。

AntiBot 默认检查 Tab 玩家列表、玩家档案一致性、重复 UUID/名字，并对新生成的玩家等待 **500 ms** 后再允许自动操作。缺少 Tab 信息或存在身份异常的玩家会暂停自动攻击和瞄准；默认观察 **1000 ms** 后隐藏疑似机器人，可单独关闭「隐藏疑似机器人」以保留视觉提示。正常隐身、零延迟与空中玩家不会仅凭这些状态被排除；「要求落地记录」默认关闭，开启后保留曾经落地的记录，并允许创造模式飞行。

识别状态按游戏 tick 更新，实体移除、切换世界、连接变化或关闭模块时会清理。单人世界跳过服务器机器人检查。Tab 信息补齐或档案恢复后可重新选择该玩家。识别参考 [LiquidBounce AntiBot](https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/misc/antibot/modes/CustomAntiBotMode.kt) 的档案、重复身份与地面记录思路，实现适配本项目的 1.8.9 客户端。

AntiBot 依据客户端可见信息判断；服务器若隐藏真实玩家的 Tab 信息，可关闭「检查玩家列表」。完整模拟正常玩家的机器人可能需要调整检查条件，最终效果需在对应服务器测试。

## R7 战斗事件与操作节奏

KillAura 使用 Motion PRE 选择目标并计算转向，等待移动更新提交后在 POST 重新检查目标、距离、遮挡与实际朝向。每个游戏 tick 最多自动攻击一次；重复事件、失效目标、菜单、暂停和世界切换会清理待执行操作。

| 转向模式 | 行为 |
| --- | --- |
| Lock / 锁定 | 较快跟随目标，使用加速、减速和转向上限，避免直接跳到目标角度 |
| Smooth / 平滑 | 逐渐接近瞄准位置，可调整平滑程度；可见镜头由原版帧间插值衔接 |
| Silent / 静默 | 连续更新移动事件中的朝向，保留上一轮转向状态，镜头由玩家控制 |
| None / 无 | 保留玩家的手动朝向，只对准星射线实际指向的有效目标攻击 |

- **Single** 保持当前有效目标，直到目标失效；**Switch** 在至少一次攻击且达到目标保持时间后按实体身份轮换，减少距离变化造成的频繁切人。
- 新增 **目标反应延迟**、**目标保持时间** 和 **瞄准位置变化**。瞄准位置在目标体积内缓慢变化，转向按当前鼠标灵敏度的角度步长计算。
- 点击间隔在每次攻击后确定并保持，节奏逐渐变化；低帧率或暂停恢复时不会补发积压攻击。手动攻击与自动攻击共享节奏，AutoClicker 在 Aura 已有目标时暂停额外点击。
- 自动格挡通过原版物品使用流程进入，攻击前解除，后续 tick 再恢复。模块只解除自己发起的格挡，手动使用物品时让出操作。

全新配置默认使用 Smooth、120° 视角范围、45°/tick 转向上限、150 ms 反应延迟、600 ms 目标保持时间和 0.12 瞄准变化。已有配置保留模式及参数名；转向上限的新范围为 5–90°/tick，旧值超过上限时会按范围调整。KillAura 仍需要开启 Blatant。

## R7 Aimbot / 瞄准辅助

在「战斗」分类中开启 Aimbot，需先开启 Blatant。默认使用 **Assist / 轻辅助**；**Track / 平滑跟随** 的响应更快。辅助逐帧运行在原版鼠标输入之后、相机计算之前，按实际经过的时间控制转向速度与平滑响应。

- 默认按住攻击键且手持剑或斧时触发，距离 3.6、总视角范围 45°、反应延迟 100 ms；辅助强度 0.65，水平和垂直基础速度分别为 80°/s 与 40°/s。
- 默认瞄准位置为 **Closest / 贴近准星**，寻找目标体积内靠近准星的位置；也可选躯干或头部。准星已穿过目标体积时停止修正，保留玩家已有的瞄准偏差。
- **鼠标操作优先** 默认开启。向目标外拉鼠或快速甩动时立即暂停修正，并让出 120 ms；普通手动输入先于辅助生效。
- 保持当前有效目标，排除队友、隐身目标、旁观者、遮挡目标与视角外目标。默认仅选玩家，可启用怪物、动物。
- 挖方块、使用物品、菜单、暂停、失焦与玩家或世界变化会停止辅助。超过 100 ms 的帧间隔会丢弃旧跟随状态，避免恢复时突然拉动。
- KillAura 已有目标且使用 Lock、Smooth 或 Silent 时让出转向；None 模式可与瞄准辅助组合使用。攻击和物品使用由原有操作流程处理，Aimbot 本身只调整镜头。

条件触发与目标体积判断参考 [Fusion+ AimAssist](https://github.com/h1meji/fusion-plus/blob/main/fusion-plus/src/base/moduleManager/modules/combat/aimAssist.cpp)，平滑控制与事件接入由本项目实现。参数用于调整操作手感，服务器中的实际表现仍需实测。

## R7 弹道与披风修复

- **Trajectories / 弹道预览**：预览方向与当前画面的帧间角度插值保持一致，避免转向时线条偏离可见瞄准方向；发射偏移、方向归一化与浮点计算对齐原版。入水阻力改为移动前采样，修复提前减速造成的落点偏差。预览保留原版手持发射偏移，实际投射物仍包含随机散布。
- **CustomCape / 自定义披风**：Meow、OptiFine、Minecon 样式分别参与选择；关闭模块后恢复原版披风。「其他玩家的 OptiFine 披风」只控制其他玩家，不再覆盖自己的 Meow 样式。
- **Meow** 优先读取游戏数据目录下的 `MeowClient/cape.png`，也支持 `cape.jpg`；没有本地图片时显示内置猫图案。颜色设置调整内置图案的配色。支持原版 64×32 贴图及其倍数，常见的 22×17 披风图会填充到原版 UV 布局。替换纹理会释放旧的 GPU 纹理。
- **OptiFine** 复用同名玩家的下载与贴图，最多并发两个任务；缺失或失败后等待五分钟再重试，成功图片缓存到 `MeowClient/cache/capes/`。原版皮肤读取不会再反复触发额外披风下载，连接失败只记录简短信息。

OptiFine 或 Minecon 样式需要该玩家实际已有的相应披风；暂未加载到 OptiFine 时显示原版披风。自己的披风可用 F5 查看，并需在原版「皮肤自定义」中开启披风显示。

## R6 新功能

- **原始鼠标输入**：ClientSetting 中默认开启 RawInput。Windows 使用独立的鼠标原始输入接收与批量读取，保留硬件相对位移，避免系统加速与窗口边界影响视角；灵敏度、反转鼠标和缩放操作继续使用原版设置。失焦或打开菜单时清空待处理位移，切回游戏时不会重放旧输入。后端不可用或收到绝对坐标输入时回退到标准输入。
- **高回报率事件处理**：扩大 LWJGL 鼠标缓冲；锁定视角时合并处理冗余移动事件，保留按键与滚轮顺序，并在帧开始时获取输入。软件回放覆盖 1k、2k、4k、8k、16k 和 32k Hz，实际设备表现仍需实鼠验证。
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
2. **从干净的 R4 MCP 源码开始**，依次执行 `git apply patches/r5-mcp-hooks.patch`、`git apply patches/r6-mcp-hooks.patch`、`git apply patches/r6-input-hooks.patch`、`git apply patches/r7-aim-hooks.patch`、`git apply patches/r7-visual-hooks.patch` 与 `git apply patches/r7-movement-hooks.patch`。已有 R7 视觉钩子的源码只需应用 R7 移动补丁。当前开发工作区中的钩子已经应用，无需重复执行。
3. 在 IntelliJ 中把 `src/`、`resources/`、`test/` 分别设为源码、资源、测试源码目录，SDK 设为 JDK 8，并把 `lib/` 的 JAR 加入类路径。
4. 准备 Minecraft assets 后运行 `test/Start.java`。

在仓库根目录的 PowerShell 中运行完整编译与回归检查。脚本优先使用 `JAVA_HOME`，也支持显式指定 JDK 8 路径：

```powershell
.\test\Run-R7Checks.ps1
# 加上离屏 OpenGL 检查（需要 LWJGL natives 与可用显卡驱动，无需启动游戏窗口）
.\test\Run-R7Checks.ps1 -Render
# 如自动查找不到开发环境，可显式指定路径
.\test\Run-R7Checks.ps1 -JdkPath 'C:\Program Files\Java\jdk1.8.0_202' -Render -NativePath 'jars\versions\1.8.8\1.8.8-natives'
```

战斗回归使用真实的步行玩家 PRE／移动更新／POST 流程记录发包，覆盖四种转向模式、两种目标模式、攻击顺序、格挡所有权、点击节奏和无效上下文。瞄准辅助回放覆盖 30、60、144、360、1000 FPS、移动目标插值、鼠标让出、目标保持、条件触发及模块冲突；Windows 下的 `-Render` 还调用真实的 EntityRenderer 帧入口验证鼠标输入与相机事件顺序。离屏检查继续覆盖视觉、GUI 与原始鼠标输入；预览图输出到 `out/r7-checks/render-previews/`。联机环境的最终效果仍需进游戏测试。

R7 视觉回归将弹道预览与原版箭的发射和逐 tick 更新对照，覆盖相机插值与入水减速；在本机 HTTP 服务上验证下载去重、两任务并发上限、缺失/失败缓存、恢复重试和磁盘缓存。`-Render` 调用真实 LayerCape 检查默认图案、本地 PNG/JPG、颜色更新、纹理释放和原版恢复，生成 `meow-cape.png` 预览。

目标回归覆盖各实体类别、隐身组合、新玩家观察、Tab 缺失与恢复、重复身份、可选落地记录、世界重置和配置保存；调用真实 KillAura / Aimbot / AutoClicker 处理流程验证假玩家不会触发自动操作。离屏检查还验证 ESP、姓名标签与追踪线能共同选择及排除友善生物和怪物。

移动回归通过真实 onLivingUpdate、物品使用乘子与碰撞物理重放，验证跳跃不叠加、各模式和补偿强度、转向以及回弹恢复。发包回归覆盖物品 PRE/POST 顺序、队列容量、独立超时、界面确认、回弹与连接切换，实际调用 NetworkManager 验证取消和释放。测试不等于实服反作弊绕过验证。

## 代码入口

| 位置 | 作用 |
| --- | --- |
| `src/cn/sux1ng/client/MeowClient.java` | 启动、关闭及管理器初始化 |
| `src/cn/sux1ng/client/mod/` | 模块注册、按键模式及 Blatant 启用规则 |
| `src/cn/sux1ng/client/events/` | 同步事件分发 |
| `src/cn/sux1ng/client/combat/` | 连续转向及攻击节奏控制 |
| `src/cn/sux1ng/client/targeting/TargetRules.java` | 自动操作与实体视觉共用的目标筛选入口 |
| `src/cn/sux1ng/client/mod/mods/misc/AntiBotMod.java` | 玩家身份观察、生成延迟和机器人筛选 |
| `src/cn/sux1ng/client/gui/` | 两套 ClickGUI、圆形皮肤头像与 ClientSetting |
| `src/cn/sux1ng/client/ui/` | 主题、人工中文译名与通知 |
| `src/cn/sux1ng/client/config/` | 模块及客户端设置保存 |
| `patches/r5-mcp-hooks.patch` | 被忽略的 MCP 源码中的事件与渲染钩子改动 |
| `patches/r6-mcp-hooks.patch` | 在 R5 基础上补充攻击、跳跃、夜视及 HUD 布局钩子 |
| `patches/r6-input-hooks.patch` | 帧开始时获取输入的补充钩子 |
| `patches/r7-aim-hooks.patch` | 鼠标输入后的逐帧相机事件钩子 |
| `patches/r7-visual-hooks.patch` | 玩家披风选择、按需获取与披风绘制钩子 |
| `patches/r7-movement-hooks.patch` | 新读取的移动输入与物品使用减速事件 |
| `src/cn/sux1ng/client/movement/` | 移动方向、队列与发包重放控制 |
| `src/cn/sux1ng/client/util/CapeManager.java` | 披风样式、本地图片与内置图案 |
| `src/cn/sux1ng/client/util/CapeTexture.java` | 有并发上限、退避与磁盘缓存的披风下载 |
| `src/cn/sux1ng/client/input/` | 原始鼠标输入、焦点隔离及鼠标事件缓冲 |
| `src/cn/sux1ng/client/util/RenderState.java` | 视觉效果使用的矩阵与 OpenGL 状态恢复 |

接收包事件在 Netty 线程同步触发；监听器如需修改世界或 GUI，需自行安排到 Minecraft 主线程执行。
