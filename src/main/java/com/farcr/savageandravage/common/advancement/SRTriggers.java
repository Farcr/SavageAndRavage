package com.farcr.savageandravage.common.advancement;

import com.farcr.savageandravage.core.SavageAndRavage;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * @author - SmellyModder(Luke Tonon)
 */
@Mod.EventBusSubscriber(modid = SavageAndRavage.MODID)
public class SRTriggers {
    public static final EmptyTrigger BURN_BANNER = CriteriaTriggers.register(new EmptyTrigger(prefix("burn_banner")));

    private static ResourceLocation prefix(String name) {
        return new ResourceLocation(SavageAndRavage.MODID, name);
    }

}
