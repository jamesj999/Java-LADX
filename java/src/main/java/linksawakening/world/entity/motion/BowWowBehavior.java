package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.BowWowMotion.*;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Focused behavior using the state owned by BowWowMotion. */
final class BowWowBehavior {
    static BowWowMotion.Update advanceWithTargetScan(BowWowMotion owner, BowWowTargetFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkEntityZ = inputs.linkEntityZ();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        java.util.List<RoomEntity> entities = inputs.entities();
        java.util.function.IntPredicate canEatEntity = inputs.canEatEntity();
        owner.tickTimers(entity.slot());
        return owner.advanceAfterTimers(new BowWowFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                randomByteSupplier,
                backgroundCollision,
                entities,
                canEatEntity));
    }

    static BowWowMotion.Update advanceAfterTimers(BowWowMotion owner, BowWowFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkEntityZ = inputs.linkEntityZ();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        java.util.List<RoomEntity> entities = inputs.entities();
        java.util.function.IntPredicate canEatEntity = inputs.canEatEntity();
        return owner.advanceAfterTimers(new BowWowCollisionFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                randomByteSupplier,
                backgroundCollision,
                entities,
                canEatEntity,
                1));
    }

    static BowWowMotion.Update advanceAfterTimers(BowWowMotion owner, BowWowCollisionFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkEntityZ = inputs.linkEntityZ();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        java.util.List<RoomEntity> entities = inputs.entities();
        java.util.function.IntPredicate canEatEntity = inputs.canEatEntity();
        int bowWowState = inputs.bowWowState();
        int slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        int variant = owner.selectedVariant[slot];
        // $05:4060 draws the saved variant; $44CB then selects the next one
        // from the existing speeds, before gravity and state dispatch.
        if ((owner.speedX[slot] | owner.speedY[slot]) != 0) {
            owner.selectedVariant[slot] = owner.movementVariant(frameCounter, slot);
        }

        if (owner.privateState4[slot] == 0) {
            return new BowWowMotion.Update(owner.setup(entity), owner.targetSlot[slot], null);
        }

        if (bowWowState != 0 && bowWowState != 0x80) {
            owner.targetX[slot] = BowWowMotion.byteValue(linkEntityX);
            // Kidnapped BowWow retains the pen anchor written by setup.
            // Following BowWow uses hLinkPositionZModified ($05:40E6).
            owner.targetY[slot] = BowWowMotion.byteValue(linkEntityY - linkEntityZ);
        }

        boolean wasAboveGround = owner.updateZ(entity.z(), slot);
        int x = entity.x();
        int y = entity.y();
        int z = owner.updatedZ[slot];
        BowWowMotion.TargetContact targetContact = null;

        switch (owner.activeState[slot]) {
            case 0 -> {
                if (owner.transitionCountdown[slot] == 0) {
                    owner.transitionCountdown[slot] = 0x28;
                    if (bowWowState == 0) {
                        owner.speedZ[slot] = 0x20;
                        owner.activeState[slot] = 2;
                        BowWowMotion.Vector vector = BowWowMotion.vectorTowardsTarget(
                                entity,
                                BowWowMotion.withPositionAndVariant(entity, linkEntityX, linkEntityY, 0, 0),
                                z,
                                0x20);
                        owner.speedX[slot] = vector.x();
                        owner.speedY[slot] = vector.y();
                        break;
                    }
                    // $05:4297 skips both target scanning and its RNG read
                    // while BowWow is chained inside the hideout.
                    int selectedTarget = bowWowState == 0x80
                            ? -1
                            : BowWowMotion.findTarget(
                                    entity, linkEntityX, linkEntityY, randomByteSupplier, entities, canEatEntity);
                    if (selectedTarget >= 0) {
                        RoomEntity target = entities.get(selectedTarget);
                        owner.targetSlot[slot] = selectedTarget;
                        BowWowMotion.Vector vector = BowWowMotion.vectorTowardsTarget(entity, target, z, 0x30);
                        owner.speedX[slot] = vector.x();
                        owner.speedY[slot] = vector.y();
                        owner.speedZ[slot] = 0x10;
                        owner.activeState[slot] = 4;
                    } else {
                        owner.transitionCountdown[slot] = 0x10;
                    }
                } else if (owner.privateCountdown1[slot] == 0) {
                    owner.privateCountdown1[slot] = 0x20 + (randomByteSupplier.getAsInt() & 0x3F);
                    owner.activeState[slot] = 1;
                    int direction = randomByteSupplier.getAsInt() & 0x07;
                    owner.speedX[slot] = SPEED_X_BY_RANDOM[direction];
                    owner.speedY[slot] = SPEED_Y_BY_RANDOM[direction];
                }
            }
            case 1 -> {
                if (owner.privateCountdown1[slot] == 0) {
                    owner.privateCountdown1[slot] = 0x20;
                    // func_005_41CF increments the state pointer, then writes
                    // B (zero), returning to the target-selection handler.
                    owner.activeState[slot] = 0;
                }
                if (wasAboveGround) {
                    owner.speedZ[slot] = 0x10;
                }
                int[] moved = owner.move(entity, x, y, slot, backgroundCollision);
                x = owner.correctTargetX(moved[0], x, slot);
                y = owner.correctTargetY(moved[1], y, slot);
            }
            case 2, 4 -> {
                boolean stopLunge = owner.transitionCountdown[slot] == 0;
                if (owner.transitionCountdown[slot] != 0) {
                    int[] moved = owner.move(entity, x, y, slot, backgroundCollision);
                    x = owner.correctTargetX(moved[0], x, slot);
                    y = owner.correctTargetY(moved[1], y, slot);
                    // func_005_4228 returns E>1 when either axis is restored;
                    // $41F6 then enters the same pause as an expired lunge.
                    stopLunge =
                            !BowWowMotion.inSignedWindow(BowWowMotion.signedByte(owner.targetX[slot] - moved[0]), 0x20)
                                    || !BowWowMotion.inSignedWindow(
                                            BowWowMotion.signedByte(owner.targetY[slot] - moved[1]), 0x20);
                }
                if (stopLunge) {
                    owner.speedX[slot] = 0;
                    owner.speedY[slot] = 0;
                    owner.activeState[slot] = 3;
                    owner.transitionCountdown[slot] = 0x10;
                }
                // func_005_41EB reaches label_005_4335 after every moving
                // state-2/state-4 frame, not only when the timer expires.
                if (bowWowState != 0) {
                    targetContact = BowWowMotion.findTargetContact(
                            entity.x(), entity.y(), entity.z(), owner.targetSlot[slot], entities);
                }
            }
            case 3 -> {
                if (owner.transitionCountdown[slot] == 0) {
                    // func_005_420E starts a ten-frame pause, then writes B
                    // (zero) back to wEntitiesStateTable for a fresh scan.
                    int pause = (randomByteSupplier.getAsInt() & 0x3F) + 0x30;
                    owner.transitionCountdown[slot] = bowWowState == 0 ? pause : 0x10;
                    owner.activeState[slot] = 0;
                }
            }
            default -> owner.activeState[slot] = 0;
        }

        return new BowWowMotion.Update(
                BowWowMotion.withPositionAndVariant(entity, x, y, z, variant), owner.targetSlot[slot], targetContact);
    }

    static int findTarget(
            RoomEntity source,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            List<RoomEntity> entities,
            IntPredicate canEatEntity) {
        boolean descending = (randomByteSupplier.getAsInt() & 0x01) == 0;
        int step = descending ? -1 : 1;
        int candidate = descending ? EntityRoomLoader.MAX_ENTITIES - 1 : 0;
        int end = descending ? -1 : EntityRoomLoader.MAX_ENTITIES;
        while (candidate != end) {
            if (candidate != source.slot() && candidate >= 0 && candidate < entities.size()) {
                RoomEntity target = entities.get(candidate);
                if (target.loaded()
                        && target.status() != EntityStatus.DYING
                        && target.spriteVariant() != 1
                        && canEatEntity.test(target.type())
                        && BowWowMotion.inUnsignedWindow(linkEntityX - target.x(), 0x2F, 0x5E)
                        && BowWowMotion.inUnsignedWindow(linkEntityY - target.y(), 0x2F, 0x5E)) {
                    return candidate;
                }
            }
            candidate += step;
        }
        return -1;
    }
}
