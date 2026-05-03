package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;
import java.util.ArrayList;
import java.util.List;

public class CustomDropBehavior implements BlockBehavior {

    private final Object dropItem;

    public CustomDropBehavior(Object nmsItem) {
        this.dropItem = nmsItem;
    }

    @Override
    public List<Object> getDrops(Object blockState, Object lootParamsBuilder) {
        try {
            Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            Class<?> nmsItemClass = Class.forName("net.minecraft.world.item.Item");

            List<Object> drops = new ArrayList<>();
            Object itemStack = nmsItemStackClass.getConstructor(nmsItemClass).newInstance(dropItem);
            drops.add(itemStack);
            return drops;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
