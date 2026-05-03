package me.testblocks.behavior;

import me.testblocks.api.BlockBehavior;
import org.bukkit.Material;

public class CustomDropBehavior extends BlockBehavior {
    private final Material drop;
    public CustomDropBehavior(Material drop) { this.drop = drop; }
    public Material getDrop() { return drop; }
}
