package com.minecraftabnormals.savageandravage.common.entity;

import net.minecraft.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

public interface IOwnableMob {

    @Nullable
    LivingEntity getOwner();

    @Nullable
    UUID getOwnerId();

    void setOwnerId(@Nullable UUID uuid);

    boolean shouldAttackEntity(LivingEntity target, LivingEntity owner);
}
