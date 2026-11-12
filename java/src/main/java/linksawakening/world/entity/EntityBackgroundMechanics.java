package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.MoldormSegmentPosition;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** ROM background collision, falling, recoil and Link push glue. */
public final class EntityBackgroundMechanics {
    final EntityBackgroundMechanicsServices runtime;

    EntityBackgroundMechanics(EntityBackgroundMechanicsServices runtime) {
        this.runtime = runtime;
    }

    public RoomEntityBackgroundInteraction interaction() {
        return runtime.physicsTables().readBackgroundInteraction();
    }

    boolean moldormOrHeadOverlapsSword(RoomEntity entity, int swordX, int swordWidth, int swordY, int swordHeight) {
        if (RoomEntityCombatRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight)) {
            return true;
        }
        if (entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            return false;
        }
        if (runtime.combatTables().readEnemyFlashCountdown(entity.slot()) != 0) {
            return false;
        }
        MoldormSegmentPosition tail =
                runtime.motions().get(MoldormMotion.class, MoldormMotion::new).vulnerableTail(entity.slot());
        RoomEntity tailCollisionEntity =
                withPositionAndVariant(entity, tail.x(), (tail.y() + entity.z()) & 0xFF, entity.spriteVariant());
        return RoomEntityCombatRules.overlapsSword(tailCollisionEntity, swordX, swordWidth, swordY, swordHeight);
    }

    public void advanceHookshotBridgeEntity(int index, RoomEntity entity) {
        EntityTerrainBehavior.advanceHookshotBridgeEntity(this, index, entity);
    }

    void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        if (slot >= 0
                && slot < runtime.slots().length
                && runtime.slots()[slot].type() == EntityRomConstants.ENTITY_PUSHED_BLOCK
                && runtime.slots()[slot].status() == EntityStatus.ACTIVE
                && RoomEntityCombatRules.overlapsLink(runtime.slots()[slot], linkEntityX, linkEntityY)) {
            requestLinkPush(runtime.slots()[slot], EntityLinkCollisionRules.PUSHED_BLOCK);
        }
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return runtime.roomInspection().gameplayEntityTimersInteractive(creditsGameplay);
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return runtime.roomInspection().hasNoGroundInteraction(entity);
    }

    boolean beginFalling(
            RoomEntity entity,
            RoomEntityGroundInteraction.PitTransition transition,
            boolean ignoreHitsDecrementedBeforeHandler) {
        return EntityTerrainBehavior.beginFalling(this, entity, transition, ignoreHitsDecrementedBeforeHandler);
    }

    RoomEntity advanceFallingEntity(RoomEntity entity) {
        return EntityTerrainBehavior.advanceFallingEntity(this, entity);
    }

    public int verticalSpeedZ(RoomEntity entity) {
        return EntityMotionCollisions.verticalSpeedZ(this, entity);
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        return runtime.enemyInspection().entityHandlerInteractive(creditsGameplay);
    }

    public boolean entityTimersInteractive() {
        return runtime.enemyInspection().entityTimersInteractive();
    }

    public void requestArmosLinkPush(RoomEntity entity, int linkEntityX, int linkEntityY, int linkMotionState) {
        if (linkMotionState >= EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
                || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return;
        }
        requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
    }

    public void requestLinkPush(RoomEntity entity, EntityLinkCollisionRules.LinkPushPolicy policy) {
        if (entity == null || policy == null || !policy.restoreFinalPosition()) {
            return;
        }
        runtime.events()
                .pendingLinkFinalPositionRequests
                .add(new LinkFinalPositionRequest(
                        entity.slot(),
                        policy.resetPegasusBoots(),
                        policy.resetHookshotChain(),
                        policy.markLinkPushing(),
                        policy.clearLinkPositionIncrement()));
        if (policy.resetHookshotChain()) {
            resetHookshotChainAfterLinkPush();
        }
    }

    void resetHookshotChainAfterLinkPush() {
        EntityMotionCollisions.resetHookshotChainAfterLinkPush(this);
    }

    public EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        return EntityMotionCollisions.applyEnemyRecoilIfNeeded(this, entity, backgroundCollision);
    }

    public boolean groundInteractionSideScrolling() {
        return runtime.physicsTables().readGroundInteractionSideScrolling();
    }

    public RoomEntityObjectQuery objectQuery() {
        return runtime.physicsTables().readObjectQuery();
    }

    public RoomEntityGroundInteraction groundInteraction() {
        return runtime.physicsTables().readGroundInteraction();
    }

    public boolean switchBlockAnimationActive() {
        return runtime.roomVariables().readSwitchBlockAnimationActive();
    }

    public void requestSwitchBlockAnimation() {
        runtime.roomVariables().writePendingSwitchBlockAnimationRequest(true);
    }

    public int entityGroundStatus(int slot) {
        return runtime.physicsTables().readEntityGroundStatus(slot);
    }

    public void setEntityGroundStatus(int slot, int value) {
        runtime.physicsTables().writeEntityGroundStatus(slot, value);
    }

    public void setFallingVisualYOffset(int slot, int value) {
        runtime.physicsTables().writeFallingVisualYOffset(slot, value);
    }

    public void initializeKeyQuicksandFall(int slot) {
        runtime.physicsTables().writeFallingTargetX(slot, 0x50);
        runtime.physicsTables().writeFallingTargetY(slot, 0x48);
        runtime.physicsTables().writeFallingSpeedX(slot, 0);
        runtime.physicsTables().writeFallingSpeedY(slot, 0);
        runtime.physicsTables().writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables().writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
    }
}
