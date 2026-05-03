package me.testblocks.injector;

import me.testblocks.api.BlockBehavior;
import me.testblocks.api.DelegatingBlock;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.util.concurrent.Callable;

public class Interceptors {

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
}
