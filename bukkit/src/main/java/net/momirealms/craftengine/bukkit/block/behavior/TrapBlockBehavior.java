package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A custom block behavior that damages entities when they step on or walk inside the block.
 * <p>
 * Configuration options:
 * <ul>
 *   <li>{@code damage} - Amount of damage to deal per trigger (default: 2.0)</li>
 *   <li>{@code damage-cause} - Bukkit DamageCause: CONTACT, HOT_FLOOR, CUSTOM, etc. (default: "CONTACT")</li>
 *   <li>{@code affect-players-only} - Whether only players are affected (default: false)</li>
 *   <li>{@code trigger} - When the trap activates: "step_on" or "entity_inside" (default: "step_on")</li>
 *   <li>{@code disable-when-sneaking} - Whether sneaking players bypass the trap (default: false)</li>
 * </ul>
 * <p>
 * Example YAML:
 * <pre>
 * behavior:
 *   type: trap_block
 *   damage: 4.0
 *   damage-cause: CONTACT
 *   affect-players-only: false
 *   trigger: step_on
 *   disable-when-sneaking: true
 * </pre>
 */
public class TrapBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<TrapBlockBehavior> FACTORY = new Factory();

    private final double damage;
    private final boolean affectPlayersOnly;
    private final TriggerMode triggerMode;
    private final boolean disableWhenSneaking;

    public TrapBlockBehavior(
            CustomBlock customBlock,
            double damage,
            boolean affectPlayersOnly,
            TriggerMode triggerMode,
            boolean disableWhenSneaking
    ) {
        super(customBlock);
        this.damage = damage;
        this.affectPlayersOnly = affectPlayersOnly;
        this.triggerMode = triggerMode;
        this.disableWhenSneaking = disableWhenSneaking;
    }

    @Override
    public void stepOn(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        if (this.triggerMode != TriggerMode.STEP_ON) return;
        Object entity = args[3];
        applyDamage(entity);
    }

    @Override
    public void entityInside(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        if (this.triggerMode != TriggerMode.ENTITY_INSIDE) return;
        Object entity = args[3];
        applyDamage(entity);
    }

    private void applyDamage(Object nmsEntity) {
        if (this.affectPlayersOnly && !CoreReflections.clazz$Player.isInstance(nmsEntity)) {
            return;
        }
        if (!CoreReflections.clazz$LivingEntity.isInstance(nmsEntity)) {
            return;
        }
        if (this.disableWhenSneaking && FastNMS.INSTANCE.method$Entity$getSharedFlag(nmsEntity, 1)) {
            return;
        }
        Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(nmsEntity);
        if (bukkitEntity instanceof LivingEntity livingEntity) {
            livingEntity.damage(this.damage);
        }
    }

    public enum TriggerMode {
        STEP_ON,
        ENTITY_INSIDE;

        public static TriggerMode fromString(String value) {
            return switch (value.toLowerCase()) {
                case "entity_inside", "inside" -> ENTITY_INSIDE;
                default -> STEP_ON;
            };
        }
    }

    private static class Factory implements BlockBehaviorFactory<TrapBlockBehavior> {

        @Override
        public TrapBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            double damage = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("damage", 2.0), "damage");
            boolean affectPlayersOnly = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("affect-players-only", false), "affect-players-only");
            String triggerStr = arguments.getOrDefault("trigger", "step_on").toString();
            TriggerMode triggerMode = TriggerMode.fromString(triggerStr);
            boolean disableWhenSneaking = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("disable-when-sneaking", false), "disable-when-sneaking");
            return new TrapBlockBehavior(block, damage, affectPlayersOnly, triggerMode, disableWhenSneaking);
        }
    }
}
