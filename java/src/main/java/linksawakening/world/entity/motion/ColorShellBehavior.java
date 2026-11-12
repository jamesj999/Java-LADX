package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.ColorShellMotion.*;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Focused behavior using the state owned by ColorShellMotion. */
final class ColorShellBehavior {
    static ColorShellMotion.Update advance(
            ColorShellMotion owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return owner.advance(new ColorShellFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                0,
                ColorShellWorld.none(),
                List.of(entity)));
    }

    static ColorShellMotion.Update advance(
            ColorShellMotion owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int ignoreHitsCountdown) {
        return owner.advance(new ColorShellFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                ignoreHitsCountdown,
                ColorShellWorld.none(),
                List.of(entity)));
    }

    static ColorShellMotion.Update advance(ColorShellMotion owner, ColorShellFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        int ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        ColorShellWorld world = inputs.world();
        java.util.List<RoomEntity> allEntities = inputs.allEntities();
        if (entity == null || randomByteSupplier == null) {
            throw new IllegalArgumentException("Color Shell motion inputs cannot be null");
        }
        int slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        owner.positionX[slot] = entity.x();
        owner.positionY[slot] = entity.y();
        owner.positionZ[slot] = entity.z();
        owner.decrementTransitionCountdown(slot);

        ColorShellWorld effects = world == null ? ColorShellWorld.none() : world;
        List<RoomEntity> entities = allEntities == null ? List.of(entity) : allEntities;
        int frame = frameCounter & 0xFF;
        int dispatchedState = owner.state[slot];
        int nextIgnoreHitsCountdown = ignoreHitsCountdown & 0xFF;
        boolean unloadRequested = false;
        switch (dispatchedState) {
            case 0 -> owner.advanceState0(linkEntityX, linkEntityY, randomByteSupplier, slot);
            case 1 -> owner.advanceState1(frame, linkEntityX, linkEntityY, backgroundCollision, slot, entity);
            case 2 -> owner.advanceState2(frame, ignoreHitsCountdown, slot);
            case 3 -> owner.advanceState3(frame, ignoreHitsCountdown, backgroundCollision, slot, entity);
            case 4 -> nextIgnoreHitsCountdown = owner.advanceState4(slot);
            case 5 -> nextIgnoreHitsCountdown = owner.advanceState5(entity, ignoreHitsCountdown, slot);
            case 6 -> owner.advanceState6(slot);
            case 7 -> owner.advanceState7(slot);
            case 8 -> owner.advanceState8(entity, effects, slot);
            case 9 -> owner.advanceState9(backgroundCollision, slot, entity);
            case 0x0A -> owner.advanceStateA(backgroundCollision, slot, entity);
            case 0x0B -> owner.advanceStateB(slot);
            case 0x0C -> owner.advanceStateC(entity, effects, entities, slot);
            case 0x0D -> unloadRequested = owner.advanceStateD(entity, effects, slot);
            default -> throw new IllegalStateException("Invalid Color Shell state: " + owner.state[slot]);
        }

        RoomEntity updated = EntitySnapshots.withMotion(
                entity,
                owner.positionX[slot],
                owner.positionY[slot],
                owner.spriteVariant[slot],
                entity.entityFlipAttribute(),
                owner.positionZ[slot],
                -1,
                false);
        return new ColorShellMotion.Update(updated, nextIgnoreHitsCountdown, unloadRequested);
    }

    static void advanceState1(
            ColorShellMotion owner,
            int frame,
            int linkEntityX,
            int linkEntityY,
            RoomEntityBackgroundCollision backgroundCollision,
            int slot,
            RoomEntity entity) {
        int currentDirection = owner.direction[slot] & 0x03;
        owner.speedX[slot] = SPEED_X_BY_DIRECTION[currentDirection];
        owner.speedY[slot] = SPEED_Y_BY_DIRECTION[currentDirection];
        owner.moveWithBackground(entity, backgroundCollision, true, slot);

        if (owner.transitionCountdown[slot] == 0) {
            owner.transitionCountdown[slot] = 0x10;
            owner.state[slot] = 0;
            if (ColorShellMotion.unsignedDistance(linkEntityX, owner.positionX[slot]) < 0x20
                    && ColorShellMotion.unsignedDistance(linkEntityY, owner.positionY[slot]) < 0x20) {
                ColorShellMotion.Vector vector = ColorShellMotion.vectorTowardsLink(
                        owner.positionX[slot], owner.positionY[slot], linkEntityX, linkEntityY, 0x0E);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                owner.transitionCountdown[slot] = 0x20;
                owner.state[slot] = 2;
            }
        }
        if ((frame & 0x07) == 0) {
            owner.spriteVariant[slot] = (owner.spriteVariant[slot] + 1) & 0x01;
        }
    }

    static ColorShellMotion.Vector vectorTowardsLink(
            int entityX, int entityY, int linkEntityX, int linkEntityY, int length) {
        int distanceX = ColorShellMotion.signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = ColorShellMotion.signedByte((linkEntityY - entityY) & 0xFF);
        boolean yIsLargerAxis = Math.abs(distanceY) > Math.abs(distanceX);
        int smallerDistance = Math.min(Math.abs(distanceX), Math.abs(distanceY));
        int largerDistance = Math.max(Math.abs(distanceX), Math.abs(distanceY));
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance != 0 && sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new ColorShellMotion.Vector(x & 0xFF, y & 0xFF);
    }
}
