package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;

public class ShapeFactory {
    public static BlockBehavior slab() {
        return EmptyBlockBehavior.INSTANCE;
    }
}
