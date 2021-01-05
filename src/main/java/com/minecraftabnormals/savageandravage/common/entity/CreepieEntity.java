package com.minecraftabnormals.savageandravage.common.entity;

import com.minecraftabnormals.abnormals_core.core.api.IAgeableEntity;
import com.minecraftabnormals.savageandravage.common.entity.goals.CreepieSwellGoal;
import com.minecraftabnormals.savageandravage.common.entity.goals.FollowMobOwnerGoal;
import com.minecraftabnormals.savageandravage.common.entity.goals.MobOwnerHurtByTargetGoal;
import com.minecraftabnormals.savageandravage.common.entity.goals.MobOwnerHurtTargetGoal;
import com.minecraftabnormals.savageandravage.core.SRConfig;
import com.minecraftabnormals.savageandravage.core.registry.SRParticles;
import com.minecraftabnormals.savageandravage.core.registry.SRSounds;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@OnlyIn(
		value = Dist.CLIENT,
		_interface = IChargeableMob.class
)
public class CreepieEntity extends MonsterEntity implements IChargeableMob, IOwnableMob, IAgeableEntity {
	private static final DataParameter<Integer> STATE = EntityDataManager.createKey(CreepieEntity.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> POWERED = EntityDataManager.createKey(CreepieEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> IGNITED = EntityDataManager.createKey(CreepieEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Optional<UUID>> OWNER_UUID = EntityDataManager.createKey(CreepieEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
	private static final DataParameter<Integer> CONVERSION_TIME = EntityDataManager.createKey(CreepieEntity.class, DataSerializers.VARINT);
	public boolean attackPlayersOnly;
	public int lastActiveTime;
	public int timeSinceIgnited;
	public int fuseTime = 30;
	private int growingAge = -24000;
	private int forcedAgeTimer;
	private float explosionRadius;
	private boolean hasStartedConverting = false;

	public CreepieEntity(EntityType<? extends CreepieEntity> type, World worldIn) {
		super(type, worldIn);
		this.explosionRadius = 1.2f;
		this.experienceValue = 0;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new SwimGoal(this));
		this.goalSelector.addGoal(2, new CreepieSwellGoal(this));
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, OcelotEntity.class, 6.0F, 1.0D, 1.2D));
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, CatEntity.class, 6.0F, 1.0D, 1.2D));
		this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
		this.goalSelector.addGoal(3, new FollowMobOwnerGoal(this, 1.0D, 2.0F, 20.0F));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 0.8D));
		this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.addGoal(6, new LookRandomlyGoal(this));
		this.targetSelector.addGoal(2, new MobOwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new MobOwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<MobEntity>(this, MobEntity.class, false) {
			@Override
			public boolean shouldExecute() {
				return super.shouldExecute()
						&& ((CreepieEntity) goalOwner).getOwnerId() == null
						&& !(this.nearestTarget instanceof CreepieEntity)
						&& !(this.nearestTarget instanceof CreeperEntity)
						&& !((CreepieEntity) goalOwner).attackPlayersOnly;
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<PlayerEntity>(this, PlayerEntity.class, false) {
			@Override
			public boolean shouldExecute() {
				return super.shouldExecute() && ((CreepieEntity) goalOwner).getOwnerId() == null;
			}
		});
	}

	public static AttributeModifierMap.MutableAttribute registerAttributes() {
		return MonsterEntity.func_233666_p_()
				.createMutableAttribute(Attributes.MAX_HEALTH, 5.0)
				.createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.35D);
	}

	@Override
	public int getMaxFallHeight() {
		return this.getAttackTarget() == null ? 3 : 3 + (int) (this.getHealth() - 1.0F);
	}

	@Override
	protected boolean isDespawnPeaceful() {
		return this.getOwner() == null;
	}

	@Override
	public boolean func_230292_f_(PlayerEntity playerIn) {
		return this.getOwner() == null;
	}

	public boolean onLivingFall(float distance, float damageMultiplier) {
		boolean flag = super.onLivingFall(distance, damageMultiplier);
		this.timeSinceIgnited = (int) ((float) this.timeSinceIgnited + distance * 1.5F);
		if (this.timeSinceIgnited > this.fuseTime - 5) {
			this.timeSinceIgnited = this.fuseTime - 5;
		}

		return flag;
	}

	@Override
	protected void registerData() {
		super.registerData();
		this.dataManager.register(STATE, -1);
		this.dataManager.register(POWERED, false);
		this.dataManager.register(IGNITED, false);
		this.dataManager.register(CONVERSION_TIME, -1);
		this.dataManager.register(OWNER_UUID, Optional.empty());
	}

	@Override
	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		if (this.getOwnerId() != null) {
			compound.putUniqueId("OwnerUUID", this.getOwnerId());
		}
		compound.putInt("Age", this.getGrowingAge());
		compound.putInt("ConversionTime", this.getConversionTime());
		compound.putShort("Fuse", (short) this.fuseTime);
		compound.putByte("ExplosionRadius", (byte) this.explosionRadius);
		compound.putBoolean("Ignited", this.hasIgnited());
		if (this.dataManager.get(POWERED)) {
			compound.putBoolean("Powered", true);
		}
		compound.putBoolean("AttackPlayersOnly", this.attackPlayersOnly);
	}

	@Override
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		if (compound.contains("Fuse", 99)) {
			this.fuseTime = compound.getShort("Fuse");
		}
		this.explosionRadius = compound.getByte("ExplosionRadius");
		if (compound.contains("Age", 99)) {
			this.setGrowingAge(compound.getInt("Age"));
		}
		if (compound.getBoolean("Ignited")) this.ignite();
		this.dataManager.set(POWERED, compound.getBoolean("Powered"));
		if (compound.contains("ConversionTime", 99) && compound.getInt("ConversionTime") > -1) {
			this.startConversion(compound.getInt("ConversionTime"));
		}
		if (compound.hasUniqueId("OwnerUUID")) {
			this.setOwnerId(compound.getUniqueId("OwnerUUID"));
		}
		this.attackPlayersOnly = compound.getBoolean("AttackPlayersOnly");
	}

	@Override
	public boolean isCharged() {
		return this.dataManager.get(POWERED);
	}

	public void setCharged(boolean charged) {
		this.dataManager.set(POWERED, charged);
	}


	@Override
	public ItemStack getPickedResult(RayTraceResult target) {
		return new ItemStack(Items.CREEPER_SPAWN_EGG);
	}

	@Override
	protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
		return sizeIn.height * 0.8F;
	}

	public int getGrowingAge() {
		return this.growingAge;
	}

	private void ageUp(int growthSeconds) {
		int i = this.getGrowingAge();
		i = i + growthSeconds * 20;
		if (i > 0) {
			i = 0;
		}

		this.setGrowingAge(i);
		if (this.forcedAgeTimer == 0) {
			this.forcedAgeTimer = 40;
		}
	}

	public void setGrowingAge(int age) {
		int i = this.growingAge;
		this.growingAge = age;
		if (i < 0 && age >= 0) {
			this.startConversion(this.rand.nextInt(80) + 160); //10 seconds before it converts
		}
	}

	/**
	 * Creates an explosion as determined by this creeper's power and explosion radius.
	 */
	protected void explode() {
		if (!this.world.isRemote()) {
			Explosion.Mode mode = SRConfig.COMMON.creepieExplosionsDestroyBlocks.get() ? Explosion.Mode.DESTROY : Explosion.Mode.NONE;
			float chargedModifier = this.isCharged() ? 2.0F : 1.0F;
			this.dead = true;
			this.world.createExplosion(this, this.getPosX(), this.getPosY(), this.getPosZ(), this.explosionRadius * chargedModifier, mode);
			this.remove();
			this.spawnLingeringCloud();
		}
	}

	@Override
	public void livingTick() {
		super.livingTick();
		if (this.world.isRemote()) {
			if (this.forcedAgeTimer > 0) {
				if (this.forcedAgeTimer % 4 == 0) {
					this.world.addParticle(ParticleTypes.HAPPY_VILLAGER, this.getPosXRandom(1.0D), this.getPosYRandom() + 0.5D, this.getPosZRandom(1.0D), 0.0D, 0.0D, 0.0D);
				}
				this.forcedAgeTimer--;
			}
		}
		if (this.isAlive()) {
			int i = this.getGrowingAge();
			if (i < 0) {
				++i;
				this.setGrowingAge(i);
			} else if (i > 0) {
				--i;
				this.setGrowingAge(i);
			}
		}

	}

	@Override
	protected float getSoundPitch() {
		return (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.5F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return SRSounds.ENTITY_CREEPIE_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SRSounds.ENTITY_CREEPIE_DEATH.get();
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	@Override
	public void tick() {
		if (this.isAlive()) {
			this.lastActiveTime = this.timeSinceIgnited;
			if (this.hasIgnited()) {
				this.setCreeperState(1);
			}
			int i = this.getCreeperState();
			if (i > 0 && this.timeSinceIgnited == 0) {
				this.playSound(SRSounds.ENTITY_CREEPIE_PRIMED.get(), this.getSoundVolume(), this.getSoundPitch());
			}
			this.timeSinceIgnited += i;
			if (this.timeSinceIgnited < 0) {
				this.timeSinceIgnited = 0;
			}

			if (this.timeSinceIgnited >= this.fuseTime) {
				this.timeSinceIgnited = this.fuseTime;
				this.explode();
			}
			if (this.isConverting()) {
				if (this.hasStartedConverting) {
					this.playSound(SRSounds.ENTITY_CREEPIE_CONVERT.get(), 1.0F, 1.0F);
					this.hasStartedConverting = false;
				}
				this.setConversionTime(this.getConversionTime() - 1);
				if (this.getConversionTime() <= 0) {
					this.finishConversion(this.world);
				}
				if (this.world.isRemote()) {
					this.world.addParticle(SRParticles.CREEPER_SPORES.get(), this.getPosX() - 0.5d + (double) (this.rand.nextFloat()), this.getPosY() + 0.5d, this.getPosZ() - 0.5d + (double) (this.rand.nextFloat()), 0.0D, (this.rand.nextFloat() / 5.0F), 0.0D);
				}
			}
		}
		super.tick();
	}

	@Override
	public boolean attackEntityAsMob(Entity entityIn) {
		return true;
	}

	@Override
	public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
		ItemStack itemstack = player.getHeldItem(hand);
		if (itemstack.getItem() == Items.BONE_MEAL) {
			if (this.getGrowingAge() < 0) {
				this.consumeItemFromStack(player, itemstack);
				this.ageUp((int) ((-this.getGrowingAge() / 20) * 0.1F));
				return ActionResultType.SUCCESS;
			}
		}
		if (itemstack.getItem() == Items.FLINT_AND_STEEL) {
			this.world.playSound(player, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ITEM_FLINTANDSTEEL_USE, this.getSoundCategory(), 1.0F, this.rand.nextFloat() * 0.4F + 0.8F);
			if (!this.world.isRemote()) {
				this.ignite();
				itemstack.damageItem(1, player, (p_213625_1_) -> p_213625_1_.sendBreakAnimation(hand));
			}

			return ActionResultType.SUCCESS;
		} else {
			return ActionResultType.FAIL;
		}
	}

	/**
	 * Params: (Float)Render tick. Returns the intensity of the creeper's flash when it is ignited.
	 */
	@OnlyIn(Dist.CLIENT)
	public float getCreeperFlashIntensity(float partialTicks) {
		return MathHelper.lerp(partialTicks, (float) this.lastActiveTime, (float) this.timeSinceIgnited) / (float) (this.fuseTime - 2);
	}

	/**
	 * Returns the current state of creeper, -1 is idle, 1 is 'in fuse'
	 */
	public int getCreeperState() {
		return this.dataManager.get(STATE);
	}

	/**
	 * Sets the state of creeper, -1 to idle and 1 to be 'in fuse'
	 */
	public void setCreeperState(int state) {
		this.dataManager.set(STATE, state);
	}

	public boolean hasIgnited() {
		return this.dataManager.get(IGNITED);
	}

	public void ignite() {
		this.dataManager.set(IGNITED, true);
	}

	protected void spawnLingeringCloud() {
		Collection<EffectInstance> collection = this.getActivePotionEffects();
		if (!collection.isEmpty()) {
			AreaEffectCloudEntity areaeffectcloudentity = new AreaEffectCloudEntity(this.world, this.getPosX(), this.getPosY(), this.getPosZ());
			areaeffectcloudentity.setRadius(1.0F);
			areaeffectcloudentity.setRadiusOnUse(-0.5F);
			areaeffectcloudentity.setWaitTime(10);
			areaeffectcloudentity.setDuration(areaeffectcloudentity.getDuration() / 2);
			areaeffectcloudentity.setRadiusPerTick(-areaeffectcloudentity.getRadius() / (float) areaeffectcloudentity.getDuration());

			for (EffectInstance effectinstance : collection) {
				areaeffectcloudentity.addEffect(new EffectInstance(effectinstance));
			}

			this.world.addEntity(areaeffectcloudentity);
		}

	}

	private void consumeItemFromStack(PlayerEntity player, ItemStack stack) {
		if (!player.isCreative()) {
			stack.shrink(1);
		}
	}

	@Override
	public boolean canBeLeashedTo(PlayerEntity player) {
		return (!this.getLeashed() && this.getOwnerId() != null);
	}

	@Override
	@Nullable
	public UUID getOwnerId() {
		return this.dataManager.get(OWNER_UUID).orElse(null);
	}

	@Override
	public void setOwnerId(@Nullable UUID ownerId) {
		this.dataManager.set(OWNER_UUID, Optional.ofNullable(ownerId));
	}

	@Override
	@Nullable
	public LivingEntity getOwner() {
		if (!this.world.isRemote()) { //TODO: this is experimental, if anything breaks by being only on the client, a packet is needed
			UUID uuid = this.getOwnerId();
			if (uuid == null)
				return null;
			Entity entity = ((ServerWorld) this.world).getEntityByUuid(uuid);
			return entity instanceof LivingEntity ? (LivingEntity) entity : null;
		}
		return null;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return target != this.getOwner() && super.canAttack(target);
	}

	@Override
	public boolean shouldAttackEntity(LivingEntity target, LivingEntity owner) {
		boolean shouldAttack = true;
		if (target instanceof IOwnableMob) {
			shouldAttack = ((IOwnableMob) target).getOwner() != owner;
		} else if (target instanceof TameableEntity) {
			shouldAttack = ((TameableEntity) target).getOwner() != owner;
		} else if (target instanceof PlayerEntity && owner instanceof PlayerEntity) {
			shouldAttack = ((PlayerEntity) owner).canAttackPlayer((PlayerEntity) target);
		}
		return shouldAttack;
	}

	public boolean isConverting() {
		return this.getDataManager().get(CONVERSION_TIME) > -1;
	}

	private void startConversion(int conversionTime) {
		this.setConversionTime(conversionTime);
		hasStartedConverting = true; //Sound playing is done in tick because after summoning, this is called before the position is set
	}

	public int getConversionTime() {
		return this.getDataManager().get(CONVERSION_TIME);
	}

	private void setConversionTime(int conversionTimeIn) {
		this.dataManager.set(CONVERSION_TIME, conversionTimeIn);
	}

	private LivingEntity finishConversion(World world) {
		CreeperEntity creeperEntity = EntityType.CREEPER.create(this.world);
		if (creeperEntity == null)
			return null;

		creeperEntity.copyLocationAndAnglesFrom(this);
		if (!this.world.isRemote())
			creeperEntity.onInitialSpawn((ServerWorld) world, this.world.getDifficultyForLocation(creeperEntity.getPosition()), SpawnReason.CONVERSION, null, null);
		creeperEntity.setNoAI(this.isAIDisabled());
		if (this.hasCustomName()) {
			creeperEntity.setCustomName(this.getCustomName());
			creeperEntity.setCustomNameVisible(this.isCustomNameVisible());
		}

		if (this.isNoDespawnRequired()) {
			creeperEntity.enablePersistence();
		}
		if (this.getLeashed()) {
			if (this.getLeashHolder() != null) creeperEntity.setLeashHolder(this.getLeashHolder(), true);
			this.clearLeashed(true, false);
		}

		if (this.getRidingEntity() != null) {
			creeperEntity.startRiding(this.getRidingEntity());
		}
		creeperEntity.setInvulnerable(this.isInvulnerable());
		creeperEntity.setHealth(creeperEntity.getMaxHealth());
		this.dead = true;
		this.remove();
		this.world.addEntity(creeperEntity);
		this.playSound(SRSounds.ENTITY_CREEPIE_GROW.get(), 1.0F, 1.0F);
		return creeperEntity;
	}

	@Override
	public Team getTeam() {
		LivingEntity owner = this.getOwner();
		if (owner != null)
			return owner.getTeam();
		return super.getTeam();
	}

	@Override
	public boolean isOnSameTeam(Entity entityIn) {
		LivingEntity owner = this.getOwner();
		if (entityIn == owner)
			return true;

		if (owner != null)
			return owner.isOnSameTeam(entityIn);

		return super.isOnSameTeam(entityIn);
	}

	@Override
	public boolean hasGrowthProgress() {
		return true;
	}

	@Override
	public void resetGrowthProgress() {
		this.setGrowingAge(-24000);
	}

	@Override
	public boolean canAge(boolean isGrowing) {
		return isGrowing;
	}

	@Override
	public LivingEntity attemptAging(boolean isGrowing) {
		if (isGrowing) {
			this.growingAge = 0;
			return this.finishConversion(this.world);
		}
		return this;
	}
}
