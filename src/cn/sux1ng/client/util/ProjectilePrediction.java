package cn.sux1ng.client.util;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic centre trajectory using the 1.8.9 per-tick projectile integrator. */
public final class ProjectilePrediction {
    private ProjectilePrediction() {}

    public interface Environment {
        MovingObjectPosition trace(Vec3 from, Vec3 to);
        boolean isWater(Vec3 position);
        boolean isLoaded(Vec3 position);
    }

    public static final class Result {
        public final List<Vec3> points;
        public final MovingObjectPosition hit;
        Result(List<Vec3> points, MovingObjectPosition hit) {
            this.points = Collections.unmodifiableList(points); this.hit = hit;
        }
    }

    public static double bowSpeed(int useTicks) {
        double charge = Math.max(0, useTicks) / 20.0;
        charge = Math.min(1, (charge * charge + charge * 2) / 3);
        return charge < 0.1 ? 0 : charge * 3;
    }

    public static Result simulate(Vec3 position, Vec3 velocity, boolean arrow, Environment environment) {
        List<Vec3> points = new ArrayList<>();
        points.add(position);
        for (int tick = 0; tick < 200; tick++) {
            Vec3 next = position.add(velocity);
            if (next.yCoord < -64 || !environment.isLoaded(next)) break;
            MovingObjectPosition hit = environment.trace(position, next);
            if (hit != null) {
                points.add(hit.hitVec);
                return new Result(points, hit);
            }
            // Vanilla detects immersion before advancing the projectile for this tick.
            boolean water = environment.isWater(position);
            position = next;
            points.add(position);
            double drag = water ? (arrow ? 0.6f : 0.8f) : 0.99f;
            velocity = new Vec3(velocity.xCoord * drag,
                    velocity.yCoord * drag - (arrow ? 0.05f : 0.03f), velocity.zCoord * drag);
        }
        return new Result(points, null);
    }
}
