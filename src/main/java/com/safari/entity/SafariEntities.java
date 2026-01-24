package com.safari.entity;

import com.safari.SafariMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.entity.EntityType;

public final class SafariEntities {
    public static final EntityType<SafariNpcEntity> SAFARI_NPC = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(SafariMod.MOD_ID, "safari_npc"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, SafariNpcEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeBlocks(8)
                    .trackedUpdateRate(2)
                    .build()
    );
    public static final EntityType<SafariPortalNpcEntity> SAFARI_PORTAL_NPC = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(SafariMod.MOD_ID, "safari_portal_npc"),
        EntityType.Builder.create(SafariPortalNpcEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6f, 1.8f)
            .maxTrackingRange(48)
            .build()
    );
    private SafariEntities() {
    }

    public static void register() {
        // Ensures static initialization runs.
    }

    public static void registerAttributes() {
        DefaultAttributeContainer attributes = SafariNpcEntity.createAttributes();
        FabricDefaultAttributeRegistry.register(SAFARI_NPC, attributes);
        FabricDefaultAttributeRegistry.register(SAFARI_PORTAL_NPC, SafariPortalNpcEntity.createNpcAttributes());
    }
}
