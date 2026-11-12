package linksawakening.entity;
/** Fallback ROM walking, shield, jump and fall animation-state tables for UpdateLinkAnimation. */
final class LinkPoseTables {
    private LinkPoseTables() {}

    static final int[][] ANIMATION_STATE = {
        {0x00, 0x01}, // DOWN:  standing, walking
        {0x04, 0x05}, // UP
        {0x06, 0x07}, // LEFT
        {0x0A, 0x0B}, // RIGHT
    };

    static final int[][] SHIELD_USE_ANIMATION_STATE = {
        {0x24, 0x25}, // DOWN
        {0x30, 0x31}, // UP
        {0x28, 0x29}, // LEFT
        {0x2A, 0x2B}, // RIGHT
    };

    static final int[][] MIRROR_SHIELD_USE_ANIMATION_STATE = {
        {0x26, 0x27}, // DOWN
        {0x32, 0x33}, // UP
        {0x28, 0x29}, // LEFT
        {0x2E, 0x2F}, // RIGHT
    };

    static final int[][] LIFTING_ANIMATION_STATE = {
        {0x44, 0x45}, // DOWN
        {0x42, 0x43}, // UP
        {0x40, 0x41}, // LEFT
        {0x3E, 0x3F}, // RIGHT
    };

    static final int[][] SWIMMING_ANIMATION_STATE = {
        {0x4C, 0x4D}, // DOWN
        {0x4A, 0x4B}, // UP
        {0x48, 0x49}, // LEFT
        {0x46, 0x47}, // RIGHT
    };

    static final int[] DIVING_ANIMATION_STATE = {0x4E, 0x4F};

    static final int[][] JUMP_ANIMATION_STATE = {
        {0x64, 0x65, 0x66}, // DOWN
        {0x67, 0x68, 0x69}, // UP
        {0x5E, 0x5F, 0x60}, // LEFT
        {0x61, 0x62, 0x63}, // RIGHT
    };

    static final int[] FALL_ANIMATION_STATE = {0x55, 0x56, 0x57, 0x57};
    static final int[] UNSHIELDED_WALK_BASE = {0x1A, 0x1C, 0x1E, 0x20};
}
