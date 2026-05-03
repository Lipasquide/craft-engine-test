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

public class FurnitureBehavior implements BlockBehavior {

    private final Material itemMaterial;

    public FurnitureBehavior(Material itemMaterial) {
        this.itemMaterial = itemMaterial;
    }

    @Override
    public void onPlace(Object blockState, Object level, Object blockPos, Object oldState, boolean isMoving) {
        try {
            int x = (int) blockPos.getClass().getMethod("getX").invoke(blockPos);
            int y = (int) blockPos.getClass().getMethod("getY").invoke(blockPos);
            int z = (int) blockPos.getClass().getMethod("getZ").invoke(blockPos);

            // Robust world detection via NMS Level -> Bukkit World
            org.bukkit.World world = (org.bukkit.World) level.getClass().getMethod("getWorld").invoke(level);

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("TestBlocks"), () -> {
                Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class);
                display.setItemStack(new ItemStack(itemMaterial));
                display.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0,0,0),
                    new Quaternionf(),
                    new Vector3f(1,1,1),
                    new Quaternionf()
                ));
                display.addScoreboardTag("custom_furniture");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void neighborChanged(Object blockState, Object level, Object blockPos, Object neighborBlock, Object neighborPos, boolean isMoving) {
        // Simple cleanup check - in real app, we'd check if the block is actually gone
        removeDisplay(level, blockPos);
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
