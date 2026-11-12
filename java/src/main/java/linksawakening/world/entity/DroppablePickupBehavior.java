package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate;
import linksawakening.world.entity.RoomEntityRuntime.SlimeKeyTransitionUpdate;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityPickupController. */
final class DroppablePickupBehavior {
    static void initializeCommonDroppable(EntityPickupController owner, RoomEntity entity) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        // EntityInitDiggableBushOrPotDroppable is used by hearts and magic
        // powder directly. The other ordinary drops use the tree/pot init
        // handler, which selects the Pegasus branch outdoors and the buried
        // branch indoors.
        runtime.bossRewards()
                .writeDroppablePrivateState3(
                        slot,
                        entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART
                                        || entity.type() == EntityRomConstants.ENTITY_DROPPABLE_MAGIC_POWDER
                                        || entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                        || runtime.indoorRoom()
                                ? EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED
                                : EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS);
        runtime.bossRewards().writeDroppablePrivateState4(slot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        slot,
                        entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                                : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1);
    }

    static CommonDroppableUpdate advanceCommonDroppable(
            EntityPickupController owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            RoomEntityObjectSample objectUnderEntity) {
        return new DroppablePickupFrameStep(owner, entity, linkEntityX, linkEntityY, objectUnderEntity).run();
    }

    static void revealCommonDroppable(
            EntityPickupController owner, RoomEntity entity, int linkEntityX, int linkEntityY) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        runtime.bossRewards().writeDroppablePrivateState3(slot, 0);
        runtime.bossRewards().writeDroppablePrivateState4(slot, 0);
        runtime.bossRewards().writeDropPrivateCountdown1(slot, EntityRomConstants.DROPPABLE_REVEAL_COUNTDOWN);
        runtime.physicsTables().writeSlowTransitionCountdown(slot, EntityRomConstants.DROPPABLE_REVEAL_SLOW_COUNTDOWN);
        runtime.physicsTables().writeSlowTimerInitialized(slot, true);

        runtime.physicsTables().writeEntityOptions1Override(slot, EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            Vector vector = vectorAwayFromLink(
                    entity.x(),
                    entity.y(),
                    linkEntityX,
                    linkEntityY,
                    EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
            runtime.motions().get(FairyMotion.class, FairyMotion::new).setSpeed(slot, vector.x(), vector.y());
            return;
        }

        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(
                        slot,
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        EntityRomConstants.DROPPABLE_REVEAL_SPEED_Z);
        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initializeAwayFromLink(
                        slot,
                        entity.x(),
                        entity.y(),
                        linkEntityX,
                        linkEntityY,
                        EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
        runtime.bossRewards().writeEnemyDropActive(slot, true);
        runtime.physicsTables().writeEntityOptions1Override(slot, EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
    }

    static SlimeKeyTransitionUpdate advanceHidingSlimeKey(
            EntityPickupController owner, RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        var runtime = owner.runtime;
        int transition = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if (transition == 0) {
            return new SlimeKeyTransitionUpdate(entity, 0, false, false, null);
        }
        if (transition == 1) {
            // The handler decrements A locally for this branch and then takes
            // UnloadEntityAndReturn; the transition table byte itself is not
            // observed again because the entity is gone.
            return new SlimeKeyTransitionUpdate(entity, transition, false, true, null);
        }

        SlimeKeyRewardEvent reward = null;
        int nextTransition = transition;
        if (transition == 0x10) {
            nextTransition = 0x0F;
            int leaves = Math.min(0x63, runtime.roomVariables().readEntityGoldenLeavesCount() & 0xFF);
            if (!runtime.indoorRoom()
                    && runtime.roomVariables().readEntityRoomId()
                            == EntityRomConstants.ROOM_OW_POTHOLE_FIELD_SLIME_KEY) {
                leaves = EntityRomConstants.GOLDEN_LEAVES_FIVE_COUNT;
            }
            leaves = Math.min(0x63, leaves + 1);
            runtime.roomVariables().writeEntityGoldenLeavesCount(leaves);
            int dialogLowId = leaves == EntityRomConstants.GOLDEN_LEAVES_FINAL_COUNT
                    ? EntityRomConstants.DIALOG_SLIME_KEY
                    : leaves == EntityRomConstants.GOLDEN_LEAVES_FIVE_COUNT
                            ? EntityRomConstants.DIALOG_FINAL_GOLDEN_LEAF
                            : EntityRomConstants.DIALOG_GOLDEN_LEAF;
            reward = new SlimeKeyRewardEvent(slot, leaves, dialogLowId);
        }

        RoomEntity held = withPositionAndVariant(entity, linkEntityX, linkEntityY - 0x0C, entity.spriteVariant());
        held = withZ(held, linkZ);
        return new SlimeKeyTransitionUpdate(held, nextTransition, true, false, reward);
    }

    static void advanceHeartPiecePickup(EntityPickupController owner, RoomEntity entity, int x, int y, int z) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        var update = owner.readHeartPiecePickup(slot)
                .advance(
                        runtime.physicsTables().readEnemyTransitionCountdown(slot),
                        runtime.linkState().dialogActive,
                        owner.readHeartPiecesCount());
        runtime.events().heartPieceUpdates.add(update);
        if (update.incrementPiece()) owner.postHeartPiecesCount(1);
        if (update.newContainer()) owner.writeHeartPiecesCount(0);
        if (update.dialogId() >= 0) runtime.events().pendingDialogRequests.add(new DialogRequest(0, update.dialogId()));
        if (update.newContainer())
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x19));
        if (update.complete()) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        runtime.slots()[slot] = withZ(withPositionAndVariant(entity, x, (y - 12) & 255, entity.spriteVariant()), z);
    }
}
