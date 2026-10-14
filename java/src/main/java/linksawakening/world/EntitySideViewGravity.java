package linksawakening.world;

/** Side-view branch of bank-$03's shared gravity routine, {@code func_003_6B7B}. */
final class EntitySideViewGravity {
    private static final int[] GRAVITY = {0x02, 0x01, 0x02, 0x02};
    private static final int[] SPEED_CAP = {0x40, 0x08, 0x40, 0x40};

    private EntitySideViewGravity() {
    }

    static Speeds advance(int speedX, int speedY, int frameCounter, int groundStatus) {
        int status = groundStatusIndex(groundStatus);
        speedX &= 0xFF;
        if (status != 0 && (frameCounter & 0x07) == 0 && speedX != 0) {
            speedX = (speedX + ((speedX & 0x80) != 0 ? 1 : -1)) & 0xFF;
        }
        speedY = (speedY + GRAVITY[status]) & 0xFF;
        int cap = SPEED_CAP[status];
        if (((speedY - cap) & 0x80) == 0) {
            speedY = cap;
        }
        return new Speeds(speedX, speedY);
    }

    static int groundStatusIndex(int groundStatus) {
        int status = groundStatus & 0xFF;
        if (status >= GRAVITY.length) {
            throw new IllegalArgumentException("Entity ground status out of range: " + status);
        }
        return status;
    }

    record Speeds(int x, int y) {
    }
}
