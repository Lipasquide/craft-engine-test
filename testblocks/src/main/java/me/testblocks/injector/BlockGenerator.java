package me.testblocks.injector;

import me.testblocks.api.*;
import me.testblocks.behavior.EmptyBlockBehavior;
import me.testblocks.util.ObjectHolder;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;

public class BlockGenerator {

    public static MethodHandle constructor_CraftEngineBlock;
    public static Field field_behaviorHolder;
    public static Field field_shapeHolder;
    public static DefaultBlockShape DEFAULT_SHAPE;

    public static void init() throws Exception {
        Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
        Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");

        Class<?> fallableClass = Class.forName("net.minecraft.world.level.block.Fallable");
        Class<?> bonemealableClass = Class.forName("net.minecraft.world.level.block.BonemealableBlock");
        Class<?> waterloggedClass = Class.forName("net.minecraft.world.level.block.SimpleWaterloggedBlock");
        Class<?> worldlyContainerClass = Class.forName("net.minecraft.world.WorldlyContainerHolder");

        Class<?> generatedBlockClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                .subclass(blockClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("me.testblocks.injector.CraftEngineBlock")
                .defineField("behaviorHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("shapeHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("isClientSideNoteBlock", boolean.class, Visibility.PUBLIC)
                .defineField("isClientSideTripwire", boolean.class, Visibility.PUBLIC)
                .implement(DelegatingBlock.class)
                .implement(fallableClass, bonemealableClass, waterloggedClass, worldlyContainerClass)
                .method(ElementMatchers.named("behaviorDelegate")).intercept(FieldAccessor.ofField("behaviorHolder"))
                .method(ElementMatchers.named("shapeDelegate")).intercept(FieldAccessor.ofField("shapeHolder"))
                .method(ElementMatchers.named("isNoteBlock")).intercept(FieldAccessor.ofField("isClientSideNoteBlock"))
                .method(ElementMatchers.named("isTripwire")).intercept(FieldAccessor.ofField("isClientSideTripwire"))

                .method(ElementMatchers.named("getShape").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(GetShapeInterceptor.INSTANCE))
                .method(ElementMatchers.named("getCollisionShape").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(GetCollisionShapeInterceptor.INSTANCE))
                .method(ElementMatchers.named("getBlockSupportShape").and(ElementMatchers.takesArguments(3)))
                    .intercept(MethodDelegation.to(GetSupportShapeInterceptor.INSTANCE))
                .method(ElementMatchers.named("isPathFindable").and(ElementMatchers.takesArguments(2)))
                    .intercept(MethodDelegation.to(IsPathFindableInterceptor.INSTANCE))
                .method(ElementMatchers.named("mirror").and(ElementMatchers.takesArguments(2)))
                    .intercept(MethodDelegation.to(MirrorInterceptor.INSTANCE))
                .method(ElementMatchers.named("rotate").and(ElementMatchers.takesArguments(2)))
                    .intercept(MethodDelegation.to(RotateInterceptor.INSTANCE))
                .method(ElementMatchers.named("hasAnalogOutputSignal").and(ElementMatchers.takesArguments(1)))
                    .intercept(MethodDelegation.to(HasAnalogOutputSignalInterceptor.INSTANCE))
                .method(ElementMatchers.named("getAnalogOutputSignal").and(ElementMatchers.takesArguments(3)))
                    .intercept(MethodDelegation.to(GetAnalogOutputSignalInterceptor.INSTANCE))
                .method(ElementMatchers.named("tick").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(TickInterceptor.INSTANCE))
                .method(ElementMatchers.named("randomTick").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(RandomTickInterceptor.INSTANCE))
                .method(ElementMatchers.named("onPlace").and(ElementMatchers.takesArguments(5)))
                    .intercept(MethodDelegation.to(OnPlaceInterceptor.INSTANCE))
                .method(ElementMatchers.named("canSurvive").and(ElementMatchers.takesArguments(3)))
                    .intercept(MethodDelegation.to(CanSurviveInterceptor.INSTANCE))
                .method(ElementMatchers.named("neighborChanged").and(ElementMatchers.takesArguments(6)))
                    .intercept(MethodDelegation.to(NeighborChangedInterceptor.INSTANCE))
                .method(ElementMatchers.named("updateShape").and(ElementMatchers.takesArguments(6)))
                    .intercept(MethodDelegation.to(UpdateShapeInterceptor.INSTANCE))
                .method(ElementMatchers.named("pickupBlock").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(PickUpBlockInterceptor.INSTANCE))
                .method(ElementMatchers.named("placeLiquid").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(PlaceLiquidInterceptor.INSTANCE))
                .method(ElementMatchers.named("canPlaceLiquid").and(ElementMatchers.takesArguments(5)))
                    .intercept(MethodDelegation.to(CanPlaceLiquidInterceptor.INSTANCE))
                .method(ElementMatchers.named("entityInside").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(EntityInsideInterceptor.INSTANCE))
                .method(ElementMatchers.named("getSignal").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(GetSignalInterceptor.INSTANCE))
                .method(ElementMatchers.named("getDirectSignal").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(GetDirectSignalInterceptor.INSTANCE))
                .method(ElementMatchers.named("isSignalSource").and(ElementMatchers.takesArguments(1)))
                    .intercept(MethodDelegation.to(IsSignalSourceInterceptor.INSTANCE))
                .method(ElementMatchers.named("playerWillDestroy").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(PlayerWillDestroyInterceptor.INSTANCE))
                .method(ElementMatchers.named("spawnAfterBreak").and(ElementMatchers.takesArguments(5)))
                    .intercept(MethodDelegation.to(SpawnAfterBreakInterceptor.INSTANCE))
                .method(ElementMatchers.named("stepOn").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(StepOnInterceptor.INSTANCE))
                .method(ElementMatchers.named("onProjectileHit").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(OnProjectileHitInterceptor.INSTANCE))
                .method(ElementMatchers.named("onRemove").and(ElementMatchers.takesArguments(5)))
                    .intercept(MethodDelegation.to(OnRemoveInterceptor.INSTANCE))
                .make()
                .load(BlockGenerator.class.getClassLoader())
                .getLoaded();

        constructor_CraftEngineBlock = MethodHandles.publicLookup().in(generatedBlockClass)
                .findConstructor(generatedBlockClass, MethodType.methodType(void.class, blockBehaviourPropertiesClass))
                .asType(MethodType.methodType(blockClass, blockBehaviourPropertiesClass));

        field_behaviorHolder = generatedBlockClass.getField("behaviorHolder");
        field_shapeHolder = generatedBlockClass.getField("shapeHolder");

        Class<?> blocksClass = Class.forName("net.minecraft.world.level.block.Blocks");
        Object stoneBlock = blocksClass.getDeclaredField("STONE").get(null);
        Object stoneDefaultState = stoneBlock.getClass().getMethod("defaultBlockState").invoke(stoneBlock);
        DEFAULT_SHAPE = new DefaultBlockShape(stoneDefaultState);
    }

    public static Object generateBlock(String id) throws Throwable {
        Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");
        Object properties = blockBehaviourPropertiesClass.getMethod("of").invoke(null);

        Object blockInstance = constructor_CraftEngineBlock.invoke(properties);
        field_behaviorHolder.set(blockInstance, new ObjectHolder<>(EmptyBlockBehavior.INSTANCE));
        field_shapeHolder.set(blockInstance, new ObjectHolder<>(DEFAULT_SHAPE));

        Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
        Class<?> builderClass = Class.forName("net.minecraft.world.level.block.state.StateDefinition$Builder");
        Constructor<?> builderCtor = builderClass.getDeclaredConstructor(blockClass);
        Object builder = builderCtor.newInstance(blockInstance);

        Class<?> factoryInterface = Class.forName("net.minecraft.world.level.block.state.StateDefinition$Factory");
        Method createMethod = builderClass.getDeclaredMethod("create", Function.class, factoryInterface);

        Object stateDefinition = createMethod.invoke(builder,
            (Function<Object, Object>) b -> {
                try { return blockClass.getMethod("defaultBlockState").invoke(b); }
                catch (Exception e) { throw new RuntimeException(e); }
            },
            BlockStateGenerator.instance_StateDefinition_Factory);

        Field stateDefField = blockClass.getDeclaredField("stateDefinition");
        stateDefField.setAccessible(true);
        stateDefField.set(blockInstance, stateDefinition);

        Field statesField = stateDefinition.getClass().getDeclaredField("states");
        statesField.setAccessible(true);
        ImmutableList<?> states = (ImmutableList<?>) statesField.get(stateDefinition);

        Field defaultStateField = blockClass.getDeclaredField("defaultBlockState");
        defaultStateField.setAccessible(true);
        defaultStateField.set(blockInstance, states.get(0));

        return blockInstance;
    }

    public static abstract class BaseInterceptor {
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisObj).behaviorDelegate();
            return handle(holder.value(), thisObj, args, superMethod);
        }
        protected abstract Object handle(BlockBehavior behavior, Object thisObj, Object[] args, Callable<Object> superMethod) throws Exception;
    }

