package me.thegiggitybyte.wumpuslink.mixin;

import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DamageTracker.class)
public interface DamageTrackerAccessor {
    @Invoker(value = "getBiggestFall")
    DamageRecord getFurthestFall();

}
