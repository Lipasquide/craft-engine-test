package me.testblocks.api;

import java.lang.reflect.Method;

public class DefaultBlockShape implements BlockShape {
    private final Object rawBlockState;
    private static Method getShapeMethod;
    private static Method getCollisionShapeMethod;
    private static Method getBlockSupportShapeMethod;

    static {
        try {
            Class<?> blockStateBaseClass = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase");
            getShapeMethod = blockStateBaseClass.getDeclaredMethod("getShape",
                Class.forName("net.minecraft.world.level.BlockGetter"),
                Class.forName("net.minecraft.core.BlockPos"),
                Class.forName("net.minecraft.world.phys.shapes.CollisionContext"));
            getShapeMethod.setAccessible(true);

            getCollisionShapeMethod = blockStateBaseClass.getDeclaredMethod("getCollisionShape",
                Class.forName("net.minecraft.world.level.BlockGetter"),
                Class.forName("net.minecraft.core.BlockPos"),
                Class.forName("net.minecraft.world.phys.shapes.CollisionContext"));
            getCollisionShapeMethod.setAccessible(true);

            getBlockSupportShapeMethod = blockStateBaseClass.getDeclaredMethod("getBlockSupportShape",
                Class.forName("net.minecraft.world.level.BlockGetter"),
                Class.forName("net.minecraft.core.BlockPos"));
            getBlockSupportShapeMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DefaultBlockShape(Object rawBlockState) {
        this.rawBlockState = rawBlockState;
    }

    @Override
    public Object getShape(Object thisObj, Object[] args) {
        try { return getShapeMethod.invoke(rawBlockState, args[1], args[2], args[3]); }
        catch (Exception e) { return null; }
    }

    @Override
    public Object getCollisionShape(Object thisObj, Object[] args) {
        try { return getCollisionShapeMethod.invoke(rawBlockState, args[1], args[2], args[3]); }
        catch (Exception e) { return null; }
    }

    @Override
    public Object getSupportShape(Object thisObj, Object[] args) {
        try { return getBlockSupportShapeMethod.invoke(rawBlockState, args[1], args[2]); }
        catch (Exception e) { return null; }
    }
}
