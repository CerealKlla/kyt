package com.github.cerealklla.kyt.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Places a {@link KytConfigurationEntity} on right-click, consuming one item -- same pattern as
 * every other placement item in this suite (e.g. Settlemynts' {@code
 * founding.SettlementClaimFlagItem}), held in the hand rather than any bigger structure-placement
 * system, per explicit user instruction.
 */
public class KytConfigurationStandItem extends Item {

    public KytConfigurationStandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            // Client-side prediction: let the swing animation play, real work happens server-side.
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.FAIL;
        }

        BlockPos placePos = context.getClickedPos().above();
        float yRot = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
        KytConfigurationEntity.create(serverLevel, placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5, yRot);

        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
