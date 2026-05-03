package me.testblocks;

import me.testblocks.api.*;
import me.testblocks.behavior.*;
import me.testblocks.injector.BlockGenerator;
import me.testblocks.injector.BlockStateGenerator;
import me.testblocks.network.PacketInterceptor;
import me.testblocks.network.VisualMappingManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.HashMap;

public class TestBlocks extends JavaPlugin implements CommandExecutor, Listener {

    private final Map<String, Object> registeredItems = new HashMap<>();

    @Override
    public void onLoad() {
        try {
            BlockStateGenerator.init();
            BlockGenerator.init();

            registerBlock("test:cube", null, "minecraft:stone");
            registerBlock("test:slab", ShapeFactory.slab(), "minecraft:stone_slab");
            registerBlock("test:rich_ore", new CustomDropBehavior(Material.DIAMOND), "minecraft:diamond_ore");
            registerBlock("test:furniture", new FurnitureBehavior(Material.LAPIS_BLOCK), "minecraft:barrier");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        getCommand("testblocks").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PacketInterceptor.inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        VisualMappingManager.removePlayer(event.getPlayer().getUniqueId());
    }

    private void registerBlock(String id, BlockBehavior behavior, String vanillaSlot) throws Exception {
        unfreezeRegistry("BLOCK");
        try {
            Object blockInstance;
            try {
                blockInstance = BlockGenerator.generateBlock(id);
            } catch (Throwable t) {
                throw new Exception(t);
            }
            if (behavior != null) {
                ((DelegatingBlock) blockInstance).behaviorDelegate().bindValue(behavior);
            }

            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Method defaultBlockStateMethod = blockClass.getMethod("defaultBlockState");
            Object defaultBlockState = defaultBlockStateMethod.invoke(blockInstance);

            // NMS Field Injection
            injectFields(blockInstance, defaultBlockState);

            registerInNms("BLOCK", id, blockInstance);

            // BLOCK_STATE_REGISTRY injection
            Field blockStateRegistryField = blockClass.getDeclaredField("BLOCK_STATE_REGISTRY");
            blockStateRegistryField.setAccessible(true);
            Object blockStateRegistry = blockStateRegistryField.get(null);
            Method addMethod = blockStateRegistry.getClass().getMethod("add", Object.class);
            addMethod.invoke(blockStateRegistry, defaultBlockState);

            registerBlockItem(id, blockInstance);
            mapVisualSlot(blockInstance, vanillaSlot);

            getLogger().info("Successfully registered " + id);
        } finally {
            freezeRegistry("BLOCK");
        }
    }

    private void injectFields(Object block, Object state) throws Exception {
        // State fields
        Class<?> stateBaseClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase");
        setField(state, stateBaseClass, "lightEmission", 0);
        setField(state, stateBaseClass, "destroySpeed", 1.5f);
        setField(state, stateBaseClass, "canOcclude", true);

        // FluidState
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field fluidRegistryField = builtInRegistriesClass.getDeclaredField("FLUID");
        Object fluidRegistry = fluidRegistryField.get(null);
        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
        Object emptyFluidLoc = resourceLocationClass.getMethod("parse", String.class).invoke(null, "minecraft:empty");
        Object emptyFluid = Class.forName("net.minecraft.core.Registry").getMethod("get", resourceLocationClass).invoke(fluidRegistry, emptyFluidLoc);
        Object fluidState = emptyFluid.getClass().getMethod("defaultFluidState").invoke(emptyFluid);
        setField(state, stateBaseClass, "fluidState", fluidState);

        // Block fields
        Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
        setField(block, blockClass, "explosionResistance", 6.0f);
        setField(block, blockClass, "friction", 0.6f);
        setField(block, blockClass, "speedFactor", 1.0f);
        setField(block, blockClass, "jumpFactor", 1.0f);

        // initCache
        Method initCache = state.getClass().getMethod("initCache");
        initCache.setAccessible(true);
        initCache.invoke(state);
    }

    private void setField(Object obj, Class<?> clazz, String name, Object value) throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void mapVisualSlot(Object blockInstance, String vanillaSlot) throws Exception {
        Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
        Method getIdMethod = blockClass.getMethod("getId", Class.forName("net.minecraft.world.level.block.state.BlockState"));
        Method defaultBlockStateMethod = blockClass.getMethod("defaultBlockState");

        Object customState = defaultBlockStateMethod.invoke(blockInstance);
        int customId = (int) getIdMethod.invoke(null, customState);

        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field blockRegistryField = builtInRegistriesClass.getDeclaredField("BLOCK");
        Object registry = blockRegistryField.get(null);

        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
        Object slotLocation = resourceLocationClass.getMethod("parse", String.class).invoke(null, vanillaSlot);

        Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
        Object vanillaBlock = registryClass.getMethod("get", resourceLocationClass).invoke(registry, slotLocation);
        Object vanillaState = defaultBlockStateMethod.invoke(vanillaBlock);
        int vanillaId = (int) getIdMethod.invoke(null, vanillaState);

        VisualMappingManager.register(customId, vanillaId);

        // Update shapeHolder to match vanilla slot shape
        ((DelegatingBlock) blockInstance).shapeDelegate().bindValue(new DefaultBlockShape(vanillaState));
    }

    private void registerBlockItem(String id, Object blockInstance) throws Exception {
        unfreezeRegistry("ITEM");
        try {
            Class<?> blockItemClass = Class.forName("net.minecraft.world.item.BlockItem");
            Class<?> itemPropertiesClass = Class.forName("net.minecraft.world.item.Item$Properties");
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");

            Object itemProperties = itemPropertiesClass.getConstructor().newInstance();
            Object itemInstance = blockItemClass.getConstructor(blockClass, itemPropertiesClass).newInstance(blockInstance, itemProperties);

            registerInNms("ITEM", id, itemInstance);
            registeredItems.put(id, itemInstance);
        } finally {
            freezeRegistry("ITEM");
        }
    }

    private void registerInNms(String registryName, String id, Object instance) throws Exception {
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Object registry = builtInRegistriesClass.getDeclaredField(registryName).get(null);
        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
        Object resourceLocation = resourceLocationClass.getMethod("parse", String.class).invoke(null, id);
        Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
        registryClass.getMethod("register", registryClass, resourceLocationClass, Object.class).invoke(null, registry, resourceLocation, instance);
    }

    private void unfreezeRegistry(String registryName) throws Exception {
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Object registry = builtInRegistriesClass.getDeclaredField(registryName).get(null);
        Class<?> mappedRegistryClass = Class.forName("net.minecraft.core.MappedRegistry");
        Field frozenField = mappedRegistryClass.getDeclaredField("frozen");
        frozenField.setAccessible(true);
        frozenField.set(registry, false);

        Field intrusiveHoldersField = mappedRegistryClass.getDeclaredField("unregisteredIntrusiveHolders");
        intrusiveHoldersField.setAccessible(true);
        intrusiveHoldersField.set(registry, new IdentityHashMap<>());
    }

    private void freezeRegistry(String registryName) throws Exception {
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Object registry = builtInRegistriesClass.getDeclaredField(registryName).get(null);
        Class<?> mappedRegistryClass = Class.forName("net.minecraft.core.MappedRegistry");
        Field frozenField = mappedRegistryClass.getDeclaredField("frozen");
        frozenField.setAccessible(true);
        frozenField.set(registry, true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length < 1) return false;

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) return false;
            String id = args[1];
            if (!id.contains(":")) id = "test:" + id;
            Object nmsItem = registeredItems.get(id);
            if (nmsItem == null) {
                player.sendMessage("§cBlock not found!");
                return true;
            }
            try {
                Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
                Method asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", Class.forName("net.minecraft.world.item.ItemStack"));
                Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                Object nmsItemStack = nmsItemStackClass.getConstructor(Class.forName("net.minecraft.world.item.Item")).newInstance(nmsItem);
                ItemStack bukkitStack = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
                player.getInventory().addItem(bukkitStack);
                player.sendMessage("§aGave " + id);
            } catch (Exception e) { player.sendMessage("§cError: " + e.getMessage()); }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            var target = player.getTargetBlockExact(5);
            if (target == null) return true;
            try {
                Method getHandle = target.getClass().getMethod("getHandle");
                Object nmsState = getHandle.invoke(target);
                Object nmsBlock = nmsState.getClass().getMethod("getBlock").invoke(nmsState);
                if (nmsBlock instanceof DelegatingBlock db) {
                    player.sendMessage("§b§l[Mini CraftEngine]");
                    Object behavior = db.behaviorDelegate().value();
                    player.sendMessage("§7Behavior: §f" + (behavior != null ? behavior.getClass().getSimpleName() : "Standard"));
                } else { player.sendMessage("§8[Vanilla Block]"); }
                Object registry = Class.forName("net.minecraft.core.registries.BuiltInRegistries").getDeclaredField("BLOCK").get(null);
                Object key = Class.forName("net.minecraft.core.Registry").getMethod("getKey", Object.class).invoke(registry, nmsBlock);
                player.sendMessage("§7Registry ID: §f" + key.toString());
            } catch (Exception e) { player.sendMessage("§cHata: " + e.getMessage()); }
            return true;
        }
        return true;
    }
}
