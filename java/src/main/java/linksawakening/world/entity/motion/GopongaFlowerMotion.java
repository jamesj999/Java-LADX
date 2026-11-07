package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityCombatRules;

/** Small deterministic runtime portion of bank-$06's Goponga flower handler. */
public final class GopongaFlowerMotion {
    public static final int ENTITY_TYPE = 0x7E;
    public static final int INITIAL_PHYSICS_FLAGS = 0x02;
    public static final int OPTIONS1 = 0x02;

    private GopongaFlowerMotion() {
    }

    /** Mirrors hFrameCounter & $30 followed by the handler's inc e. */
    public static int frameVariant(int frameCounter) {
        return (frameCounter & 0x30) == 0 ? 0 : 1;
    }

    /**
     * PushLinkOutOfEntity_06 calls collisionEvenInTheAir, so Link's Z byte is
     * deliberately not part of this predicate.
     */
    public static boolean overlapsInteractiveLink(RoomEntity entity, int linkEntityX,
                                            int linkEntityY,
                                            boolean handlerLinkCollisionEnabled) {
        return handlerLinkCollisionEnabled
            && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
    }
}
