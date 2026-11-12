package linksawakening.entity;

import static linksawakening.entity.Link.*;

import linksawakening.state.PlayerState;

/** Constants and lookup tables mirroring the ROM Link motion paths. */
final class LinkRomConstants {
    private LinkRomConstants() {}

    // Bit order matches JoypadToLinkDirection (bank2.asm:1183): entry 1 = right,
    // 2 = left, 4 = up, 8 = down.
    static final int JOY_RIGHT = 1 << 0;
    static final int JOY_LEFT = 1 << 1;
    static final int JOY_UP = 1 << 2;
    static final int JOY_DOWN = 1 << 3;

    // JoypadToLinkDirection table from bank2.asm:1183 — any two-axis combination
    // keeps the previous facing. Encoded as one of DIRECTION_* or -1 (= KEEP).
    static final int[] JOYPAD_TO_DIRECTION = {
        -1, //  0000 none       -> keep
        DIRECTION_RIGHT, //  0001 right
        DIRECTION_LEFT, //  0010 left
        -1, //  0011 right+left -> keep
        DIRECTION_UP, //  0100 up
        -1, //  0101 up+right   -> keep
        -1, //  0110 up+left    -> keep
        -1, //  0111 keep
        DIRECTION_DOWN, //  1000 down
        -1, //  1001 down+right -> keep
        -1, //  1010 down+left  -> keep
        -1, //  1011 keep
        -1, //  1100 down+up    -> keep
        -1, //  1101 keep
        -1, //  1110 keep
        -1, //  1111 keep
    };

    // hLinkAnimationState values from gameplay.asm. Index = direction, inner
    // index = walking-frame bit (0 = standing, 1 = walking alt). These match
    // LinkAnimationsList_WalkingNoShield (bank2.asm:1208).

    // LinkAnimationsList_WalkUsingDefaultShield and the corresponding mirror
    // list in bank2.asm:1214-1240. Java direction order is DOWN, UP, LEFT,
    // RIGHT, matching the ordinary walking table above.

    // LinkAnimationsList_LiftingObject (bank2.asm:1249), indexed in the
    // same Java direction order as the walking table.

    // Data_002_4948 (bank2.asm:1257), in Java direction order. The second
    // row is selected while hLinkPhysicsModifier is non-zero (diving).

    static final int WALK_FRAME_TICKS = 8;
    static final int JUMP_FRAME_TICKS = 8;
    static final int FALL_FRAME_TICKS = 16;
    static final int FALL_DURATION_FRAMES = 96;
    static final int PIT_RECOVERY_INVINCIBILITY_FRAMES = 0x40;
    static final int PIT_DAMAGE = PlayerState.HP_PER_HEART / 2;
    static final int ATTACK_STEP_ITEM_ANY = 0x00;
    static final int ATTACK_STEP_DURATION_MASK = 0x7F;
    static final int ROM_ITEM_ATTACK_STEP_COUNTDOWN = 0x0C | ATTACK_STEP_ITEM_ANY;
    static final int COLLISION_TYPE_UP = 0x01;
    static final int COLLISION_TYPE_DOWN = 0x02;
    static final int COLLISION_TYPE_LEFT = 0x04;
    static final int COLLISION_TYPE_RIGHT = 0x08;
    static final int PEGASUS_BOOTS_MAX_CHARGE = 0x20;
    static final int PEGASUS_BOOTS_RUNNING_SPEED = 0x20;
    static final int PEGASUS_BOOTS_COLLISION_COUNTDOWN = 0x02;
    static final int PEGASUS_BOOTS_COLLISION_SHAKE = 0x20;
    static final int PEGASUS_BOOTS_COLLISION_VELOCITY_Z = 0x18;

    // Two collision check points per direction, as offsets from the
    // sprite's top-left. Ported from LinkCollisionPointsX/Y
    // (bank2.asm:6302-6319). Only the leading edge of movement is
    // checked, which is why Link can walk out of a "stuck in tree row"
    // position by pressing UP (top-edge points are in the clear row)
    // even though his feet are still overlapping a solid cell below.
    //
    // Indexed by DIRECTION_* constants below.
    static final int[][] COLLISION_POINTS_X = {
        {6, 9}, // DOWN
        {6, 9}, // UP
        {4, 4}, // LEFT
        {11, 11}, // RIGHT
    };
    static final int[][] COLLISION_POINTS_Y = {
        {15, 15}, // DOWN — bottom edge
        {6, 6}, // UP — top edge (not 0; matches LA's offset)
        {9, 12}, // LEFT — middle-left
        {9, 12}, // RIGHT — middle-right
    };
}
