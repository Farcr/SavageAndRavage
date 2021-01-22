package com.minecraftabnormals.savageandravage.core.other;

import com.minecraftabnormals.abnormals_core.core.util.DataUtil;
import com.minecraftabnormals.savageandravage.common.entity.BurningBannerEntity;
import com.minecraftabnormals.savageandravage.common.entity.MischiefArrowEntity;
import com.minecraftabnormals.savageandravage.common.entity.SporeCloudEntity;
import com.minecraftabnormals.savageandravage.common.entity.block.SporeBombEntity;
import com.minecraftabnormals.savageandravage.core.registry.SRBlocks;
import com.minecraftabnormals.savageandravage.core.registry.SRItems;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.dispenser.ProjectileDispenseBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

import static com.minecraftabnormals.abnormals_core.core.util.BlockUtil.getEntitiesAtOffsetPos;
import static com.minecraftabnormals.abnormals_core.core.util.BlockUtil.offsetPos;

public class SRCompat {

	public static void registerFlammables() {
		DataUtil.registerFlammable(SRBlocks.CREEPER_SPORE_SACK.get(), 30, 60);
		DataUtil.registerFlammable(SRBlocks.SPORE_BOMB.get(), 15, 100);
	}

	public static void registerDispenserBehaviors() {
		DispenserBlock.registerDispenseBehavior(SRItems.MISCHIEF_ARROW.get(), new ProjectileDispenseBehavior() {
			@Override
			protected ProjectileEntity getProjectileEntity(World world, IPosition position, ItemStack stack) {
				return new MischiefArrowEntity(world, position.getX(), position.getY(), position.getZ());
			}
		});
		DispenserBlock.registerDispenseBehavior(SRItems.CREEPER_SPORES.get(), new ProjectileDispenseBehavior() {
			@Override
			protected ProjectileEntity getProjectileEntity(World world, IPosition position, ItemStack stack) {
				SporeCloudEntity cloud = new SporeCloudEntity(world, position.getX(), position.getY(), position.getZ());
				cloud.setCloudSize(world.getRandom().nextInt(50) == 0 ? 0 : 1 + world.getRandom().nextInt(3));
				return cloud;
			}
		});

		DispenserBlock.registerDispenseBehavior(SRBlocks.SPORE_BOMB.get(), new DefaultDispenseItemBehavior() {
			protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
				World world = source.getWorld();
				BlockPos blockpos = offsetPos(source);
				SporeBombEntity sporeBomb = new SporeBombEntity(world, (double) blockpos.getX() + 0.5D, blockpos.getY(), (double) blockpos.getZ() + 0.5D, null);
				world.addEntity(sporeBomb);
				world.playSound(null, sporeBomb.getPosX(), sporeBomb.getPosY(), sporeBomb.getPosZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
				stack.shrink(1);
				return stack;
			}
		});
		ForgeRegistries.ITEMS.getEntries().stream().map(Map.Entry::getValue).filter(i -> i instanceof BannerItem).forEach(i -> DataUtil.registerAlternativeDispenseBehavior(i, (source, stack) -> !getEntitiesAtOffsetPos(source, LivingEntity.class, EntityPredicates.NOT_SPECTATING.and(new EntityPredicates.ArmoredMob(stack))).isEmpty(), ArmorItem.DISPENSER_BEHAVIOR));
		DataUtil.registerAlternativeDispenseBehavior(Items.FLINT_AND_STEEL, (source, stack) -> SREvents.isValidBurningBannerPos(source.getWorld(), offsetPos(source)), new DefaultDispenseItemBehavior() {
			@Override
			protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
				World world = source.getWorld();
				world.addEntity(new BurningBannerEntity(world, offsetPos(source), null));
				if (stack.attemptDamageItem(1, world.rand, null)) {
					stack.setCount(0);
				}
				return stack;
			}
		});
	}
}
