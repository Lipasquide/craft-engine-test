package me.testblocks;

import me.testblocks.api.DelegatingBlock;
import me.testblocks.util.BuiltInRegistriesProxy;
import me.testblocks.util.MappedRegistryProxy;
import me.testblocks.util.RegistryProxy;
import me.testblocks.util.ResourceLocationProxy;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.IdentityHashMap;

public class TestBlocks extends JavaPlugin {

    private static Class<?> generatedBlockClass;

    @Override
    public void onEnable() {
        getLogger().info("Standalone TestBlocks is initializing...");

        try {
            // 1. Generate the block class using ByteBuddy
            // We subclass net.minecraft.world.level.block.Block
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");

            generatedBlockClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                    .subclass(blockClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("me.testblocks.injector.TestCustomBlock")
                    .defineField("behavior", Object.class, Visibility.PUBLIC)
                    .implement(DelegatingBlock.class)
                    .method(ElementMatchers.named("getBehavior"))
                    .intercept(FieldAccessor.ofField("behavior"))
                    .method(ElementMatchers.named("setBehavior"))
                    .intercept(FieldAccessor.ofField("behavior"))
                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded();

            // 2. Register sample blocks
            registerBlock("test:cube");
            registerBlock("test:stairs");

        } catch (Exception e) {
            getLogger().severe("Initialization failed: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("Standalone TestBlocks initialized!");
    }

    private void registerBlock(String id) throws Exception {
        unfreezeRegistry();
        try {
            // Create properties
            Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");
            java.lang.reflect.Method ofMethod = blockBehaviourPropertiesClass.getMethod("of");
            Object properties = ofMethod.invoke(null);

            // Create block instance
            Constructor<?> constructor = generatedBlockClass.getDeclaredConstructor(blockBehaviourPropertiesClass);
            Object blockInstance = constructor.newInstance(properties);

            // Register in Minecraft
            Object registry = BuiltInRegistriesProxy.INSTANCE.BLOCK();
            Object resourceLocation = ResourceLocationProxy.INSTANCE.parse(id);

            RegistryProxy.INSTANCE.register(registry, resourceLocation, blockInstance);

            getLogger().info("Successfully registered " + id);
        } finally {
            freezeRegistry();
        }
    }

    private void unfreezeRegistry() {
        Object registry = BuiltInRegistriesProxy.INSTANCE.BLOCK();
        MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
        MappedRegistryProxy.INSTANCE.setUnregisteredIntrusiveHolders(registry, new IdentityHashMap<>());
    }

    private void freezeRegistry() {
        Object registry = BuiltInRegistriesProxy.INSTANCE.BLOCK();
        MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
    }
}
