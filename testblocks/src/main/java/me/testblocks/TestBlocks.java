package me.testblocks;

import me.testblocks.api.BlockBehavior;
import me.testblocks.api.DelegatingBlock;
import me.testblocks.behavior.CustomShapeBehavior;
import me.testblocks.injector.Interceptors;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

public class TestBlocks extends JavaPlugin {

    private static Class<?> generatedBlockClass;

    @Override
    public void onEnable() {
        getLogger().info("Standalone TestBlocks (Final) is initializing...");

        try {
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockBehaviourClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour");
            Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
            Class<?> blockGetterClass = Class.forName("net.minecraft.world.level.BlockGetter");
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Class<?> collisionContextClass = Class.forName("net.minecraft.world.phys.shapes.CollisionContext");
            Class<?> shapesClass = Class.forName("net.minecraft.world.phys.shapes.Shapes");

            // Interceptor for getShape
            Method getShapeMethod = blockBehaviourClass.getDeclaredMethod("getShape",
                blockStateClass, blockGetterClass, blockPosClass, collisionContextClass);

            generatedBlockClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                    .subclass(blockClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("me.testblocks.injector.TestCustomBlock")
                    .defineField("behavior", Object.class, Visibility.PUBLIC)
                    .implement(DelegatingBlock.class)
                    .method(ElementMatchers.named("getBehavior")).intercept(FieldAccessor.ofField("behavior"))
                    .method(ElementMatchers.named("setBehavior")).intercept(FieldAccessor.ofField("behavior"))
                    .method(ElementMatchers.is(getShapeMethod))
                    .intercept(MethodDelegation.to(Interceptors.GetShapeInterceptor.class))
                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded();

            // Example shape
            Method boxMethod = shapesClass.getMethod("box", double.class, double.class, double.class, double.class, double.class, double.class);
            Object slabShape = boxMethod.invoke(null, 0, 0, 0, 16, 8, 16);

            registerBlock("test:cube", null);
            registerBlock("test:custom_slab", new CustomShapeBehavior(slabShape));

        } catch (Exception e) {
            getLogger().severe("Initialization failed: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("Standalone TestBlocks Final Stage initialized!");
    }

    private void registerBlock(String id, BlockBehavior behavior) throws Exception {
        unfreezeRegistry();
        try {
            Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");
            Method ofMethod = blockBehaviourPropertiesClass.getMethod("of");
            Object properties = ofMethod.invoke(null);

            Constructor<?> constructor = generatedBlockClass.getDeclaredConstructor(blockBehaviourPropertiesClass);
            Object blockInstance = constructor.newInstance(properties);

            if (behavior != null) {
                ((DelegatingBlock) blockInstance).setBehavior(behavior);
            }

            Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field blockRegistryField = builtInRegistriesClass.getDeclaredField("BLOCK");
            Object blockRegistry = blockRegistryField.get(null);

            Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
            Method parseMethod = resourceLocationClass.getMethod("parse", String.class);
            Object resourceLocation = parseMethod.invoke(null, id);

            Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
            Method registerMethod = registryClass.getMethod("register", registryClass, resourceLocationClass, Object.class);
            registerMethod.invoke(null, blockRegistry, resourceLocation, blockInstance);

            getLogger().info("Successfully registered " + id);
        } finally {
            freezeRegistry();
        }
    }

    private void unfreezeRegistry() throws Exception {
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field blockRegistryField = builtInRegistriesClass.getDeclaredField("BLOCK");
        Object blockRegistry = blockRegistryField.get(null);

        Class<?> mappedRegistryClass = Class.forName("net.minecraft.core.MappedRegistry");
        Field frozenField = mappedRegistryClass.getDeclaredField("frozen");
        frozenField.setAccessible(true);
        frozenField.set(blockRegistry, false);

        Field intrusiveHoldersField = mappedRegistryClass.getDeclaredField("unregisteredIntrusiveHolders");
        intrusiveHoldersField.setAccessible(true);
        intrusiveHoldersField.set(blockRegistry, new IdentityHashMap<>());
    }

    private void freezeRegistry() throws Exception {
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field blockRegistryField = builtInRegistriesClass.getDeclaredField("BLOCK");
        Object blockRegistry = blockRegistryField.get(null);

        Class<?> mappedRegistryClass = Class.forName("net.minecraft.core.MappedRegistry");
        Field frozenField = mappedRegistryClass.getDeclaredField("frozen");
        frozenField.setAccessible(true);
        frozenField.set(blockRegistry, true);
    }
}
