package cn.sux1ng.client.combat;

import java.util.Objects;
import java.util.Random;

/** Holds each chosen interval until the next attack and never queues catch-up attacks after a pause. */
public final class AttackRhythm {
    private final Random random;
    private double min = 10, max = 13, tempo = 11.5, due;
    private boolean aligned;
    private long acquired;
    private int lastAttackTick = Integer.MIN_VALUE;

    public AttackRhythm(Random random) { this.random = Objects.requireNonNull(random, "random"); }
    public void configure(double min, double max) {
        this.min = Math.max(1, Math.min(20, Math.min(min, max)));
        this.max = Math.max(this.min, Math.min(20, Math.max(min, max)));
        tempo = Math.max(this.min, Math.min(this.max, tempo));
    }
    public void acquire(long now, long reaction) { acquired = now + reaction; due = acquired; aligned = false; }
    public void reset() { aligned = false; due = Double.POSITIVE_INFINITY; lastAttackTick = Integer.MIN_VALUE; }
    public void aligned(boolean value, long now) {
        if (value && !aligned) due = Math.max(acquired, now + interval());
        aligned = value;
    }
    public boolean ready(long now, int tick) { return aligned && now >= due && tick != lastAttackTick; }
    public void attacked(long now, int tick) {
        double base = Double.isInfinite(due) || now - due > 100 ? now : due;
        due = Math.max(now + 1, base + interval()); lastAttackTick = tick;
    }
    public void externalAttack(long now, int tick) { due = now + interval(); lastAttackTick = tick; }
    public boolean reactionPassed(long now) { return now >= acquired; }
    private double interval() {
        double next = min + random.nextDouble() * (max - min);
        tempo = Math.max(min, Math.min(max, tempo * 0.65 + next * 0.35));
        return 1000.0 / tempo;
    }
}
