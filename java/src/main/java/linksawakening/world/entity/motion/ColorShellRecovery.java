package linksawakening.world.entity.motion;

import java.util.List;
import linksawakening.world.entity.*;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Focused behavior using the state owned by ColorShellMotion. */
final class ColorShellRecovery {
    static void advanceState9(
            ColorShellMotion owner, RoomEntityBackgroundCollision backgroundCollision, int slot, RoomEntity entity) {
        if (owner.transitionCountdown[slot] != 0) {
            return;
        }
        if (owner.speedX[slot] != 0) {
            owner.moveWithBackground(entity, backgroundCollision, false, slot);
        }
        owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
        owner.positionZ[slot] = ColorShellMotion.addSpeedToPosition(
                owner.positionZ[slot], owner.speedZ[slot], owner.speedZAccumulator, slot);
        if ((owner.positionZ[slot] & 0x80) != 0) {
            owner.positionZ[slot] = 0;
            owner.speedZ[slot] = 0x08;
            owner.speedX[slot] = ColorShellMotion.arithmeticShiftRight(owner.speedX[slot]);
            owner.state[slot] = 0x0A;
        }
    }

    static void advanceStateA(
            ColorShellMotion owner, RoomEntityBackgroundCollision backgroundCollision, int slot, RoomEntity entity) {
        owner.moveWithBackground(entity, backgroundCollision, false, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
        owner.positionZ[slot] = ColorShellMotion.addSpeedToPosition(
                owner.positionZ[slot], owner.speedZ[slot], owner.speedZAccumulator, slot);
        if ((owner.positionZ[slot] & 0x80) != 0) {
            owner.transitionCountdown[slot] = 0x20;
            owner.state[slot]++;
        }
    }

    static void advanceStateB(ColorShellMotion owner, int slot) {
        if (owner.transitionCountdown[slot] != 0) {
            return;
        }
        owner.transitionCountdown[slot] = 0;
        owner.speedZ[slot] = 0;
        owner.speedX[slot] = 0;
        owner.state[slot] = 1;
        owner.physicsFlags[slot] &= 0x7F;
    }

    static void advanceStateC(
            ColorShellMotion owner, RoomEntity entity, ColorShellWorld world, List<RoomEntity> allEntities, int slot) {
        for (RoomEntity other : allEntities) {
            if (other == null
                    || !ColorShellMotion.isColorShellType(other.type())
                    || !other.loaded()
                    || other.status().value() == 0
                    || owner.state[other.slot()] >= 0x0C) {
                continue;
            }
            return;
        }
        int color = entity.type() - ColorShellMotion.ENTITY_COLOR_SHELL_RED;
        owner.transitionCountdown[slot] = 0x18;
        owner.state[slot] = 0x0D;
        world.writeObject(entity, 0x67 + color);
    }

    static boolean advanceStateD(ColorShellMotion owner, RoomEntity entity, ColorShellWorld world, int slot) {
        if (owner.transitionCountdown[slot] != 0) {
            return false;
        }
        int color = entity.type() - ColorShellMotion.ENTITY_COLOR_SHELL_RED;
        world.writeObject(entity, ColorShellMotion.PUZZLE_FINAL_OBJECTS[color]);
        world.spawnPoof(owner.positionX[slot], (owner.positionY[slot] - owner.positionZ[slot]) & 0xFF);
        return true;
    }
}
