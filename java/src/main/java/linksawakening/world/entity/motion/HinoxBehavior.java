package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.HinoxMotion.*;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Focused behavior using the state owned by HinoxMotion. */
final class HinoxBehavior {
    static HinoxMotion.Update advance(HinoxMotion owner, HinoxFrame inputs) {
        return owner.hinoxStep.run(owner, inputs);
    }

    static HinoxMotion.Position move(
            HinoxMotion owner, RoomEntity entity, int x, int y, RoomEntityBackgroundInteraction background, int slot) {
        owner.entityCollisionFlags[slot] = 0;
        int nextX = HinoxMotion.addSpeed(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        if (nextX != x && background != null) {
            EntityBackgroundCollisionResult result =
                    background.probe(entity, HinoxMotion.horizontalDirection(owner.speedX[slot]), nextX, y);
            if (result.blocked()) {
                owner.entityCollisionFlags[slot] |= result.collisionFlag();
                nextX = x;
            }
        }
        int nextY = HinoxMotion.addSpeed(y, owner.speedY[slot], owner.speedYAccumulator, slot);
        if (nextY != y && background != null) {
            EntityBackgroundCollisionResult result =
                    background.probe(entity, HinoxMotion.verticalDirection(owner.speedY[slot]), nextX, nextY);
            if (result.blocked()) {
                owner.entityCollisionFlags[slot] |= result.collisionFlag();
                nextY = y;
            }
        }
        return new HinoxMotion.Position(nextX, nextY);
    }
}
