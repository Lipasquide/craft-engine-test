package me.testblocks.api;

/**
 * Custom behavior for our delegating block.
 */
public interface BlockBehavior {

    default Object getShape(Object blockState, Object blockGetter, Object blockPos, Object collisionContext) {
        return null;
    }

    default void onPlace(Object blockState, Object level, Object blockPos, Object oldState, boolean isMoving) {
    }

    default void neighborChanged(Object blockState, Object level, Object blockPos, Object neighborBlock, Object neighborPos, boolean isMoving) {
    }

    default Object use(Object blockState, Object level, Object blockPos, Object player, Object hand, Object hitResult) {
        return null;
    }

    default java.util.List<Object> getDrops(Object blockState, Object lootParamsBuilder) {
        return null;
    }

    default Object getSoundType(Object blockState) {
        return null;
    }

    default void tick(Object blockState, Object serverLevel, Object blockPos, Object randomSource) {
    }

    default void randomTick(Object blockState, Object serverLevel, Object blockPos, Object randomSource) {
    }
}
