package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.TarinRaccoonMotion.*;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Focused behavior using the state owned by TarinRaccoonMotion. */
final class TarinRaccoonBehavior {
    static TarinRaccoonMotion.Update advance(
            TarinRaccoonMotion owner, RoomEntity source, TarinRaccoonMotion.Input input) {
        return owner.advance(source, input, null);
    }

    static TarinRaccoonMotion.Update advance(
            TarinRaccoonMotion owner,
            RoomEntity source,
            TarinRaccoonMotion.Input input,
            RoomEntityBackgroundInteraction backgroundInteraction) {
        return new TarinRaccoonStep(owner, source, input, backgroundInteraction).run();
    }

    static TarinRaccoonMotion.Movement moveWithBackground(
            TarinRaccoonMotion owner, RoomEntity entity, RoomEntityBackgroundInteraction interaction, int slot) {
        int x = TarinRaccoonMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.accumulatorX, slot);
        int y = TarinRaccoonMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.accumulatorY, slot);
        int flags = 0;
        if (interaction != null && x != entity.x()) {
            int direction = TarinRaccoonMotion.signedByte(owner.speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, entity.y());
            if (result.blocked()) {
                flags |= result.collisionFlag();
                x = entity.x();
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = TarinRaccoonMotion.signedByte(owner.speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                flags |= result.collisionFlag();
                y = entity.y();
            }
        }
        return new TarinRaccoonMotion.Movement(x & 0xFF, y & 0xFF, flags & 0x0F);
    }
}
