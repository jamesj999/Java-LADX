package linksawakening.world;

/** State-zero port of bank-$05's outdoor {@code TarinEntityHandler}. */
final class TarinRaccoonMotion {
    static final int ENTITY_TYPE = 0x3F;
    private static final int NO_EVENT = -1;

    record Input(int frameCounter, int linkX, int linkY, int linkDirection,
                 boolean actionHeld, boolean dialogActive, boolean powderHit) {
        Input(int frameCounter, int linkX, int linkY, boolean actionHeld,
              boolean dialogActive, boolean powderHit) {
            this(frameCounter, linkX, linkY, -1, actionHeld, dialogActive, powderHit);
        }

        Input {
            if (linkDirection < -1 || linkDirection > 3) {
                throw new IllegalArgumentException("Invalid Link direction: " + linkDirection);
            }
        }
    }

    record Update(RoomEntity entity, int state, int spriteVariant,
                  boolean shouldGetLost, int dialogGlobalId,
                  boolean linkMotionBlocked, boolean roomChanged,
                  boolean tarinFlag, int soundChannel, int soundId,
                  boolean spawnBomb) {
    }

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] warningShown = new boolean[EntityRoomLoader.MAX_ENTITIES];

    Update advance(RoomEntity source, Input input) {
        if (source.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException("Entity is not Tarin: " + source.type());
        }

        int slot = source.slot();
        int linkY = input.linkY() & 0xFF;
        boolean shouldGetLost = false;
        int variant = (input.frameCounter() >>> 4) & 1;

        if (state[slot] == 0) {
            if (linkY < 0x30) {
                shouldGetLost = true;
                variant = 2 + ((input.frameCounter() >>> 3) & 1);
            } else {
                warningShown[slot] = false;
            }
        }

        RoomEntity entity = withVariant(source, variant);
        int dialog = NO_EVENT;
        if (state[slot] == 0 && !input.dialogActive()) {
            if (linkY < 0x20 && !warningShown[slot]) {
                warningShown[slot] = true;
                dialog = 0x021;
            } else if (input.actionHeld() && nearbyAndFacing(entity, input)) {
                dialog = 0x00D;
            }
        }

        return new Update(entity, state[slot], variant, shouldGetLost, dialog,
            false, false, false, NO_EVENT, NO_EVENT, false);
    }

    private static boolean nearbyAndFacing(RoomEntity entity, Input input) {
        int yWindow = (input.linkY() - entity.y() + 0x14) & 0xFF;
        if (yWindow >= 0x28) {
            return false;
        }
        int xWindow = (input.linkX() - entity.x() + 0x10) & 0xFF;
        if (xWindow >= 0x20) {
            return false;
        }
        return input.linkDirection() != -1
            && directionToLink(entity, input.linkX(), input.linkY())
                == (input.linkDirection() ^ 0x01);
    }

    private static int directionToLink(RoomEntity entity, int linkX, int linkY) {
        int distanceX = (byte) (linkX - entity.x());
        int distanceY = (byte) (linkY - entity.y());
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static RoomEntity withVariant(RoomEntity source, int variant) {
        return new RoomEntity(source.slot(), source.sourceLoadOrder(), source.type(),
            source.x(), source.y(), source.status(), source.spriteDefinition(), variant,
            source.entityFlipAttribute(), source.spriteTileOffset(), source.z(),
            source.deathSpriteVariant(), source.powerRecoilDeath());
    }
}
