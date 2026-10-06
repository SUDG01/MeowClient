package cn.sux1ng.client.combat;

/** Critically damped angular correction using elapsed seconds, not a fixed fraction per frame. */
public final class FrameAimMotion {
    private double yawVelocity, pitchVelocity;
    public void reset() { yawVelocity = pitchVelocity = 0; }
    public float[] step(float yawError, float pitchError, double seconds, double responseMillis,
                        double yawSpeed, double pitchSpeed, double strength, double deadZone) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 0.1) { reset(); return new float[]{0, 0}; }
        double omega = 2.0 / Math.max(0.04, responseMillis / 1000.0);
        double[] yaw = axis(AimMotion.wrap(yawError), yawVelocity, omega, seconds, yawSpeed * strength, deadZone);
        double[] pitch = axis(pitchError, pitchVelocity, omega, seconds, pitchSpeed * strength, deadZone);
        yawVelocity = yaw[1]; pitchVelocity = pitch[1];
        return new float[]{(float)yaw[0], (float)pitch[0]};
    }
    private static double[] axis(double error, double velocity, double omega, double dt, double speed, double deadZone) {
        if (Math.abs(error) <= deadZone || speed <= 0) return new double[]{0, 0};
        double relative = -error, carry = velocity + omega * relative, decay = Math.exp(-omega * dt);
        double step = (relative + carry * dt) * decay - relative;
        double nextVelocity = (velocity - omega * carry * dt) * decay;
        double limit = speed * dt;
        if (Math.signum(step) != Math.signum(error)) return new double[]{0, 0};
        if (Math.abs(step) > limit) step = Math.copySign(limit, step);
        if (Math.abs(step) >= Math.abs(error)) return new double[]{error, 0};
        nextVelocity = Math.max(-speed, Math.min(speed, nextVelocity));
        return new double[]{step, nextVelocity};
    }
}
