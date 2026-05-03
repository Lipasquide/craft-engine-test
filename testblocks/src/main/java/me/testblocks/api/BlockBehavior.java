package me.testblocks.api;

/**
 * Custom behavior for our delegating block.
 */
public interface BlockBehavior {
    // We can add methods here that correspond to NMS block methods
    default Object getShape(Object blockState, Object blockGetter, Object blockPos, Object collisionContext) {
        return null; // Let NMS handle it by default
    }
}
