package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.SecretSeashellMotion.*;

import linksawakening.world.entity.*;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;

/** Focused behavior using the state owned by SecretSeashellMotion. */
final class SecretSeashellBehavior {
    static SecretSeashellMotion.Update advance(SecretSeashellMotion owner, SecretSeashellFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = inputs.objectUnderEntity();
        return owner.advanceInternal(new SecretSeashellInteractionFrame(
                entity,
                roomId,
                frameCounter,
                roomTransitionActive,
                screenShakeActive,
                pegasusCollisionActive,
                false,
                entity.x() + 0x08,
                entity.y() + 0x08,
                linkEntityX,
                linkEntityY,
                objectUnderEntity));
    }

    static SecretSeashellMotion.Update advance(SecretSeashellMotion owner, SecretSeashellWorldFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        int pegasusCollisionX = inputs.pegasusCollisionX();
        int pegasusCollisionY = inputs.pegasusCollisionY();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = inputs.objectUnderEntity();
        return owner.advanceInternal(new SecretSeashellInteractionFrame(
                entity,
                roomId,
                frameCounter,
                roomTransitionActive,
                screenShakeActive,
                pegasusCollisionActive,
                true,
                pegasusCollisionX,
                pegasusCollisionY,
                linkEntityX,
                linkEntityY,
                objectUnderEntity));
    }

    static SecretSeashellMotion.Update advance(SecretSeashellMotion owner, SecretSeashellInteractionFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        boolean pegasusCollisionCoordinatesKnown = inputs.pegasusCollisionCoordinatesKnown();
        int pegasusCollisionX = inputs.pegasusCollisionX();
        int pegasusCollisionY = inputs.pegasusCollisionY();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = inputs.objectUnderEntity();
        return owner.advanceInternal(new SecretSeashellInteractionFrame(
                entity,
                roomId,
                frameCounter,
                roomTransitionActive,
                screenShakeActive,
                pegasusCollisionActive,
                pegasusCollisionCoordinatesKnown,
                pegasusCollisionX,
                pegasusCollisionY,
                linkEntityX,
                linkEntityY,
                objectUnderEntity));
    }

    static SecretSeashellMotion.Update advanceInternal(
            SecretSeashellMotion owner, SecretSeashellInteractionFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        boolean pegasusCollisionCoordinatesKnown = inputs.pegasusCollisionCoordinatesKnown();
        int pegasusCollisionX = inputs.pegasusCollisionX();
        int pegasusCollisionY = inputs.pegasusCollisionY();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = inputs.objectUnderEntity();
        if (entity == null) {
            throw new IllegalArgumentException("Secret seashell entity cannot be null");
        }
        int slot = entity.slot();
        owner.ensureInitialized(slot, roomId);
        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
        }
        if ((frameCounter & 0x03) == 0 && owner.slowTransitionCountdown[slot] > 0) {
            owner.slowTransitionCountdown[slot]--;
        }

        if (owner.privateState3[slot] != 0) {
            if (roomTransitionActive) {
                return new SecretSeashellMotion.Update(SecretSeashellMotion.withVariant(entity, -1), true);
            }

            boolean reveal = owner.privateState3[slot] == TREE_HIDDEN_STATE
                    ? screenShakeActive
                            && pegasusCollisionActive
                            && (!pegasusCollisionCoordinatesKnown
                                    || SecretSeashellMotion.withinPegasusCollisionWindow(
                                            entity.x() + 0x08, entity.y() + 0x08, pegasusCollisionX, pegasusCollisionY))
                    : owner.shouldRevealFromObject(roomId, objectUnderEntity, slot);
            if (reveal) {
                owner.revealAwayFromLink(slot, entity.x(), entity.y(), linkEntityX, linkEntityY);
                return new SecretSeashellMotion.Update(
                        SecretSeashellMotion.withVariant(
                                entity, entity.spriteDefinition().supported() ? 0 : -1),
                        false);
            }
            return new SecretSeashellMotion.Update(SecretSeashellMotion.withVariant(entity, -1), true);
        }

        int x = SecretSeashellMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
        int y = SecretSeashellMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
        int z = SecretSeashellMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 0x02) & 0xFF;
        return new SecretSeashellMotion.Update(
                SecretSeashellMotion.withPositionAndVariant(entity, x, y, z, entity.spriteVariant()), false);
    }
}
