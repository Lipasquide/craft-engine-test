package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;
import java.lang.reflect.Method;

public class ShapeFactory {

    private static Method boxMethod;

    static {
        try {
            Class<?> shapesClass = Class.forName("net.minecraft.world.phys.shapes.Shapes");
            boxMethod = shapesClass.getMethod("box", double.class, double.class, double.class, double.class, double.class, double.class);
        } catch (Exception e) {
            System.err.println("[TestBlocks] CRITICAL: Shapes.box method not found! Version mismatch?");
        }
    }

    public static Object createBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        if (boxMethod == null) {
            throw new IllegalStateException("Shapes.box method not found");
        }
        try {
            return boxMethod.invoke(null, x1, y1, z1, x2, y2, z2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create NMS box", e);
        }
    }

    public static BlockBehavior fence() {
        return new CustomShapeBehavior(createBox(6, 0, 6, 10, 16, 10));
    }

    public static BlockBehavior slab() {
        return new CustomShapeBehavior(createBox(0, 0, 0, 16, 8, 16));
    }

    public static BlockBehavior wall() {
        return new CustomShapeBehavior(createBox(4, 0, 4, 12, 16, 12));
    }
}
