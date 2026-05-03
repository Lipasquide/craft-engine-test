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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class TestBlocks extends JavaPlugin implements CommandExecutor, Listener {

    private static Class<?> generatedBlockClass;
    private final Map<String, Object> registeredItems = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Standalone TestBlocks (STABLE MODE) is initializing...");

        try {
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockBehaviourPropertiesClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");

            // GOD MODE Level 999 STABILIZED GENERATOR
            generatedBlockClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                    .subclass(blockClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("me.testblocks.injector.TestCustomBlock")
                    .defineField("behavior", Object.class, Visibility.PUBLIC)
                    .implement(DelegatingBlock.class)
                    .method(ElementMatchers.named("getBehavior")).intercept(FieldAccessor.ofField("behavior"))
                    .method(ElementMatchers.named("setBehavior")).intercept(FieldAccessor.ofField("behavior"))

                    // Robust Method Matching
                    .method(ElementMatchers.named("getShape").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(Interceptors.GetShapeInterceptor.class))

                    .method(ElementMatchers.named("useWithoutItem").and(ElementMatchers.takesArguments(5)))
                    .intercept(MethodDelegation.to(Interceptors.UseInterceptor.class))

                    .method(ElementMatchers.named("onPlace").and(ElementMatchers.takesArguments(5)))
                    .intercept(MethodDelegation.to(Interceptors.OnPlaceInterceptor.class))

                    .method(ElementMatchers.named("neighborChanged").and(ElementMatchers.takesArguments(6)))
                    .intercept(MethodDelegation.to(Interceptors.NeighborChangedInterceptor.class))

                    .method(ElementMatchers.named("getDrops").and(ElementMatchers.takesArguments(2)))
                    .intercept(MethodDelegation.to(Interceptors.GetDropsInterceptor.class))

                    .method(ElementMatchers.named("getSoundType").and(ElementMatchers.takesArguments(1)))
                    .intercept(MethodDelegation.to(Interceptors.GetSoundTypeInterceptor.class))

                    .method(ElementMatchers.named("tick").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(Interceptors.TickInterceptor.class))

                    .method(ElementMatchers.named("randomTick").and(ElementMatchers.takesArguments(4)))
                    .intercept(MethodDelegation.to(Interceptors.RandomTickInterceptor.class))

                    // Physics
                    .method(ElementMatchers.named("getFriction").and(ElementMatchers.takesArguments(0)))
                    .intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.named("getExplosionResistance").and(ElementMatchers.takesArguments(0)))
                    .intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.named("getJumpFactor").and(ElementMatchers.takesArguments(0)))
                    .intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.named("getSpeedFactor").and(ElementMatchers.takesArguments(0)))
                    .intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))

                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded();

            // Register blocks
            registerBlock("test:cube", null);
            registerBlock("test:slab", ShapeFactory.slab());
            registerBlock("test:rich_ore", new CustomDropBehavior(Material.DIAMOND));

            getCommand("testblocks").setExecutor(this);
            Bukkit.getPluginManager().registerEvents(this, this);

        } catch (Exception e) {
            getLogger().severe("Stabilization failed: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("STABLE Standalone TestBlocks initialized!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PacketInterceptor.inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        VisualMappingManager.removePlayer(event.getPlayer().getUniqueId());
    }

    private void registerBlock(String id, BlockBehavior behavior) throws Exception {
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

            registerInNms("BLOCK", id, blockInstance);
            registerBlockItem(id, blockInstance);

            getLogger().info("Successfully registered " + id);
        } finally {
            freezeRegistry("BLOCK");
        }
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

            // Safe field search for 'frozen'
            try {
                Field frozenField = mappedRegistryClass.getDeclaredField("frozen");
                frozenField.setAccessible(true);
                frozenField.setBoolean(registry, false);
            } catch (NoSuchFieldException e) {
                getLogger().warning("NMS 'frozen' field not found in " + registryName + " - possibly version mismatch.");
            }

            // Intrusive holders map
            try {
                Field intrusiveHoldersField = mappedRegistryClass.getDeclaredField("unregisteredIntrusiveHolders");
                intrusiveHoldersField.setAccessible(true);
                intrusiveHoldersField.set(registry, new IdentityHashMap<>());
            } catch (NoSuchFieldException ignore) {}

        } catch (Exception e) {
            getLogger().severe("Failed to unfreeze registry " + registryName + ": " + e.getMessage());
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
                player.sendMessage("Block not found!");
                return true;
            }

            try {
                // Use non-version-locked CraftItemStack (via reflection on player class loader)
                Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
                // If it fails, fallback to standard versioning search

                Method asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", Class.forName("net.minecraft.world.item.ItemStack"));

                Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                Constructor<?> nmsItemStackConstructor = nmsItemStackClass.getConstructor(Class.forName("net.minecraft.world.item.Item"));
                Object nmsItemStack = nmsItemStackConstructor.newInstance(nmsItem);

                ItemStack bukkitStack = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
                player.getInventory().addItem(bukkitStack);
                player.sendMessage("§aGave " + id);
            } catch (Exception e) {
                player.sendMessage("Error: " + e.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            var target = player.getTargetBlockExact(5);
            if (target == null) {
                player.sendMessage("Block yok");
                return true;
            }

            try {
                // Robust block handle access
                Method getHandle = target.getClass().getMethod("getHandle");
                Object nmsState = getHandle.invoke(target);

                Method getBlock = nmsState.getClass().getMethod("getBlock");
                Object nmsBlock = getBlock.invoke(nmsState);

                if (nmsBlock instanceof DelegatingBlock db) {
                    player.sendMessage("§b[Custom Block Found!]");
                    Object behavior = db.getBehavior();
                    player.sendMessage("§7Behavior: §f" + (behavior != null ? behavior.getClass().getSimpleName() : "None"));
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
                player.sendMessage("Hata: " + e.getMessage());
            }
            return true;
        }

        return true;
    }
}
