package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;

public class ComplexShapeBehavior implements BlockBehavior {

    private final Object shape;
    private final Object soundType;

    public ComplexShapeBehavior(Object shape, Object soundType) {
        this.shape = shape;
        this.soundType = soundType;
    }

    @Override
    public Object getShape(Object blockState, Object blockGetter, Object blockPos, Object collisionContext) {
        return shape;
    }

    @Override
    public Object getSoundType(Object blockState) {
        return soundType;
    }
}
