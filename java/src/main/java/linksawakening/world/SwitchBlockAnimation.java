package linksawakening.world;

import java.util.List;

/**
 * The stage/state portion of UpdateSwitchBlockTiles from bank0.asm.
 *
 * <p>The ROM starts this sequence at stage {@code $01}; one VBlank update
 * increments the stage before selecting its copy. The source only produces
 * switch states {@code $00} and {@code $02}, so other values are rejected
 * instead of being interpreted as guessed table indices.</p>
 */
public final class SwitchBlockAnimation {
    static final int LOWERED_STATE = 0x00;
    static final int RAISED_STATE = 0x02;
    static final int SWITCH_BLOCK_A_TILE = 0x104;
    static final int SWITCH_BLOCK_B_TILE = 0x108;

    private SwitchBlockAnimation() {
    }

    public record TileCopy(int sourceOffset, int destinationTile) {
        public TileCopy {
            if (sourceOffset != 0x00 && sourceOffset != 0x40 && sourceOffset != 0x80) {
                throw new IllegalArgumentException("Unknown switch-block source offset: "
                    + sourceOffset);
            }
            if (destinationTile != SWITCH_BLOCK_A_TILE
                && destinationTile != SWITCH_BLOCK_B_TILE) {
                throw new IllegalArgumentException("Unknown switch-block destination tile: "
                    + destinationTile);
            }
        }
    }

    public record Step(int nextStage, int switchBlocksState, TileCopy tileCopy) {
        public Step {
            if (nextStage < 0 || nextStage > 0x09) {
                throw new IllegalArgumentException("Switch-block stage out of range: " + nextStage);
            }
            validateState(switchBlocksState);
        }
    }

    public static boolean isAnimating(int stage) {
        return stage >= 0x01 && stage <= 0x09;
    }

    public static Step advance(int stage, int switchBlocksState) {
        if (!isAnimating(stage)) {
            throw new IllegalArgumentException("Cannot advance idle/invalid switch-block stage: "
                + stage);
        }
        validateState(switchBlocksState);

        int nextStage = stage + 1;
        int nextState = stage == 0x02
            ? switchBlocksState ^ 0x02 : switchBlocksState;
        if (nextStage == 0x0A) {
            nextStage = 0;
        }

        TileCopy tileCopy = switch (stage) {
            case 0x03 -> new TileCopy(0x40, SWITCH_BLOCK_A_TILE);
            case 0x05 -> new TileCopy(0x40, SWITCH_BLOCK_B_TILE);
            case 0x07 -> new TileCopy(finalSourceOffset(nextState, true),
                SWITCH_BLOCK_A_TILE);
            case 0x09 -> new TileCopy(finalSourceOffset(nextState, false),
                SWITCH_BLOCK_B_TILE);
            default -> null;
        };
        return new Step(nextStage, nextState, tileCopy);
    }

    public static List<TileCopy> initialCopies(int switchBlocksState) {
        validateState(switchBlocksState);
        return List.of(
            new TileCopy(finalSourceOffset(switchBlocksState, true), SWITCH_BLOCK_A_TILE),
            new TileCopy(finalSourceOffset(switchBlocksState, false), SWITCH_BLOCK_B_TILE));
    }

    private static int finalSourceOffset(int switchBlocksState, boolean blockA) {
        if (blockA) {
            return switchBlocksState == LOWERED_STATE ? 0x00 : 0x80;
        }
        return switchBlocksState == LOWERED_STATE ? 0x80 : 0x00;
    }

    private static void validateState(int switchBlocksState) {
        if (switchBlocksState != LOWERED_STATE && switchBlocksState != RAISED_STATE) {
            throw new IllegalArgumentException("Switch-block state must be $00 or $02: "
                + switchBlocksState);
        }
    }
}
