package cn.sux1ng.client.mod.mods.draw;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;

/**
 * BetterFont —— 自定义字体渲染模块
 * TODO: 未来实现 TTF 字体加载、字形缓存、OpenGL 纹理渲染
 * 参考 Reversal 的 ModernFontRenderer + FontManager 实现
 */
public class BetterFontMod extends Mod {
    public BetterFontMod() {
        super("BetterFont", Category.DRAW);
    }
}
