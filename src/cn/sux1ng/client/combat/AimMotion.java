package cn.sux1ng.client.combat;

/** Stateful angular motion, stepped once per movement PRE rather than once per rendered frame. */
public final class AimMotion {
    private boolean initialized;
    private String mode;
    private float yaw, pitch, velocityYaw, velocityPitch;

    public void reset() { initialized = false; mode = null; velocityYaw = velocityPitch = 0; }
    public void targetChanged() { velocityYaw = velocityPitch = 0; }
    public void observe(float actualYaw, float actualPitch) {
        if (Math.abs(wrap(actualYaw - yaw)) > 0.01 || Math.abs(actualPitch - pitch) > 0.01) targetChanged();
        yaw = actualYaw; pitch = clamp(actualPitch, -90, 90);
    }
    public float[] turn(String mode, float cameraYaw, float cameraPitch, float intendedYaw, float intendedPitch,
                        float maxSpeed, float smoothness, float sensitivity) {
        if ("None".equals(mode)) { reset(); return new float[]{cameraYaw, cameraPitch}; }
        if (!initialized || !mode.equals(this.mode)) {
            yaw = cameraYaw; pitch = cameraPitch; velocityYaw = velocityPitch = 0; initialized = true; this.mode = mode;
        } else if (!"Silent".equals(mode)) {
            if (Math.abs(wrap(cameraYaw - yaw)) > maxSpeed || Math.abs(cameraPitch - pitch) > maxSpeed) targetChanged();
            yaw = cameraYaw; pitch = cameraPitch;
        }
        float errorYaw = wrap(intendedYaw - yaw), errorPitch = clamp(intendedPitch, -90, 90) - pitch;
        float gain = "Lock".equals(mode) ? 0.85f : 1f / (1f + smoothness * ("Silent".equals(mode) ? 0.10f : 0.14f));
        float wantedYaw = errorYaw * gain, wantedPitch = errorPitch * gain;
        float length = (float)Math.hypot(wantedYaw, wantedPitch);
        if (length > maxSpeed) { wantedYaw *= maxSpeed / length; wantedPitch *= maxSpeed / length; }
        float acceleration = Math.max(0.5f, maxSpeed * ("Lock".equals(mode) ? 0.45f : 0.22f));
        velocityYaw = approach(velocityYaw, wantedYaw, acceleration);
        velocityPitch = approach(velocityPitch, wantedPitch, acceleration);
        float speed = (float)Math.hypot(velocityYaw, velocityPitch);
        if (speed > maxSpeed) { velocityYaw *= maxSpeed / speed; velocityPitch *= maxSpeed / speed; }
        float stepYaw = matchingStep(velocityYaw, errorYaw), stepPitch = matchingStep(velocityPitch, errorPitch);
        float factor = sensitivity * 0.6f + 0.2f;
        float unit = factor * factor * factor * 8f * 0.15f;
        stepYaw = quantize(stepYaw, errorYaw, unit); stepPitch = quantize(stepPitch, errorPitch, unit);
        yaw += stepYaw; pitch = clamp(pitch + stepPitch, -90, 90);
        return new float[]{yaw, pitch};
    }
    private static float quantize(float step, float error, float unit) {
        if (unit <= 0) return step;
        float value = Math.round(step / unit) * unit;
        if (Math.abs(value) > Math.abs(error)) value = (float)Math.floor(Math.abs(error) / unit) * unit * Math.signum(error);
        return value;
    }
    private static float matchingStep(float velocity, float error) {
        return Math.signum(velocity) != Math.signum(error) ? 0 : Math.signum(error) * Math.min(Math.abs(velocity), Math.abs(error));
    }
    private static float approach(float value, float goal, float limit) { return value + clamp(goal - value, -limit, limit); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    public static float wrap(float value) { value %= 360; if (value >= 180) value -= 360; if (value < -180) value += 360; return value; }
}
