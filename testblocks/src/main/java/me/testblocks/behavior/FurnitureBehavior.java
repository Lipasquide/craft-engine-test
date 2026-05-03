package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.Collection;
import java.util.concurrent.Callable;

public class FurnitureBehavior extends BlockBehavior {
    private final Material itemMaterial;
    public FurnitureBehavior(Material itemMaterial) { this.itemMaterial = itemMaterial; }

    @Override
    public void onPlace(Object blockState, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[1];
        Object blockPos = args[2];
        int x = (int) blockPos.getClass().getMethod("getX").invoke(blockPos);
        int y = (int) blockPos.getClass().getMethod("getY").invoke(blockPos);
        int z = (int) blockPos.getClass().getMethod("getZ").invoke(blockPos);
        org.bukkit.World world = (org.bukkit.World) level.getClass().getMethod("getWorld").invoke(level);

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("TestBlocks"), () -> {
            Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
            ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class);
            display.setItemStack(new ItemStack(itemMaterial));
            display.setTransformation(new org.bukkit.util.Transformation(new Vector3f(0,0,0), new Quaternionf(), new Vector3f(1,1,1), new Quaternionf()));
            display.addScoreboardTag("custom_furniture");
        });
        superMethod.call();
    }

    @Override
    public void neighborChanged(Object blockState, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[1];
        Object blockPos = args[2];
        removeDisplay(level, blockPos);
        superMethod.call();
    }

    private void removeDisplay(Object level, Object blockPos) {
        try {
            int x = (int) blockPos.getClass().getMethod("getX").invoke(blockPos);
            int y = (int) blockPos.getClass().getMethod("getY").invoke(blockPos);
            int z = (int) blockPos.getClass().getMethod("getZ").invoke(blockPos);
            org.bukkit.World world = (org.bukkit.World) level.getClass().getMethod("getWorld").invoke(level);
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("TestBlocks"), () -> {
                Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, 0.1, 0.1, 0.1);
                for (Entity entity : entities) {
                    if (entity instanceof ItemDisplay && entity.getScoreboardTags().contains("custom_furniture")) {
                        entity.remove();
                    }
                }
            });
        } catch (Exception ignore) {}
    }
}
