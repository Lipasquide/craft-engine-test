package me.testblocks.api;

import me.testblocks.util.ObjectHolder;

public interface DelegatingBlock {
    ObjectHolder<BlockBehavior> behaviorDelegate();
    ObjectHolder<BlockShape> shapeDelegate();
    boolean isNoteBlock();
    boolean isTripwire();
}
