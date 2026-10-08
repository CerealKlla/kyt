package com.github.cerealklla.kyt.registration;

import com.github.cerealklla.kyt.KytMod;
import com.github.cerealklla.kyt.structure.KytConfigurationEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

    private ModEntities() {
    }

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(KytMod.MODID);

    // Sized to vanilla armor stand's own real hitbox dimensions, since this entity renders using
    // that model -- keeps the interaction/collision box matching what's actually drawn.
    public static final DeferredHolder<EntityType<?>, EntityType<KytConfigurationEntity>> KYT_CONFIGURATION = ENTITIES.registerEntityType(
            "kyt_configuration",
            KytConfigurationEntity::new,
            MobCategory.MISC,
            builder -> builder.sized(0.5f, 1.975f));
}
