package linksawakening.world;

/** Source-shaped state-0 Link contact for entity $D6. */
final class SideViewPotContact {
    private static final int COLLISION_X = 0x08;
    private static final int COLLISION_Y = 0x08;

    private SideViewPotContact() {
    }

    record Result(boolean collided, boolean resetPegasusBoots,
                  boolean restoreFinalPositionX, int ignoreCollisionCountdown,
                  int speedX, boolean snapTop, int positionY, int speedY) {
        static Result none() {
            return new Result(false, false, false, 0, 0, false, 0, 0);
        }
    }

    /** CheckLinkCollisionWithEnemy using D6's hitbox flags $BD/$3C. */
    static boolean overlaps(RoomEntity entity, int linkPositionX, int linkPositionY) {
        int xDifference = signedByte(entity.x() - linkPositionX);
        if (Math.abs(xDifference) >= COLLISION_X + 0x04) {
            return false;
        }
        // HitboxPositions.$3C is (x=$08, y=$08, width=$02, height=$08)
        // in source storage; CheckLinkCollision uses the visual Y plus the
        // second coordinate byte ($02), then subtracts Link's $08 origin.
        int yDifference = signedByte((entity.y() - entity.z() + 0x02)
            - linkPositionY - 0x08);
        return Math.abs(yDifference) < COLLISION_Y + 0x04;
    }

    /**
     * Mirrors SideViewPotState0Handler and func_019_599B. The input positions
     * are the ROM hLinkPositionX/Y and entity position bytes.
     */
    static Result resolve(RoomEntity entity, int linkPositionX, int linkPositionY,
                          boolean linkAirborne, int linkSpeedY) {
        if (!overlaps(entity, linkPositionX, linkPositionY)) {
            return Result.none();
        }

        int yDifference = signedByte(linkPositionY - entity.y());
        boolean nearVertical = ((yDifference + 0x03) & 0xFF) < 0x06;
        // func_019_599B calls ResetPegasusBoots before its airborne branch.
        boolean resetPegasusBoots = nearVertical;
        boolean restoreFinalPositionX = false;
        int ignoreCollisionCountdown = 0;
        int speedX = 0;
        if (nearVertical) {
            if (linkAirborne) {
                restoreFinalPositionX = true;
            } else {
                ignoreCollisionCountdown = 0x02;
                // EntityLinkPositionXDifference's raw sign is the source
                // branch: zero and positive bytes push right, negative left.
                speedX = signedByte(linkPositionX - entity.x()) < 0 ? 0xF0 : 0x10;
            }
        }

        boolean snapTop = (linkSpeedY & 0x80) == 0
            && (((yDifference + 0x08) & 0xFF) & 0x80) != 0;
        return new Result(true, resetPegasusBoots, restoreFinalPositionX,
            ignoreCollisionCountdown, speedX, snapTop,
            (entity.y() - 0x10) & 0xFF, snapTop ? 0x02 : 0);
    }

    static boolean liftWindow(RoomEntity entity, int linkPositionX, int linkPositionY) {
        return withinSignedWindow(linkPositionX - entity.x(), 0x12)
            && withinSignedWindow(linkPositionY - entity.y(), 0x12);
    }

    private static boolean withinSignedWindow(int difference, int radius) {
        return (((difference + radius) & 0xFF) < radius * 2);
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }
}
