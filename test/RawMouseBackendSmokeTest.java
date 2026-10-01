import cn.sux1ng.client.input.WindowsRawMouseInput;
import java.lang.reflect.Field;

/** Creates and releases the real hidden Windows input window without launching Minecraft. */
public class RawMouseBackendSmokeTest {
    public static void main(String[] args) throws Exception {
        WindowsRawMouseInput input = new WindowsRawMouseInput();
        try {
            long deadline = System.nanoTime() + 3_000_000_000L;
            while (!input.isAvailable() && System.nanoTime() < deadline) Thread.sleep(10);
            if (!input.isAvailable()) throw new AssertionError("Windows raw mouse registration failed");
            input.setCaptured(true);
            Thread.sleep(50);
            input.setCaptured(false);
            input.setCaptured(true);
            Thread.sleep(50);
            if (input.consume() == null) throw new AssertionError("raw input switched to fallback unexpectedly");
            System.out.println("PASS native raw mouse registration and capture transitions");
        } finally { input.close(); }
        Field field = WindowsRawMouseInput.class.getDeclaredField("thread"); field.setAccessible(true);
        if (((Thread)field.get(input)).isAlive()) throw new AssertionError("raw input worker did not stop");
        System.out.println("PASS native raw mouse window/thread cleanup");
    }
}
