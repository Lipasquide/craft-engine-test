package me.testblocks.injector;

import me.testblocks.api.BlockBehavior;
import me.testblocks.api.DelegatingBlock;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.util.concurrent.Callable;
import java.util.List;

/**
 * The bridge between Minecraft NMS and our Custom Java API.
 */
public class Interceptors {

    /**
     * Intercepts getShape to allow custom hitboxes.
     */
    public static class GetShapeInterceptor {
        @RuntimeType
        public static Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                Object shape = ((BlockBehavior) behavior).getShape(args[0], args[1], args[2], args[3]);
                if (shape != null) return shape;
            }
            return superMethod.call();
        }
    }

    /**
     * Intercepts interactions (right-clicks).
     */
    public static class UseInterceptor {
        @RuntimeType
        public static Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                Object result = ((BlockBehavior) behavior).use(args[0], args[1], args[2], args[3], args[4], args[5]);
                if (result != null) return result;
            }
            return superMethod.call();
        }
    }

    public static class OnPlaceInterceptor {
        @RuntimeType
        public static void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Runnable superMethod) {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                ((BlockBehavior) behavior).onPlace(args[0], args[1], args[2], args[3], (boolean)args[4]);
            }
            superMethod.run();
        }
    }

    public static class NeighborChangedInterceptor {
        @RuntimeType
        public static void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Runnable superMethod) {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                ((BlockBehavior) behavior).neighborChanged(args[0], args[1], args[2], args[3], args[4], (boolean)args[5]);
            }
            superMethod.run();
        }
    }

    /**
     * Intercepts drops (loot tables).
     */
    public static class GetDropsInterceptor {
        @RuntimeType
        public static List<Object> intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<List<Object>> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                List<Object> drops = ((BlockBehavior) behavior).getDrops(args[0], args[1]);
                if (drops != null) return drops;
            }
            return superMethod.call();
        }
    }

    /**
     * Intercepts block sounds (break, place, step, hit).
     */
    public static class GetSoundTypeInterceptor {
        @RuntimeType
        public static Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                Object sound = ((BlockBehavior) behavior).getSoundType(args[0]);
                if (sound != null) return sound;
            }
            return superMethod.call();
        }
    }

    /**
     * Intercepts Physics (Friction, Speed, Jump).
     */
    public static class PhysicsInterceptor {
        @RuntimeType
        public static float getFriction(@This Object thisObj, @SuperCall Callable<Float> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            return (behavior instanceof BlockBehavior) ? ((BlockBehavior) behavior).getFriction() : superMethod.call();
        }

        @RuntimeType
        public static float getExplosionResistance(@This Object thisObj, @SuperCall Callable<Float> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            return (behavior instanceof BlockBehavior) ? ((BlockBehavior) behavior).getExplosionResistance() : superMethod.call();
        }

        @RuntimeType
        public static float getJumpFactor(@This Object thisObj, @SuperCall Callable<Float> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            return (behavior instanceof BlockBehavior) ? ((BlockBehavior) behavior).getJumpFactor() : superMethod.call();
        }

        @RuntimeType
        public static float getSpeedFactor(@This Object thisObj, @SuperCall Callable<Float> superMethod) throws Exception {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            return (behavior instanceof BlockBehavior) ? ((BlockBehavior) behavior).getSpeedFactor() : superMethod.call();
        }
    }

    public static class TickInterceptor {
        @RuntimeType
        public static void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Runnable superMethod) {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                ((BlockBehavior) behavior).tick(args[0], args[1], args[2], args[3]);
            }
            superMethod.run();
        }
    }

    public static class RandomTickInterceptor {
        @RuntimeType
        public static void intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Runnable superMethod) {
            Object behavior = ((DelegatingBlock) thisObj).getBehavior();
            if (behavior instanceof BlockBehavior) {
                ((BlockBehavior) behavior).randomTick(args[0], args[1], args[2], args[3]);
            }
            superMethod.run();
        }
    }
}
