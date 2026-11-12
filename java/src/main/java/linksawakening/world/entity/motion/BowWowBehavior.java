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
        return new BowWowStep(owner, inputs).run();
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
