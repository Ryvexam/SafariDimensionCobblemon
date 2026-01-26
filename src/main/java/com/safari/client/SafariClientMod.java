package com.safari.client;

import com.safari.SafariMod;
import com.safari.entity.SafariEntities;
import com.safari.entity.SafariNpcEntity;
import com.safari.entity.SafariPortalNpcEntity;
import com.safari.item.ModItems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.Identifier;

public class SafariClientMod implements ClientModInitializer {
    private static final Identifier NPC_TEXTURE = Identifier.of(SafariMod.MOD_ID, "textures/entity/safari_npc.png");
    public static ModelTransformationMode currentMode = ModelTransformationMode.NONE;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(SafariEntities.SAFARI_NPC, (EntityRendererFactory.Context context) ->
                new MobEntityRenderer<>(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.5f) {
                    @Override
                    public Identifier getTexture(SafariNpcEntity entity) {
                        return NPC_TEXTURE;
                    }
                }
        );
        EntityRendererRegistry.register(SafariEntities.SAFARI_PORTAL_NPC, (EntityRendererFactory.Context context) ->
                new MobEntityRenderer<>(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.5f) {
                    @Override
                    public Identifier getTexture(SafariPortalNpcEntity entity) {
                        return NPC_TEXTURE;
                    }
                }
        );

        // Register perspective predicate (0 = GUI/Other, 1 = Hand)
        ModelPredicateProviderRegistry.register(ModItems.SAFARI_BALL, Identifier.of(SafariMod.MOD_ID, "perspective"), (stack, world, entity, seed) -> {
            if (currentMode == ModelTransformationMode.GUI || currentMode == ModelTransformationMode.FIXED) {
                return 0.0f;
            }
            return 1.0f;
        });
    }
}
