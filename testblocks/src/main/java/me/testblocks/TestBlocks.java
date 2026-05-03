package me.testblocks;

import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehaviors;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.block.DelegatingBlock;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.bukkit.plugin.injector.BlockGenerator;
import net.momirealms.craftengine.bukkit.plugin.injector.BlockStateGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.IdentityHashMap;

public class TestBlocks extends JavaPlugin {

    @Override
    public void onEnable() {
        // Core voodoo initialization
        BlockGenerator.init();
        BlockStateGenerator.init();
        // Initialize CraftEngine's built-in behaviors so we can use them
        BukkitBlockBehaviors.init();

        getLogger().info("TestBlocks is initializing...");

        // 1. Standart Cube Block (Empty Behavior)
        registerBlock(Key.of("test", "cube"), EmptyBlockBehavior.INSTANCE);

        // 2. Shaped Block Example: Door
        // Note: Real implementation would require defining properties (facing, half, etc.)
        // But here we demonstrate the registration with behavior.
        // In CraftEngine, these are wired up via .yml configs.

        getLogger().info("TestBlocks initialized!");
    }

    private void registerBlock(Key id, BlockBehavior behavior) {
        unfreezeRegistry();
        try {
            DelegatingBlock customBlock = BlockGenerator.generateBlock(id);
            Object identifier = KeyUtils.toIdentifier(id);
            RegistryProxy.INSTANCE.registerForHolder$1(BuiltInRegistriesProxy.BLOCK, identifier, customBlock);
            customBlock.behaviorDelegate().bindValue(behavior);
            getLogger().info("Registered: " + id.asString());
        } finally {
            freezeRegistry();
        }
    }

    private void unfreezeRegistry() {
        MappedRegistryProxy.INSTANCE.setFrozen(BuiltInRegistriesProxy.BLOCK, false);
        MappedRegistryProxy.INSTANCE.setUnregisteredIntrusiveHolders(BuiltInRegistriesProxy.BLOCK, new IdentityHashMap<>());
    }

    private void freezeRegistry() {
        MappedRegistryProxy.INSTANCE.setFrozen(BuiltInRegistriesProxy.BLOCK, true);
    }
}
