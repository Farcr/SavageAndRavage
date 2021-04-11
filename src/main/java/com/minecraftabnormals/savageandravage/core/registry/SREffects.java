package com.minecraftabnormals.savageandravage.core.registry;

import com.minecraftabnormals.savageandravage.common.effect.ConfusionEffect;
import com.minecraftabnormals.savageandravage.common.effect.FrostbiteEffect;
import com.minecraftabnormals.savageandravage.common.effect.WeightEffect;
import com.minecraftabnormals.savageandravage.core.SavageAndRavage;
import net.minecraft.potion.Effect;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class SREffects {
	public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, SavageAndRavage.MOD_ID);

	public static final RegistryObject<Effect> CONFUSION = EFFECTS.register("confusion", ConfusionEffect::new);
	public static final RegistryObject<Effect> FROSTBITE = EFFECTS.register("frostbite", FrostbiteEffect::new);
	public static final RegistryObject<Effect> WEIGHT = EFFECTS.register("weight", WeightEffect::new);

}
