package linksawakening.world;

/** Small deterministic runtime portion of bank-$06's Goponga flower handler. */
final class GopongaFlowerMotion {
    static final int ENTITY_TYPE = 0x7E;
    static final int INITIAL_PHYSICS_FLAGS = 0x02;
    static final int OPTIONS1 = 0x02;

    private GopongaFlowerMotion() {
    }

    /** Mirrors hFrameCounter & $30 followed by the handler's inc e. */
    static int frameVariant(int frameCounter) {
        return (frameCounter & 0x30) == 0 ? 0 : 1;
    }

    /**
     * PushLinkOutOfEntity_06 calls collisionEvenInTheAir, so Link's Z byte is
     * deliberately not part of this predicate.
     */
    static boolean overlapsInteractiveLink(RoomEntity entity, int linkEntityX,
                                            int linkEntityY,
                                            boolean handlerLinkCollisionEnabled) {
        return handlerLinkCollisionEnabled
            && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
    }
}
