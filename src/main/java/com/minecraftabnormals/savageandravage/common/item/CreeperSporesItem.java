package com.minecraftabnormals.savageandravage.common.item;

import com.minecraftabnormals.savageandravage.common.entity.CreeperSporeCloudEntity;
import com.minecraftabnormals.savageandravage.core.registry.SRBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class CreeperSporesItem extends Item implements PottableItem {

    public CreeperSporesItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        world.playSound(player, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ENTITY_EGG_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        if (!world.isRemote()) {
            CreeperSporeCloudEntity spores = new CreeperSporeCloudEntity(world, player);
            spores.func_234612_a_(player, player.rotationPitch, player.rotationYaw, 0.0F, 0.99F, 1.0F);
            spores.setCloudSize(world.getRandom().nextInt(50) == 0 ? 0 : 1 + spores.world.getRandom().nextInt(3));
            world.addEntity(spores);
        }

        player.addStat(Stats.ITEM_USED.get(this));
        if (!player.isCreative())
            stack.shrink(1);

        return ActionResult.resultSuccess(stack);
    }

    @Override
    public BlockState getPottedState() {
        return SRBlocks.POTTED_CREEPIE.get().getDefaultState();
    }
}