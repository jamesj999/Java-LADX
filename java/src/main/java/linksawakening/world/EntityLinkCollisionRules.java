package linksawakening.world;

/**
 * Link-side effects emitted by entity handlers after a ROM hitbox collision.
 *
 * <p>The disassembly implements most of these collisions through one of the
 * bank-local {@code PushLinkOutOfEntity_*} helpers. The helper choice matters:
 * the urchin and a few stateful handlers write only a subset of the shared
 * side effects.</p>
 */
final class EntityLinkCollisionRules {
    static final LinkPushPolicy NONE = new LinkPushPolicy(false, false, false, false);
    static final LinkPushPolicy RESTORE_ONLY = new LinkPushPolicy(true, false, false, false);
    static final LinkPushPolicy STANDARD_PUSH = new LinkPushPolicy(true, true, true, false);
    static final LinkPushPolicy STANDARD_PUSH_WITH_SPEED_CLEAR =
        new LinkPushPolicy(true, true, true, false, true);
    static final LinkPushPolicy URCHIN = new LinkPushPolicy(true, false, false, false);
    static final LinkPushPolicy ZORA = new LinkPushPolicy(true, true, false, false);
    static final LinkPushPolicy PUSHED_BLOCK = new LinkPushPolicy(true, false, false, true);

    private EntityLinkCollisionRules() {
    }

    /** The side effects performed when an entity type reports a Link collision. */
    record LinkPushPolicy(boolean restoreFinalPosition, boolean resetPegasusBoots,
                          boolean resetHookshotChain, boolean markLinkPushing,
                          boolean clearLinkPositionIncrement) {
        LinkPushPolicy(boolean restoreFinalPosition, boolean resetPegasusBoots,
                       boolean resetHookshotChain, boolean markLinkPushing) {
            this(restoreFinalPosition, resetPegasusBoots, resetHookshotChain,
                markLinkPushing, false);
        }
    }

    /**
     * Returns the policy used by the source handler for an entity type.
     *
     * <p>Most entries below are the types that call one of the shared
     * {@code PushLinkOutOfEntity_04/05/06/07/15/18/19/36} helpers. Keeping the
     * table here makes the shared behavior explicit while leaving stateful
     * handler timing to {@link RoomEntityRuntime}.</p>
     */
    static LinkPushPolicy policyFor(int type) {
        return switch (type & 0xFF) {
            // Custom handlers which still copy the final position but do not
            // use one of the common bank helpers.
            case 0x06 -> PUSHED_BLOCK; // ENTITY_PUSHED_BLOCK
            case 0xCB -> ZORA;         // ENTITY_ZORA
            case 0xC5 -> URCHIN;       // ENTITY_URCHIN

            // PushLinkOutOfEntity_04.
            case 0x4D, 0x4F, 0x54, 0x5C -> STANDARD_PUSH;

            // PushLinkOutOfEntity_05.
            case 0x3E, 0x3F, 0x40, 0x6A, 0xD0 -> STANDARD_PUSH;

            // PushLinkOutOfEntity_06.
            case 0x41, 0x70, 0x71, 0x72, 0x73, 0x77, 0x79, 0x7B,
                0x7C, 0x7E, 0x88, 0x8C, 0x95 -> STANDARD_PUSH;

            // PushLinkOutOfEntity_07.
            case 0xA7, 0xAD, 0xB1, 0xB4, 0xB5, 0xB6, 0xB7 -> STANDARD_PUSH;

            // PushLinkOutOfEntity_15.
            case 0x66, 0xD1 -> STANDARD_PUSH_WITH_SPEED_CLEAR;

            // PushLinkOutOfEntity_18.
            case 0x42, 0x69, 0x74, 0x75, 0x76, 0x7F, 0xC1, 0xC3, 0xC4,
                0xC7, 0xCE, 0xD2, 0xD3 -> STANDARD_PUSH;

            // PushLinkOutOfEntity_19.
            case 0x8F, 0x9D, 0xCD, 0xD7, 0xD8, 0xD9, 0xDC ->
                STANDARD_PUSH_WITH_SPEED_CLEAR;

            // PushLinkOutOfEntity_36, including the photographer and color
            // guardian paths that share bank 36's helper.
            case 0xEF, 0xF0, 0xF1, 0xF6, 0xF7, 0xFA ->
                STANDARD_PUSH_WITH_SPEED_CLEAR;
            default -> NONE;
        };
    }

    /**
     * Returns only policies that can be applied by the runtime's generic
     * post-handler boundary. Custom ports issue their request at the source
     * call site so that movement/state ordering remains faithful.
     */
    static LinkPushPolicy genericPolicyFor(int type) {
        return switch (type & 0xFF) {
            case 0x06, 0x0F, 0x7C, 0x7E, 0x88, 0xC5, 0xCB -> NONE;
            default -> policyFor(type);
        };
    }
}
