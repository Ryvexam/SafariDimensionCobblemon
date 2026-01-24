package com.safari.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class SafariPortalNpcEntity extends PathAwareEntity {

    private String npcName = "Safari Keeper";

    public SafariPortalNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setCustomName(Text.literal(npcName));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void initGoals() {
        // Basic AI goals
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createNpcAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!player.getWorld().isClient() && hand == Hand.MAIN_HAND) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            var server = serverPlayer.getServer();
            if (server != null) {
                try {
                    server.getCommandManager().getDispatcher()
                        .execute("safari enter", serverPlayer.getCommandSource());
                } catch (Exception e) {
                    return ActionResult.FAIL;
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean isInvulnerable() {
        return true; // NPCs are invulnerable by default
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("CustomName", npcName);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("CustomName")) {
            this.npcName = nbt.getString("CustomName");
            this.setCustomName(Text.literal(npcName));
        }
    }

    // Setters for customization
    public void setNpcName(String name) {
        this.npcName = name;
        this.setCustomName(Text.literal(name));
    }

    public String getNpcName() {
        return npcName;
    }
}
