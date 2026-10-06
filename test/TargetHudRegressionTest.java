import cn.sux1ng.client.mod.mods.render.TargetHUDMod;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.awt.Color;

/** Boundary checks for the health colors used by the real TargetHUD draw path. */
public class TargetHudRegressionTest {
    public static void main(String[] args) throws Exception {
        TargetHUDMod hud = new TargetHUDMod();
        Method color = TargetHUDMod.class.getDeclaredMethod("getHealthColor", float.class, float.class); color.setAccessible(true);
        int failures = 0;
        float[][] cases = {{40, 20}, {20, 20}, {13.2f, 20}, {6.6f, 20}, {0, 20}, {-1, 20}, {20, 0}, {20, -1},
                {Float.NaN, 20}, {Float.POSITIVE_INFINITY, 20}, {20, Float.NaN}, {20, Float.POSITIVE_INFINITY}, {Float.MAX_VALUE, Float.MIN_VALUE}};
        for (float[] values : cases) {
            try {
                int argb = (Integer)color.invoke(hud, values[0], values[1]);
                Color actual = new Color(argb, true);
                if (actual.getAlpha() != 255) throw new AssertionError("health color lost its opacity");
                System.out.println("PASS health color " + values[0] + "/" + values[1]);
            } catch (Throwable failure) {
                failures++; Throwable cause = failure instanceof InvocationTargetException ? ((InvocationTargetException)failure).getCause() : failure;
                System.err.println("FAIL health color " + values[0] + "/" + values[1] + ": " + cause);
            }
        }
        int full = (Integer)color.invoke(hud, 20f, 20f), empty = (Integer)color.invoke(hud, 0f, 20f);
        if ((Integer)color.invoke(hud, 40f, 20f) != full || (Integer)color.invoke(hud, -1f, 20f) != empty
                || (Integer)color.invoke(hud, Float.NaN, 20f) != empty) throw new AssertionError("invalid health did not use the bounded endpoint colors");
        if (failures != 0) throw new AssertionError(failures + " TargetHUD health checks failed");
        System.out.println("TargetHUD health checks passed");
    }
}
