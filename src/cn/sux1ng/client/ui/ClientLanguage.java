package cn.sux1ng.client.ui;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Curated display names. Internal names and saved config keys stay in English. */
public final class ClientLanguage {
    private static final Map<String, String> MODULES = pairs(
            "AutoClicker", "自动点击", "KillAura", "自动攻击", "Aimbot", "瞄准辅助", "ClickSound", "点击音效",
            "NoClickDelay", "无点击间隔", "Sprint", "自动疾跑", "Speed", "速度增强",
            "NoSlow", "使用无减速", "NoJumpDelay", "无跳跃延迟", "CustomCape", "自定义披风",
            "Animations", "手部动作", "Derp", "摇头", "SkinDerp", "皮肤闪烁",
            "Twerk", "自动蹲起", "ESP", "实体高亮", "NameTag", "姓名标签",
            "FullBright", "全亮", "Zoom", "缩放", "Tracers", "追踪线",
            "Breadcrumbs", "移动轨迹", "JumpEffect", "跳跃光环", "HitParticles", "命中特效",
            "DamageParticles", "伤害数字", "AutoTool", "自动工具", "Eagle", "边缘潜行",
            "FastPlace", "快速放置", "TimeChanger", "时间天气", "AutoGG", "自动发送 GG",
            "ArrayList", "功能列表", "InfoHUD", "信息面板", "TargetHUD", "目标面板",
            "ArmorHUD", "装备面板", "Tab", "键盘菜单", "Logo", "标识",
            "ClickGUI", "功能界面", "Blatant", "高风险模式", "BetterFont", "自定义字体",
            "Trajectories", "弹道预览", "TNTTimer", "TNT 倒计时", "DamageIndicator", "受击方向");

    private static final Map<String, String> VALUES = pairs(
            "OnlyWhileAttacking", "仅攻击时", "Target", "目标模式", "Range", "距离",
            "FOV", "视角范围", "MinCPS", "最低点击率", "MaxCPS", "最高点击率",
            "Rotation", "视角调整", "MaxTurnSpeed", "最大转向速度", "Smoothness", "平滑程度",
            "ReactionDelay", "目标反应延迟", "SwitchDelay", "目标保持时间", "AimVariation", "瞄准位置变化",
            "AimPoint", "瞄准位置", "ResponseTime", "平滑响应时间", "HorizontalSpeed", "水平辅助速度",
            "VerticalSpeed", "垂直辅助速度", "Strength", "辅助强度", "DeadZone", "停止修正范围",
            "ClickOnly", "仅按住攻击时", "WeaponOnly", "仅手持武器", "MousePriority", "鼠标操作优先",
            "StopOnTarget", "准星命中后停止修正", "IgnoreTeams", "忽略队友",
            "Players", "玩家", "Animals", "动物", "Mobs", "怪物", "Invisibles", "隐身目标",
            "AutoBlock", "自动格挡", "Mode", "模式", "Volume", "音量", "Variation", "音调浮动",
            "CPS", "点击率", "BlockHit", "格挡攻击", "Amount", "补偿强度",
            "KeepSprint", "攻击时保持疾跑", "Style", "样式", "Theme", "主题",
            "RemoveDelay", "移除延迟", "Speed", "速度", "ColorMode", "配色",
            "Background", "背景", "Sidebar", "侧边条", "StaticColor", "固定颜色",
            "Hat", "帽子", "Jacket", "外套", "Sleeve", "袖子", "Pants", "裤子",
            "Cape", "披风", "ShowHeld", "显示手持物", "SwordProtect", "保护剑",
            "EatProtect", "使用物品时保护", "PreferSilkTouch", "优先精准采集",
            "MinImprovement", "最小效率提升", "Length", "轨迹长度", "FadeTime", "淡出时间",
            "Color", "颜色", "AccentColor", "强调色", "BackgroundColor", "背景色",
            "Scale", "缩放", "Rainbow", "彩虹色", "FPS", "FPS", "BPS", "BPS", "XYZ", "XYZ",
            "Time", "时间", "Weather", "天气", "SneakDelay", "潜行延迟",
            "OnlyOnGround", "仅地面", "Message", "消息", "CustomMsg", "自定义消息",
            "AutoSend", "自动发送", "ShowOptiFine", "其他玩家的 OptiFine 披风",
            "CapeColor", "披风颜色", "Delay", "放置间隔", "Size", "大小",
            "Duration", "持续时间", "VisibleColor", "可见颜色", "InvisibleColor", "遮挡颜色",
            "Armor", "装备", "Gamma", "亮度", "Effect", "效果", "Count", "数量",
            "Rings", "光环数量", "Height", "高度", "Opacity", "不透明度",
            "Width", "轨迹宽度", "LineWidth", "线条粗细",
            "IdleBow", "持弓预览满弦", "Landing", "落点标记", "Radius", "提示半径", "InferMelee", "推断近战来源",
            "ZoomFOV", "缩放视角", "X", "X", "Y", "Y", "Z", "Z");

