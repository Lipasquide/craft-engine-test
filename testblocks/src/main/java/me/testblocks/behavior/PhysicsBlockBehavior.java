package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;

public class PhysicsBlockBehavior implements BlockBehavior {

    private final float friction;
    private final float speedFactor;
    private final float jumpFactor;

    public PhysicsBlockBehavior(float friction, float speedFactor, float jumpFactor) {
        this.friction = friction;
        this.speedFactor = speedFactor;
        this.jumpFactor = jumpFactor;
    }

    @Override
    public float getFriction() {
        return friction;
    }

    @Override
    public float getSpeedFactor() {
        return speedFactor;
    }

    @Override
    public float getJumpFactor() {
        return jumpFactor;
    }
}
