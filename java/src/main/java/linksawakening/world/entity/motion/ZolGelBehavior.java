package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.ZolGelMotion.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Focused behavior using the state owned by ZolGelMotion. */
final class ZolGelBehavior {
    static ZolGelMotion.Update advance(
            ZolGelMotion owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return owner.advance(new ZolGelFrame(
                entity,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                randomByteSupplier,
                backgroundCollision == null ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision),
                0,
                0,
                false));
    }

    static ZolGelMotion.Update advance(ZolGelMotion owner, ZolGelFrame inputs) {
        return owner.zolGelStep.run(owner, inputs);
    }

    static int[] move(
            ZolGelMotion owner,
            RoomEntity entity,
            int x,
            int y,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter) {
        int slot = entity.slot();
        int movedX = ZolGelMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        int movedY = ZolGelMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
        if (backgroundInteraction != null) {
            if (movedX != x
                    && backgroundInteraction
                            .probe(
                                    entity,
                                    ZolGelMotion.signedByte(owner.speedX[slot]) < 0 ? 1 : 0,
                                    movedX,
                                    y,
                                    ignoreHitsCountdown,
                                    frameCounter)
                            .blocked()) {
                movedX = x;
            }
            if (movedY != y
                    && backgroundInteraction
                            .probe(
                                    entity,
                                    ZolGelMotion.signedByte(owner.speedY[slot]) < 0 ? 2 : 3,
                                    movedX,
                                    movedY,
                                    ignoreHitsCountdown,
                                    frameCounter)
                            .blocked()) {
                movedY = y;
            }
        }
        return new int[] {movedX, movedY};
    }

    static ZolGelMotion.Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY, int length) {
        int distanceX = ZolGelMotion.signedByte((linkX - entityX) & 0xFF);
        int distanceY = ZolGelMotion.signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance != 0 && sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new ZolGelMotion.Vector(x & 0xFF, y & 0xFF);
    }
}
