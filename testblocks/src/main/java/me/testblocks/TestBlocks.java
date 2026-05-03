package me.testblocks;

import me.testblocks.api.BlockBehavior;
import me.testblocks.api.DelegatingBlock;
import me.testblocks.behavior.CustomDropBehavior;
import me.testblocks.behavior.CustomShapeBehavior;
import me.testblocks.behavior.FurnitureBehavior;
import me.testblocks.behavior.PhysicsBlockBehavior;
import me.testblocks.behavior.ComplexShapeBehavior;
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
        getLogger().info("Standalone TestBlocks (GOD MODE Level 999) is initializing...");

        try {
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockBehaviourClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour");
            Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            Class<?> serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");
            Class<?> blockGetterClass = Class.forName("net.minecraft.world.level.BlockGetter");
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Class<?> collisionContextClass = Class.forName("net.minecraft.world.phys.shapes.CollisionContext");
            Class<?> playerClass = Class.forName("net.minecraft.world.entity.player.Player");
            Class<?> blockHitResultClass = Class.forName("net.minecraft.world.phys.BlockHitResult");
            Class<?> lootParamsBuilderClass = Class.forName("net.minecraft.world.level.storage.loot.LootParams$Builder");
            Class<?> randomSourceClass = Class.forName("net.minecraft.util.RandomSource");
            Class<?> soundTypeClass = Class.forName("net.minecraft.world.level.block.SoundType");

            // Define methods to intercept
            Method getShapeMethod = blockBehaviourClass.getDeclaredMethod("getShape",
                blockStateClass, blockGetterClass, blockPosClass, collisionContextClass);
            Method useMethod = blockBehaviourClass.getDeclaredMethod("useWithoutItem",
                blockStateClass, levelClass, blockPosClass, playerClass, blockHitResultClass);
            Method onPlaceMethod = blockBehaviourClass.getDeclaredMethod("onPlace",
                blockStateClass, levelClass, blockPosClass, blockStateClass, boolean.class);
            Method neighborChangedMethod = blockBehaviourClass.getDeclaredMethod("neighborChanged",
                blockStateClass, levelClass, blockPosClass, blockClass, blockPosClass, boolean.class);
            Method getDropsMethod = blockBehaviourClass.getDeclaredMethod("getDrops",
                blockStateClass, lootParamsBuilderClass);
            Method getSoundTypeMethod = blockBehaviourClass.getDeclaredMethod("getSoundType",
                blockStateClass);
            Method tickMethod = blockBehaviourClass.getDeclaredMethod("tick",
                blockStateClass, serverLevelClass, blockPosClass, randomSourceClass);
            Method randomTickMethod = blockBehaviourClass.getDeclaredMethod("randomTick",
                blockStateClass, serverLevelClass, blockPosClass, randomSourceClass);

            // Physics
            Method getFrictionMethod = blockClass.getDeclaredMethod("getFriction");
            Method getExplosionResistanceMethod = blockClass.getDeclaredMethod("getExplosionResistance");
            Method getSpeedFactorMethod = blockClass.getDeclaredMethod("getSpeedFactor");
            Method getJumpFactorMethod = blockClass.getDeclaredMethod("getJumpFactor");

            // GOD MODE Level 999 GENERATOR
            generatedBlockClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                    .subclass(blockClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("me.testblocks.injector.TestCustomBlock")
                    .defineField("behavior", Object.class, Visibility.PUBLIC)
                    .implement(DelegatingBlock.class)
                    .method(ElementMatchers.named("getBehavior")).intercept(FieldAccessor.ofField("behavior"))
                    .method(ElementMatchers.named("setBehavior")).intercept(FieldAccessor.ofField("behavior"))
                    .method(ElementMatchers.is(getShapeMethod)).intercept(MethodDelegation.to(Interceptors.GetShapeInterceptor.class))
                    .method(ElementMatchers.is(useMethod)).intercept(MethodDelegation.to(Interceptors.UseInterceptor.class))
                    .method(ElementMatchers.is(onPlaceMethod)).intercept(MethodDelegation.to(Interceptors.OnPlaceInterceptor.class))
                    .method(ElementMatchers.is(neighborChangedMethod)).intercept(MethodDelegation.to(Interceptors.NeighborChangedInterceptor.class))
                    .method(ElementMatchers.is(getDropsMethod)).intercept(MethodDelegation.to(Interceptors.GetDropsInterceptor.class))
                    .method(ElementMatchers.is(getSoundTypeMethod)).intercept(MethodDelegation.to(Interceptors.GetSoundTypeInterceptor.class))
                    .method(ElementMatchers.is(tickMethod)).intercept(MethodDelegation.to(Interceptors.TickInterceptor.class))
                    .method(ElementMatchers.is(randomTickMethod)).intercept(MethodDelegation.to(Interceptors.RandomTickInterceptor.class))
                    .method(ElementMatchers.is(getFrictionMethod)).intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.is(getExplosionResistanceMethod)).intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.is(getJumpFactorMethod)).intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .method(ElementMatchers.is(getSpeedFactorMethod)).intercept(MethodDelegation.to(Interceptors.PhysicsInterceptor.class))
                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded();

            // Register blocks
            registerBlock("test:cube", null);

            Class<?> shapesClass = Class.forName("net.minecraft.world.phys.shapes.Shapes");
            Method boxMethod = shapesClass.getMethod("box", double.class, double.class, double.class, double.class, double.class, double.class);
            Object slabShape = boxMethod.invoke(null, 0, 0, 0, 16, 8, 16);
            registerBlock("test:slab", new CustomShapeBehavior(slabShape));

            // Complex Shaped Block (Fence-like)
            Object fenceShape = boxMethod.invoke(null, 6, 0, 6, 10, 16, 10);
            Object woodSound = soundTypeClass.getField("WOOD").get(null);
            registerBlock("test:post", new ComplexShapeBehavior(fenceShape, woodSound));

            getCommand("testblocks").setExecutor(this);
            Bukkit.getPluginManager().registerEvents(this, this);

        } catch (Exception e) {
            getLogger().severe("Initialization failed: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("GOD MODE Level 999 Standalone TestBlocks initialized!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PacketInterceptor.inject(event.getPlayer(), null);
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

    private void unfreezeRegistry(String registryName) throws Exception {
        Class<?> builtInRegistriesClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field registryField = builtInRegistriesClass.getDeclaredField(registryName);
        Object registry = registryField.get(null);

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
        Field registryField = builtInRegistriesClass.getDeclaredField(registryName);
        Object registry = registryField.get(null);

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
                player.sendMessage("Block not found!");
                return true;
            }

            try {
                Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack");
                Method asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", Class.forName("net.minecraft.world.item.ItemStack"));

                Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                Constructor<?> nmsItemStackConstructor = nmsItemStackClass.getConstructor(Class.forName("net.minecraft.world.item.Item"));
                Object nmsItemStack = nmsItemStackConstructor.newInstance(nmsItem);

                ItemStack bukkitStack = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
                player.getInventory().addItem(bukkitStack);
                player.sendMessage("Gave " + id);
            } catch (Exception e) {
                player.sendMessage("Error: " + e.getMessage());
                e.printStackTrace();
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
                Object craftBlock = target;
                Method getHandle = craftBlock.getClass().getMethod("getHandle");
                Object nmsState = getHandle.invoke(craftBlock);

                Method getBlock = nmsState.getClass().getMethod("getBlock");
                Object nmsBlock = getBlock.invoke(nmsState);

                if (nmsBlock instanceof DelegatingBlock db) {
                    player.sendMessage("§aCustom block!");
                    Object behavior = db.getBehavior();
                    player.sendMessage("Behavior: " + (behavior != null ? behavior.getClass().getSimpleName() : "null"));
                } else {
                    player.sendMessage("§cVanilla block");
                }

                Class<?> builtInRegistries = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
                Field blockField = builtInRegistries.getDeclaredField("BLOCK");
                Object registry = blockField.get(null);

                Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
                Method getKey = registryClass.getMethod("getKey", Object.class);
                Object key = getKey.invoke(registry, nmsBlock);

                player.sendMessage("ID: " + key.toString());

            } catch (Exception e) {
                player.sendMessage("Hata: " + e.getMessage());
                e.printStackTrace();
            }

            return true;
        }

        return true;
    }
}
