package com.safari.entity;

import com.safari.world.SafariDimension;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import com.safari.shop.SafariShopScreenHandler;
import net.minecraft.entity.damage.DamageSource;

import java.util.EnumSet;

public class SafariNpcEntity extends PathAwareEntity {
    public SafariNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent();
        this.setInvulnerable(true);
    }

    public static DefaultAttributeContainer createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .build();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SmoothLookGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && !this.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            this.discard();
        }
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, user) -> new SafariShopScreenHandler(syncId, inv),
                    Text.of("Safari Shop")
            ));
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    private static final class SmoothLookGoal extends Goal {
        private final SafariNpcEntity npc;
        private int cooldown;

        private SmoothLookGoal(SafariNpcEntity npc) {
            this.npc = npc;
            this.setControls(EnumSet.of(Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return true;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
                return;
            }

            PlayerEntity target = npc.getWorld().getClosestPlayer(npc, 8.0);
            if (target == null) {
                return;
            }

            Vec3d targetPos = target.getPos();
            npc.getLookControl().lookAt(targetPos.x, targetPos.y + target.getStandingEyeHeight(), targetPos.z, 12.0f, 12.0f);
            cooldown = 8;
        }
    }
}
