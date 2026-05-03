package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;

public class CustomShapeBehavior implements BlockBehavior {

    private final Object shape;

    public CustomShapeBehavior(Object nmsShape) {
        this.shape = nmsShape;
    }

    @Override
    public Object getShape(Object blockState, Object blockGetter, Object blockPos, Object collisionContext) {
        return shape;
    }
}