    public static class GetShapeInterceptor {
        public static final GetShapeInterceptor INSTANCE = new GetShapeInterceptor();
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            return ((DelegatingBlock) thisObj).shapeDelegate().value().getShape(thisObj, args);
        }
    }
    public static class GetCollisionShapeInterceptor {
        public static final GetCollisionShapeInterceptor INSTANCE = new GetCollisionShapeInterceptor();
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            return ((DelegatingBlock) thisObj).shapeDelegate().value().getCollisionShape(thisObj, args);
        }
    }
    public static class GetSupportShapeInterceptor {
        public static final GetSupportShapeInterceptor INSTANCE = new GetSupportShapeInterceptor();
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            return ((DelegatingBlock) thisObj).shapeDelegate().value().getSupportShape(thisObj, args);
        }
    }

    public static class IsPathFindableInterceptor extends BaseInterceptor {
        public static final IsPathFindableInterceptor INSTANCE = new IsPathFindableInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.isPathFindable(o, a, s); }
    }
    public static class MirrorInterceptor extends BaseInterceptor {
        public static final MirrorInterceptor INSTANCE = new MirrorInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.mirror(o, a, s); }
    }
    public static class RotateInterceptor extends BaseInterceptor {
        public static final RotateInterceptor INSTANCE = new RotateInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.rotate(o, a, s); }
    }
    public static class HasAnalogOutputSignalInterceptor extends BaseInterceptor {
        public static final HasAnalogOutputSignalInterceptor INSTANCE = new HasAnalogOutputSignalInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.hasAnalogOutputSignal(o, a); }
    }
    public static class GetAnalogOutputSignalInterceptor extends BaseInterceptor {
        public static final GetAnalogOutputSignalInterceptor INSTANCE = new GetAnalogOutputSignalInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.getAnalogOutputSignal(o, a); }
    }
    public static class TickInterceptor extends BaseInterceptor {
        public static final TickInterceptor INSTANCE = new TickInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.tick(o, a, s); return null; }
    }
    public static class RandomTickInterceptor extends BaseInterceptor {
        public static final RandomTickInterceptor INSTANCE = new RandomTickInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.randomTick(o, a, s); return null; }
    }
    public static class OnPlaceInterceptor extends BaseInterceptor {
        public static final OnPlaceInterceptor INSTANCE = new OnPlaceInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.onPlace(o, a, s); return null; }
    }
    public static class CanSurviveInterceptor extends BaseInterceptor {
        public static final CanSurviveInterceptor INSTANCE = new CanSurviveInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.canSurvive(o, a, s); }
    }
    public static class NeighborChangedInterceptor extends BaseInterceptor {
        public static final NeighborChangedInterceptor INSTANCE = new NeighborChangedInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.neighborChanged(o, a, s); return null; }
    }
    public static class UpdateShapeInterceptor extends BaseInterceptor {
        public static final UpdateShapeInterceptor INSTANCE = new UpdateShapeInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.updateShape(o, a, s); }
    }
    public static class PickUpBlockInterceptor extends BaseInterceptor {
        public static final PickUpBlockInterceptor INSTANCE = new PickUpBlockInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.pickupBlock(o, a, s); }
    }
    public static class PlaceLiquidInterceptor extends BaseInterceptor {
        public static final PlaceLiquidInterceptor INSTANCE = new PlaceLiquidInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.placeLiquid(o, a, s); }
    }
    public static class CanPlaceLiquidInterceptor extends BaseInterceptor {
        public static final CanPlaceLiquidInterceptor INSTANCE = new CanPlaceLiquidInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.canPlaceLiquid(o, a, s); }
    }
    public static class EntityInsideInterceptor extends BaseInterceptor {
        public static final EntityInsideInterceptor INSTANCE = new EntityInsideInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.entityInside(o, a, s); return null; }
    }
    public static class GetSignalInterceptor extends BaseInterceptor {
        public static final GetSignalInterceptor INSTANCE = new GetSignalInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.getSignal(o, a, s); }
    }
    public static class GetDirectSignalInterceptor extends BaseInterceptor {
        public static final GetDirectSignalInterceptor INSTANCE = new GetDirectSignalInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.getDirectSignal(o, a, s); }
    }
    public static class IsSignalSourceInterceptor extends BaseInterceptor {
        public static final IsSignalSourceInterceptor INSTANCE = new IsSignalSourceInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.isSignalSource(o, a, s); }
    }
    public static class PlayerWillDestroyInterceptor extends BaseInterceptor {
        public static final PlayerWillDestroyInterceptor INSTANCE = new PlayerWillDestroyInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { return b.playerWillDestroy(o, a, s); }
    }
    public static class SpawnAfterBreakInterceptor extends BaseInterceptor {
        public static final SpawnAfterBreakInterceptor INSTANCE = new SpawnAfterBreakInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.spawnAfterBreak(o, a, s); return null; }
    }
    public static class StepOnInterceptor extends BaseInterceptor {
        public static final StepOnInterceptor INSTANCE = new StepOnInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.stepOn(o, a, s); return null; }
    }
    public static class OnProjectileHitInterceptor extends BaseInterceptor {
        public static final OnProjectileHitInterceptor INSTANCE = new OnProjectileHitInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.onProjectileHit(o, a, s); return null; }
    }
    public static class OnRemoveInterceptor extends BaseInterceptor {
        public static final OnRemoveInterceptor INSTANCE = new OnRemoveInterceptor();
        protected Object handle(BlockBehavior b, Object o, Object[] a, Callable<Object> s) throws Exception { b.onRemove(o, a, s); return null; }
    }
}
