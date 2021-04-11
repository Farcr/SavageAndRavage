package com.minecraftabnormals.savageandravage.core;

import com.minecraftabnormals.abnormals_core.core.util.DataUtil;
import com.minecraftabnormals.abnormals_core.core.util.registry.RegistryHelper;
import com.minecraftabnormals.savageandravage.client.render.IceChunkRenderer;
import com.minecraftabnormals.savageandravage.client.render.layer.EvokerShieldLayer;
import com.minecraftabnormals.savageandravage.core.other.SRCompat;
import com.minecraftabnormals.savageandravage.core.other.SRFeatures;
import com.minecraftabnormals.savageandravage.core.other.SRLoot;
import com.minecraftabnormals.savageandravage.core.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.EvokerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SavageAndRavage.MOD_ID)
public class SavageAndRavage {
	public static final String MOD_ID = "savageandravage";
	public static final RegistryHelper REGISTRY_HELPER = new RegistryHelper(MOD_ID);

	public SavageAndRavage() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

		REGISTRY_HELPER.register(bus);
		SREntities.ENTITIES.register(bus);
		SRParticles.PARTICLES.register(bus);
		SREffects.EFFECTS.register(bus);
		SRFeatures.FEATURES.register(bus);
		SRAttributes.ATTRIBUTES.register(bus);

		MinecraftForge.EVENT_BUS.register(this);

		bus.addListener(this::commonSetup);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			bus.addListener(this::clientSetup);
			bus.addListener(this::registerModels);
			bus.addListener(this::finish);
		});

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SRConfig.COMMON_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SRConfig.CLIENT_SPEC);
		DataUtil.registerConfigCondition(SavageAndRavage.MOD_ID, SRConfig.COMMON, SRConfig.CLIENT);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			SREntities.registerEntitySpawns();
			SREntities.registerTrackedData();
			SRFeatures.registerPools();
			SRFeatures.registerBiomeModifications();
			SREntities.registerAttributes();
			SREntities.registerWaveMembers();
			SRLoot.registerLootConditions();
			SRCompat.registerFlammables();
			SRCompat.registerDispenserBehaviors();
		});
	}

	private void clientSetup(FMLClientSetupEvent event) {
		SREntities.registerRendering();
		event.enqueueWork(() -> {
			SRItems.registerItemProperties();
		});
	}

	private void registerModels(ModelRegistryEvent event) {
		ModelLoader.addSpecialModel(IceChunkRenderer.MODEL_LOCATION);
	}

	@SuppressWarnings("unchecked")
	@OnlyIn(Dist.CLIENT)
	private void finish(FMLLoadCompleteEvent event) {
		event.enqueueWork(() -> {
			EntityRendererManager manager = Minecraft.getInstance().getRenderManager();
			EntityRenderer<?> render = manager.renderers.get(EntityType.EVOKER);
			if (render instanceof EvokerRenderer)
				((EvokerRenderer<EvokerEntity>) render).addLayer(new EvokerShieldLayer((EvokerRenderer<EvokerEntity>) render));
		});
	}
}
