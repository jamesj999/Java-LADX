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
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEnemyInspection enemyInspection;
    final EntityEventChannels events;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final RoomEntity[] slots;
    final EntitySpriteViews spriteViews;
    final EntitySlotLifecycle lifecycle;

    EntityBackgroundMechanics(
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEnemyInspection enemyInspection,
            EntityEventChannels events,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            RoomEntity[] slots,
            EntitySpriteViews spriteViews,
            EntitySlotLifecycle lifecycle) {
        this.spriteHandlers = spriteHandlers;
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.enemyInspection = java.util.Objects.requireNonNull(enemyInspection, "enemyInspection");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.spriteViews = java.util.Objects.requireNonNull(spriteViews, "spriteViews");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public RoomEntityBackgroundInteraction interaction() {
        return this.physicsTables.readBackgroundInteraction();
    }

    boolean moldormOrHeadOverlapsSword(RoomEntity entity, int swordX, int swordWidth, int swordY, int swordHeight) {
        if (RoomEntityCombatRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight)) {
            return true;
        }
        if (entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            return false;
        }
        if (this.combatTables.readEnemyFlashCountdown(entity.slot()) != 0) {
            return false;
        }
        MoldormSegmentPosition tail =
                this.motions.get(MoldormMotion.class, MoldormMotion::new).vulnerableTail(entity.slot());
        RoomEntity tailCollisionEntity =
                withPositionAndVariant(entity, tail.x(), (tail.y() + entity.z()) & 0xFF, entity.spriteVariant());
        return RoomEntityCombatRules.overlapsSword(tailCollisionEntity, swordX, swordWidth, swordY, swordHeight);
    }

    public void advanceHookshotBridgeEntity(int index, RoomEntity entity) {
        EntityTerrainBehavior.advanceHookshotBridgeEntity(this, index, entity);
    }

    void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        if (slot >= 0
                && slot < this.slots.length
                && this.slots[slot].type() == EntityRomConstants.ENTITY_PUSHED_BLOCK
                && this.slots[slot].status() == EntityStatus.ACTIVE
                && RoomEntityCombatRules.overlapsLink(this.slots[slot], linkEntityX, linkEntityY)) {
            requestLinkPush(this.slots[slot], EntityLinkCollisionRules.PUSHED_BLOCK);
        }
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return this.roomInspection.gameplayEntityTimersInteractive(creditsGameplay);
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return this.roomInspection.hasNoGroundInteraction(entity);
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
        return this.enemyInspection.entityHandlerInteractive(creditsGameplay);
    }

    public boolean entityTimersInteractive() {
        return this.enemyInspection.entityTimersInteractive();
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
        this.events.pendingLinkFinalPositionRequests.add(new LinkFinalPositionRequest(
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
        return this.physicsTables.readGroundInteractionSideScrolling();
    }

    public RoomEntityObjectQuery objectQuery() {
        return this.physicsTables.readObjectQuery();
    }

    public RoomEntityGroundInteraction groundInteraction() {
        return this.physicsTables.readGroundInteraction();
    }

    public boolean switchBlockAnimationActive() {
        return this.roomVariables.readSwitchBlockAnimationActive();
    }

    public void requestSwitchBlockAnimation() {
        this.roomVariables.writePendingSwitchBlockAnimationRequest(true);
    }

    public int entityGroundStatus(int slot) {
        return this.physicsTables.readEntityGroundStatus(slot);
    }

    public void setEntityGroundStatus(int slot, int value) {
        this.physicsTables.writeEntityGroundStatus(slot, value);
    }

    public void setFallingVisualYOffset(int slot, int value) {
        this.physicsTables.writeFallingVisualYOffset(slot, value);
    }

    public void initializeKeyQuicksandFall(int slot) {
        this.physicsTables.writeFallingTargetX(slot, 0x50);
        this.physicsTables.writeFallingTargetY(slot, 0x48);
        this.physicsTables.writeFallingSpeedX(slot, 0);
        this.physicsTables.writeFallingSpeedY(slot, 0);
        this.physicsTables.writeFallingSpeedXAccumulator(slot, 0);
        this.physicsTables.writeFallingSpeedYAccumulator(slot, 0);
        this.physicsTables.writeFallingVisualYOffset(slot, 0);
    }
}
