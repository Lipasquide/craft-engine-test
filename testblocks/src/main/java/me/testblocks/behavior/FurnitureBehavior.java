package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;
import org.joml.Quaternionf;

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

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("TestBlocks"), () -> {
                Location loc = new Location(Bukkit.getWorlds().get(0), x + 0.5, y + 0.5, z + 0.5);
                ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class);
                display.setItemStack(new ItemStack(itemMaterial));
                display.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0,0,0),
                    new Quaternionf(),
                    new Vector3f(1,1,1),
                    new Quaternionf()
                ));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
