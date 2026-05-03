package me.testblocks;

import me.testblocks.api.BlockBehavior;
import me.testblocks.api.DelegatingBlock;
import me.testblocks.behavior.*;
import me.testblocks.injector.Interceptors;
import me.testblocks.network.PacketInterceptor;
import me.testblocks.network.VisualMappingManager;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GOD MODE Level 999 - THE FINAL DOT .
 * A 100% Standalone Custom Block Engine for Minecraft 1.21.1+
 */
public class TestBlocks extends JavaPlugin implements CommandExecutor, Listener {

    private static Class<?> generatedBlockClass;
    private final Map<String, Object> registeredItems = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Initializing GOD MODE Level 999 - Independent Block Engine...");

        try {
            // NMS Class References
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");

            // ByteBuddy Generator: Create a block that Minecraft treats as native
            generatedBlockClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                    .subclass(blockClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("me.testblocks.injector.TestCustomBlock")
                    .defineField("behavior", Object.class, Visibility.PUBLIC)
                    .implement(DelegatingBlock.class)
                    .method(ElementMatchers.named("getBehavior")).intercept(FieldAccessor.ofField("behavior"))
                    .method(ElementMatchers.named("setBehavior")).intercept(FieldAccessor.ofField("behavior"))

                    // Hook into NMS methods for full control
                    .method(ElementMatchers.named("getShape").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(Interceptors.GetShapeInterceptor.class))
                    .method(ElementMatchers.named("useWithoutItem").and(ElementMatchers.takesArguments(5)))
                    .intercept(MethodDelegation.to(Interceptors.UseInterceptor.class))
                    .method(ElementMatchers.named("getDrops").and(ElementMatchers.takesArguments(2)))
                    .intercept(MethodDelegation.to(Interceptors.GetDropsInterceptor.class))
                    .method(ElementMatchers.named("getSoundType").and(ElementMatchers.takesArguments(1)))
                    .intercept(MethodDelegation.to(Interceptors.GetSoundTypeInterceptor.class))

                    // Physics Hooks
                    .method(ElementMatchers.named("getFriction")).intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.named("getSpeedFactor")).intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.named("getJumpFactor")).intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))

                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded();

            // Register blocks with visual mapping
            registerBlock("test:cube", null, "minecraft:stone");
            registerBlock("test:slab", ShapeFactory.slab(), "minecraft:stone_slab");
            registerBlock("test:rich_ore", new CustomDropBehavior(Material.DIAMOND), "minecraft:diamond_ore");
            registerBlock("test:speed_block", new PhysicsBlockBehavior(0.1f, 2.0f, 1.2f), "minecraft:blue_ice");

            // Setup Commands and Events
            getCommand("testblocks").setExecutor(this);
            Bukkit.getPluginManager().registerEvents(this, this);

            getLogger().info("GOD MODE Level 999 is now ACTIVE. Bestcodemode = ON.");

        } catch (Exception e) {
            getLogger().severe("CRITICAL: Failed to initialize the custom block engine!");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PacketInterceptor.inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        VisualMappingManager.removePlayer(event.getPlayer().getUniqueId());
    }

    /**
     * The master registration method.
     */
    private void registerBlock(String id, BlockBehavior behavior, String vanillaSlot) throws Exception {
        unfreezeRegistry("BLOCK");
        try {
            Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");
            Method ofMethod = blockBehaviourPropertiesClass.getMethod("of");
            Object properties = ofMethod.invoke(null);

            Constructor<?> constructor = generatedBlockClass.getDeclaredConstructor(blockBehaviourPropertiesClass);
            Object blockInstance = constructor.newInstance(properties);

            if (behavior != null) {
                ((DelegatingBlock) blockInstance).setBehavior(behavior);
            }

            // --- NMS Property Injection (Facing example) ---
            Class<?> stateDefinitionBuilderClass = Class.forName("net.minecraft.world.level.block.state.StateDefinition$Builder");
            Object builder = stateDefinitionBuilderClass.getConstructor(Class.forName("net.minecraft.world.level.block.Block")).newInstance(blockInstance);

            // This is where CraftEngine adds Properties like HORIZONTAL_FACING
            // In this standalone version, we maintain a simplified state but allow full behavior control

            registerInNms("BLOCK", id, blockInstance);
            registerBlockItem(id, blockInstance);

            // Map the custom ID to a vanilla slot for the PacketInterceptor
            mapVisualSlot(blockInstance, vanillaSlot);

            getLogger().info("Registered " + id + " [" + (behavior != null ? behavior.getClass().getSimpleName() : "Standard") + "]");
        } finally {
            freezeRegistry("BLOCK");
        }
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
        Method parseMethod = resourceLocationClass.getMethod("parse", String.class);
        Object slotLocation = parseMethod.invoke(null, vanillaSlot);

        Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
        Method getMethod = registryClass.getMethod("get", resourceLocationClass);
        Object vanillaBlock = getMethod.invoke(registry, slotLocation);
        Object vanillaState = defaultBlockStateMethod.invoke(vanillaBlock);
        int vanillaId = (int) getIdMethod.invoke(null, vanillaState);

        VisualMappingManager.register(customId, vanillaId);
    }

    private void registerBlockItem(String id, Object blockInstance) throws Exception {
        unfreezeRegistry("ITEM");
        try {
            Class<?> blockItemClass = Class.forName("net.minecraft.world.item.BlockItem");
            Class<?> itemPropertiesClass = Class.forName("net.minecraft.world.item.Item$Properties");
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");

            Object itemProperties = itemPropertiesClass.getConstructor().newInstance();
            Constructor<?> itemConstructor = blockItemClass.getConstructor(blockClass, itemPropertiesClass);
            Object itemInstance = itemConstructor.newInstance(blockInstance, itemProperties);

            registerInNms("ITEM", id, itemInstance);
            registeredItems.put(id, itemInstance);
        } finally {
            freezeRegistry("ITEM");
        }
    }

    private void registerInNms(String registryName, String id, Object instance) throws Exception {
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field registryField = builtInRegistriesClass.getDeclaredField(registryName);
        Object registry = registryField.get(null);

        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
        Method parseMethod = resourceLocationClass.getMethod("parse", String.class);
        Object resourceLocation = parseMethod.invoke(null, id);

        Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
        Method registerMethod = registryClass.getMethod("register", registryClass, resourceLocationClass, Object.class);
        registerMethod.invoke(null, registry, resourceLocation, instance);
    }

    private void unfreezeRegistry(String registryName) {
        try {
            Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field registryField = builtInRegistriesClass.getDeclaredField(registryName);
            Object registry = registryField.get(null);
            Class<?> mappedRegistryClass = Class.forName("net.minecraft.core.MappedRegistry");

            Field frozenField = mappedRegistryClass.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.setBoolean(registry, false);

            Field intrusiveHoldersField = mappedRegistryClass.getDeclaredField("unregisteredIntrusiveHolders");
            intrusiveHoldersField.setAccessible(true);
            intrusiveHoldersField.set(registry, new IdentityHashMap<>());
        } catch (Exception e) {
            getLogger().warning("Registry Unfreeze Warning: " + e.getMessage());
        }
    }

    private void freezeRegistry(String registryName) {
        try {
            Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field registryField = builtInRegistriesClass.getDeclaredField(registryName);
            Object registry = registryField.get(null);
            Class<?> mappedRegistryClass = Class.forName("net.minecraft.core.MappedRegistry");
            Field frozenField = mappedRegistryClass.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.setBoolean(registry, true);
        } catch (Exception ignore) {}
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
                Constructor<?> nmsItemStackConstructor = nmsItemStackClass.getConstructor(Class.forName("net.minecraft.world.item.Item"));
                Object nmsItemStack = nmsItemStackConstructor.newInstance(nmsItem);

                ItemStack bukkitStack = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
                player.getInventory().addItem(bukkitStack);
                player.sendMessage("§aGave " + id);
            } catch (Exception e) {
                player.sendMessage("§cError: " + e.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            var target = player.getTargetBlockExact(5);
            if (target == null) {
                player.sendMessage("§cBlock yok");
                return true;
            }

            try {
                Method getHandle = target.getClass().getMethod("getHandle");
                Object nmsState = getHandle.invoke(target);
                Method getBlock = nmsState.getClass().getMethod("getBlock");
                Object nmsBlock = getBlock.invoke(nmsState);

                if (nmsBlock instanceof DelegatingBlock db) {
                    player.sendMessage("§b§l[Custom Block Found]");
                    Object behavior = db.getBehavior();
                    player.sendMessage("§7Behavior: §f" + (behavior != null ? behavior.getClass().getSimpleName() : "Standard"));
                } else {
                    player.sendMessage("§8[Vanilla Block]");
                }

                Class<?> builtInRegistries = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
                Field blockField = builtInRegistries.getDeclaredField("BLOCK");
                Object registry = blockField.get(null);
                Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
                Method getKey = registryClass.getMethod("getKey", Object.class);
                Object key = getKey.invoke(registry, nmsBlock);

                player.sendMessage("§7Registry ID: §f" + key.toString());
            } catch (Exception e) {
                player.sendMessage("§cHata: " + e.getMessage());
            }
            return true;
        }

        return true;
    }
}
