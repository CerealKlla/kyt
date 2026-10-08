package com.github.cerealklla.kyt.structure;

import com.github.cerealklla.kyt.registration.ModEntities;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The "Kyt Configuration" structure -- a plain marker {@link Entity} (not a real {@code
 * ArmorStand}), rendered client-side using vanilla's own armor-stand model so it visually matches
 * one, same pattern as Settlemynts' {@code founding.GhostTownHallCoreEntity}. Being a non-{@code
 * LivingEntity} marker gets indestructibility and non-equippability for free -- nothing here ever
 * calls {@code setItemSlot}, and {@link #hurtServer} always refuses damage -- without needing to
 * fight any of real {@code ArmorStand}'s built-in pose/equip/hit behavior.
 */
public class KytConfigurationEntity extends Entity {

    public KytConfigurationEntity(EntityType<? extends KytConfigurationEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Sits exactly where it's placed -- no falling/pushing.
    }

    public static KytConfigurationEntity create(ServerLevel level, double x, double y, double z, float yRot) {
        KytConfigurationEntity entity = new KytConfigurationEntity(ModEntities.KYT_CONFIGURATION.get(), level);
        entity.setPos(x, y, z);
        entity.setYRot(yRot);
        entity.setYHeadRot(yRot);
        level.addFreshEntity(entity);
        return entity;
    }

    // Entity#isPickable() defaults to false -- without this override this entity would be
    // invisible to the game's own crosshair/interaction raycast (the same real bug class
    // Settlemynts/Yconomics both hit first with their own marker entities).
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenKytMainMenuPayload(getId()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Kyt Configuration");
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // Indestructible -- a configuration structure can't be destroyed by combat/explosions.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced data needed -- this entity's appearance is a fixed default armor-stand pose,
        // nothing about it varies per-client.
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        // No persisted state beyond position/rotation (handled by the base Entity class itself).
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        // No persisted state beyond position/rotation (handled by the base Entity class itself).
    }
}
