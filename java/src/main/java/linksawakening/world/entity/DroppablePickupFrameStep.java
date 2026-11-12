package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate;
import linksawakening.world.entity.motion.*;

/** Reused, reentrancy-safe scratch for DroppablePickupBehavior.advanceCommonDroppable; values are assigned in source order. */
final class DroppablePickupFrameStep {
    private linksawakening.world.entity.EntityPickupController owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int linkEntityX;
    private int linkEntityY;
    private linksawakening.world.entity.RoomEntityObjectSample objectUnderEntity;
    private linksawakening.world.entity.EntityPickupControllerServices runtime;
    private int slot;
    private int hiddenState;

    DroppablePickupFrameStep() {}

    linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate run() {
        runtime = owner.runtime;
        slot = entity.slot();
        hiddenState = runtime.bossRewards().readDroppablePrivateState3(slot);
        if (hiddenState != 0) {
            boolean reveal = false;
            if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED) {
                // DroppableRevealOrReturnIfNeeded deliberately keeps ordinary
                // buried drops invisible indoors. Outdoors, hearts reveal on
                // short grass or a shovel hole; the remaining shared drops
                // require the shovel-hole path and set privateState4 first.
                if (!runtime.indoorRoom()) {
                    int objectId = objectUnderEntity == null ? 0xFF : objectUnderEntity.objectId() & 0xFF;
                    if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART) {
                        reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHORT_GRASS
                                || objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    } else {
                        runtime.bossRewards().writeDroppablePrivateState4(slot, 1);
                        reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    }
                }
            } else if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS) {
                reveal = runtime.motions()
                                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                .readSecretSeashellScreenShakeActive()
                        && runtime.motions()
                                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                .readSecretSeashellPegasusCollisionActive()
                        && (!runtime.motions()
                                        .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                        .readSecretSeashellPegasusCollisionCoordinatesKnown()
                                || withinUnsignedWindow(
                                                entity.x() + 0x08,
                                                runtime.motions()
                                                        .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                        .readSecretSeashellPegasusCollisionX(),
                                                0x10)
                                        && withinUnsignedWindow(
                                                entity.y() + 0x08,
                                                runtime.motions()
                                                        .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                        .readSecretSeashellPegasusCollisionY(),
                                                0x10));
            }

            if (!reveal) {
                runtime.physicsTables()
                        .writeEntityOptions1Override(
                                slot,
                                entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                        ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                                        : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1);
                return new CommonDroppableUpdate(withVariant(entity, -1), true);
            }
            owner.revealCommonDroppable(entity, linkEntityX, linkEntityY);
            // $629A falls through to .remainInvisible ($629C): POP AF removes
            // the caller return address even on the reveal pass. Drawing,
            // collection and BouncingEntityPhysics start next frame.
            return new CommonDroppableUpdate(withVariant(entity, -1), true);
        }

        runtime.physicsTables()
                .writeEntityOptions1Override(
                        slot,
                        hiddenState == 0
                                ? runtime.physicsTables().readEntityOptions1Override(slot)
                                : EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
        if (entity.type() != EntityRomConstants.ENTITY_DROPPABLE_FAIRY
                && !runtime.bossRewards().readEnemyDropActive(slot)) {
            runtime.motions()
                    .get(EnemyDropMotion.class, EnemyDropMotion::new)
                    .initialize(slot, runtime.physicsTables().readGroundInteractionSideScrolling());
            runtime.bossRewards().writeEnemyDropActive(slot, true);
        }
        return new CommonDroppableUpdate(
                withVariant(entity, entity.spriteDefinition().supported() ? 0 : -1), false);
    }

    private boolean inUse;

    linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate run(
            linksawakening.world.entity.EntityPickupController owner,
            linksawakening.world.entity.RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            linksawakening.world.entity.RoomEntityObjectSample objectUnderEntity) {
        if (inUse) {
            return new DroppablePickupFrameStep().run(owner, entity, linkEntityX, linkEntityY, objectUnderEntity);
        }
        inUse = true;
        runtime = null;
        slot = 0;
        hiddenState = 0;
        this.owner = owner;
        this.entity = entity;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.objectUnderEntity = objectUnderEntity;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.objectUnderEntity = null;
            this.runtime = null;
        }
    }
}
