package me.testblocks.injector;

import me.testblocks.api.BlockBehavior;
import me.testblocks.api.DelegatingBlock;
import me.testblocks.api.DelegatingBlockState;
import me.testblocks.behavior.CustomDropBehavior;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class BlockStateGenerator {

    public static MethodHandle constructor_CraftEngineBlockState;
    public static Object instance_StateDefinition_Factory;

    public static void init() throws Exception {
        Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
        Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
        Class<?> reference2ObjectArrayMapClass = Class.forName("it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap");
        Class<?> mapCodecClass = Class.forName("com.mojang.serialization.MapCodec");

        Class<?> generatedStateClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                .subclass(blockStateClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("me.testblocks.injector.CraftEngineBlockState")
                .defineField("customBlockState", Object.class, Visibility.PUBLIC)
                .implement(DelegatingBlockState.class)
                .method(ElementMatchers.named("blockState")).intercept(FieldAccessor.ofField("customBlockState"))
                .method(ElementMatchers.named("setBlockState")).intercept(FieldAccessor.ofField("customBlockState"))
                .method(ElementMatchers.named("getDrops").and(ElementMatchers.takesArguments(2)))
                    .intercept(MethodDelegation.to(GetDropsInterceptor.INSTANCE))
                .method(ElementMatchers.named("hasProperty").and(ElementMatchers.takesArguments(1)))
                    .intercept(MethodDelegation.to(HasPropertyInterceptor.INSTANCE))
                .method(ElementMatchers.named("getValue").and(ElementMatchers.takesArguments(1)))
                    .intercept(MethodDelegation.to(GetPropertyValueInterceptor.INSTANCE))
                .method(ElementMatchers.named("setValue").and(ElementMatchers.takesArguments(2)))
                    .intercept(MethodDelegation.to(SetPropertyValueInterceptor.INSTANCE))
                .method(ElementMatchers.named("is").and(ElementMatchers.takesArguments(1))
                        .and(ElementMatchers.returns(boolean.class)))
                    .intercept(MethodDelegation.to(IsBlockInterceptor.INSTANCE))
                .make()
                .load(BlockStateGenerator.class.getClassLoader())
                .getLoaded();

        constructor_CraftEngineBlockState = MethodHandles.publicLookup().in(generatedStateClass)
                .findConstructor(generatedStateClass, MethodType.methodType(void.class, blockClass, reference2ObjectArrayMapClass, mapCodecClass))
                .asType(MethodType.methodType(blockStateClass, blockClass, reference2ObjectArrayMapClass, mapCodecClass));

        Class<?> factoryInterface = Class.forName("net.minecraft.world.level.block.state.StateDefinition$Factory");
        Class<?> generatedFactoryClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                .subclass(Object.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("me.testblocks.injector.CraftEngineStateFactory")
                .implement(factoryInterface)
                .method(ElementMatchers.named("create"))
                    .intercept(MethodDelegation.to(CreateStateInterceptor.INSTANCE))
                .make()
                .load(BlockStateGenerator.class.getClassLoader())
                .getLoaded();

        instance_StateDefinition_Factory = generatedFactoryClass.getDeclaredConstructor().newInstance();
    }

    public static class GetDropsInterceptor {
        public static final GetDropsInterceptor INSTANCE = new GetDropsInterceptor();
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            try {
                Object block = thisObj.getClass().getMethod("getBlock").invoke(thisObj);
                if (block instanceof DelegatingBlock db) {
                    BlockBehavior behavior = db.behaviorDelegate().value();
                    if (behavior instanceof CustomDropBehavior cdb) {
                        org.bukkit.Material mat = cdb.getDrop();
                        Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
                        Method asNmsCopyMethod = craftItemStackClass.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class);
                        Object nmsStack = asNmsCopyMethod.invoke(null, new org.bukkit.inventory.ItemStack(mat));
                        List<Object> drops = new ArrayList<>();
                        drops.add(nmsStack);
                        return drops;
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            return List.of();
        }
    }

    public static class HasPropertyInterceptor {
        public static final HasPropertyInterceptor INSTANCE = new HasPropertyInterceptor();
        private static Object WATERLOGGED;
        static {
            try {
                WATERLOGGED = Class.forName("net.minecraft.world.level.block.state.properties.BlockStateProperties").getDeclaredField("WATERLOGGED").get(null);
            } catch (Exception ignore) {}
        }
        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args) {
            if (args[0] == WATERLOGGED) return true; // Pretend we are always waterloggable if requested for simplicity
            return false;
        }
    }

    public static class GetPropertyValueInterceptor {
        public static final GetPropertyValueInterceptor INSTANCE = new GetPropertyValueInterceptor();
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            // Return false for waterlogged by default
            if (args[0].getClass().getSimpleName().equals("BooleanProperty")) return false;
            return null;
        }
    }

    public static class SetPropertyValueInterceptor {
        public static final SetPropertyValueInterceptor INSTANCE = new SetPropertyValueInterceptor();
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            return thisObj;
        }
    }

    public static class IsBlockInterceptor {
        public static final IsBlockInterceptor INSTANCE = new IsBlockInterceptor();
        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args) {
            try {
                Object block = thisObj.getClass().getMethod("getBlock").invoke(thisObj);
                Object targetBlock = args[0];
                if (targetBlock.getClass().getSimpleName().equals("BlockHolder")) {
                   targetBlock = targetBlock.getClass().getMethod("value").invoke(targetBlock);
                }
                return block == targetBlock;
            } catch (Exception e) { return false; }
        }
    }

    public static class CreateStateInterceptor {
        public static final CreateStateInterceptor INSTANCE = new CreateStateInterceptor();
        @RuntimeType
        public Object intercept(@AllArguments Object[] args) throws Throwable {
            return constructor_CraftEngineBlockState.invoke(args[0], args[1], args[2]);
        }
    }
}