    private static final Map<String, String> OPTIONS = pairs(
            "Assist", "轻辅助", "Track", "平滑跟随", "Closest", "贴近准星", "Body", "躯干", "Head", "头部",
            "Single", "单目标", "Switch", "切换目标", "Lock", "锁定", "Smooth", "平滑",
            "Silent", "静默", "None", "无", "Normal", "标准", "Jitter", "抖动",
            "Double", "双击", "Fixed", "固定", "Legit", "自然", "Vanilla", "原版",
            "NCP", "NCP", "AutoJump", "自动跳跃", "Omni", "全向",
            "Dropdown", "面板式", "CSGO", "窗口式", "MeowClient", "MeowClient",
            "Skeet", "简洁", "Light", "浅色", "Rainbow", "彩虹", "Astolfo", "柔彩",
            "Pulse", "呼吸", "Static", "固定", "Spin", "旋转", "Backward", "反向",
            "1.7", "1.7", "Exhibition", "展示", "Old", "经典", "Chill", "舒缓",
            "Horizontal", "横向", "Vertical", "纵向", "Clear", "清爽", "Rain", "下雨",
            "Thunder", "雷暴", "Custom", "自定义", "Meow", "喵系",
            "OptiFine", "OptiFine", "Minecon", "Minecon", "2D", "2D", "Box3D", "3D 方框",
            "Gamma", "伽马亮度", "NightVision", "夜视", "Heart", "爱心", "Flame", "火焰",
            "Crit", "暴击", "Slime", "史莱姆", "Portal", "传送门", "Smoke", "烟雾",
            "Neon", "霓虹", "Simple", "简洁", "Outline", "描边", "Rect", "方框",
            "CSGO(Gamesense)", "边框", "Sense", "简约");

    private static final Map<String, String> UI = pairs(
            "Raw mouse input", "原始鼠标输入", "Unaccelerated relative motion", "使用原始位移，避免系统加速",
            "Standard input fallback", "当前使用标准输入",
            "HUD editor", "HUD 编辑器", "Key bindings", "按键管理", "Reset all", "全部重置",
            "Reset", "重置", "Done", "完成", "Conflict", "按键冲突", "None", "无",
            "Drag to move · Scroll to scale · Right click to toggle", "拖动调整位置 · 滚轮缩放 · 右键切换显示",
            "Search", "搜索", "Select a module", "选择功能", "Settings", "设置",
            "Key", "按键", "Bind", "触发方式", "Press key...", "请按下按键…",
            "Back", "返回", "ClientSetting", "客户端设置", "Chinese", "中文",
            "Show client controls in Chinese", "将客户端功能显示为中文",
            "ON", "开", "OFF", "关", "Enabled", "已开启", "Disabled", "已关闭",
            "Enable Blatant in Misc first", "请先在其他分类开启高风险模式",
            "Ready", "已就绪", "Help", "帮助", "Usage", "用法",
            "Module not found", "未找到功能", "Bound", "已绑定",
            "No settings", "无可调选项", "Module", "功能", "Unknown command", "未知指令",
            "Show help", "显示帮助", "Toggle module", "切换功能",
            "Bind module key", "绑定功能按键");

    private static volatile boolean chinese;

    private ClientLanguage() {}

    public static boolean isChinese() { return chinese; }
    public static void setChinese(boolean enabled) { chinese = enabled; }

    public static String category(Category category) {
        if (!chinese) return category.name();
        switch (category) {
            case COMBAT: return "战斗";
            case MOVEMENT: return "移动";
            case PLAYER: return "玩家";
            case RENDER: return "视觉";
            case MISC: return "其他";
            case WORLD: return "世界";
            case HUD: return "信息";
            case DRAW: return "界面";
            default: return category.name();
        }
    }

    public static String module(Mod mod) { return module(mod.getName()); }

    public static String module(String internalName) {
        return chinese ? MODULES.getOrDefault(internalName, internalName) : internalName;
    }

    public static String value(Value<?> setting) { return value(setting.getName()); }

    public static String value(String internalName) {
        return chinese ? VALUES.getOrDefault(internalName, internalName) : internalName;
    }

    public static String mode(ModeValue setting) { return option(setting.getName(), setting.getValue()); }

    public static String option(String raw) { return option("", raw); }

    public static String option(String settingName, String raw) {
        if ("Theme".equals(settingName)) {
            if ("MeowClient".equals(raw)) return chinese ? "樱粉" : "Sakura";
            if ("Skeet".equals(raw)) return chinese ? "暮紫" : "Twilight";
            if ("Light".equals(raw)) return chinese ? "奶霜" : "Cream";
        }
        if ("Message".equals(settingName) && !"Custom".equals(raw)) return raw;
        if ("Weather".equals(settingName) && "Clear".equals(raw)) return chinese ? "晴朗" : raw;
        if (!chinese) {
            if ("CSGO".equals(raw)) return "Window";
            if ("Dropdown".equals(raw)) return "Panels";
            if ("CSGO(Gamesense)".equals(raw)) return "Framed";
            if ("Skeet".equals(raw)) return "Clean";
            return raw;
        }
        return OPTIONS.getOrDefault(raw, raw);
    }

    public static String bindMode(Mod.BindMode mode) {
        if (!chinese) {
            switch (mode) {
                case TOGGLE: return "Toggle";
                case HOLD: return "Hold";
                case SMART: return "Smart";
                default: return mode.name();
            }
        }
        switch (mode) {
            case TOGGLE: return "切换";
            case HOLD: return "按住";
            case SMART: return "智能";
            default: return mode.name();
        }
    }

    public static String ui(String english) { return chinese ? UI.getOrDefault(english, english) : english; }

    private static Map<String, String> pairs(String... entries) {
        if (entries.length % 2 != 0) throw new IllegalArgumentException("Unpaired translation");
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) result.put(entries[i], entries[i + 1]);
        return Collections.unmodifiableMap(result);
    }
}
