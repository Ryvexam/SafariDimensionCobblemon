package com.safari.client;

import com.safari.SafariMod;
import com.safari.entity.SafariEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class SafariClientMod implements ClientModInitializer {
    private static final Identifier NPC_TEXTURE = Identifier.of(SafariMod.MOD_ID, "textures/entity/safari_npc.png");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(SafariEntities.SAFARI_NPC, (EntityRendererFactory.Context context) ->
                new MobEntityRenderer<>(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.5f) {
                    @Override
                    public Identifier getTexture(com.safari.entity.SafariNpcEntity entity) {
                        return NPC_TEXTURE;
                    }
                }
        );
    }
}
