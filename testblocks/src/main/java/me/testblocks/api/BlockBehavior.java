package me.testblocks.api;

import java.util.concurrent.Callable;

public abstract class BlockBehavior {

    public Object rotate(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    public Object mirror(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return args[0];
    }

    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return (boolean) superMethod.call();
    }

    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return (boolean) superMethod.call();
    }

    public void onBrokenAfterFall(Object thisBlock, Object[] args) throws Exception {}
    public void onLand(Object thisBlock, Object[] args) throws Exception {}
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) throws Exception { return false; }
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) throws Exception { return false; }
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) throws Exception { return 0; }
    public Object getContainer(Object thisBlock, Object[] args) throws Exception { return null; }
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) throws Exception { return false; }
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {}

    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public boolean placeLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) { return false; }
    public boolean canPlaceLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) { return false; }

    public Object pickupBlock(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    public void entityInside(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public void onRemove(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public int getSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) { return 0; }
    public int getDirectSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) { return 0; }
    public boolean isSignalSource(Object thisBlock, Object[] args, Callable<Object> superMethod) { return false; }

    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    public void spawnAfterBreak(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {}
    public void stepOn(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {}
    public void onProjectileHit(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {}
    public void placeMultiState(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {}
}
