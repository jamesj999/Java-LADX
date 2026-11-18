package linksawakening.world;

import java.util.function.IntSupplier;

/** One villa frog's fractional position, hop velocity and rest timer (bank 19). */
final class RichardFrogMotion {
    private static final int[] X_SPEED = {0, 16, 0, -16, 12, 12, -12, -12};
    private static final int[] Y_SPEED = {-16, 0, 16, 0, -12, 12, 12, -12};
    private static final int[] FACING = {0, 6, 2, 4, 0, 6, 2, 4};
    private int fractionX, fractionY, fractionZ;
    private int speedX, speedY, speedZ, facing, rest;
    private boolean jumping;

    RoomEntity tick(RoomEntity frog, IntSupplier random, RoomEntityBackgroundInteraction walls, int frame) {
        fractionX += speedX;
        fractionY += speedY;
        fractionZ += speedZ;
        int x = (frog.x() + (fractionX >> 4)) & 255;
        int y = (frog.y() + (fractionY >> 4)) & 255;
        int z = frog.z() + (fractionZ >> 4);
        fractionX &= 15;
        fractionY &= 15;
        fractionZ &= 15;
        if (walls != null && x != frog.x() && walls.probe(frog, speedX < 0 ? EntityBackgroundCollisionResult.LEFT
            : EntityBackgroundCollisionResult.RIGHT, x, frog.y(), 0, frame).blocked()) x = frog.x();
        if (walls != null && y != frog.y() && walls.probe(frog, speedY < 0 ? EntityBackgroundCollisionResult.UP
            : EntityBackgroundCollisionResult.DOWN, x, y, 0, frame).blocked()) y = frog.y();
        speedZ -= 2;
        boolean landed = z < 0;
        if (landed) {
            z = 0;
            speedZ = 0;
        }
        int variant = facing;
        if (jumping && landed) {
            jumping = false;
            speedX = speedY = 0;
            rest = 16 + (random.getAsInt() & 31);
            variant = frog.spriteVariant();
        } else if (jumping) {
            variant = facing + 1;
        } else if (rest == 0 || --rest == 0) {
            speedZ = 16 | (random.getAsInt() & 31);
            int heading = random.getAsInt() & 7;
            speedX = X_SPEED[heading];
            speedY = Y_SPEED[heading];
            facing = FACING[heading];
            variant = facing;
            jumping = true;
        }
        return new RoomEntity(frog.slot(), frog.sourceLoadOrder(), frog.type(), x, y, frog.status(),
            frog.spriteDefinition(), variant, frog.entityFlipAttribute(), frog.spriteTileOffset(), z);
    }
}
