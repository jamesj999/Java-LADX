package linksawakening.startup;

import java.util.Objects;

import linksawakening.state.PlayerState;

/**
 * Source-derived values written by {@code LoadSavedFile.initNewGame}
 * (bank1.asm:$5394) when a newly created file has no saved spawn position.
 */
public record NewGameStartProfile(
    int mapId,
    int roomId,
    int entryX,
    int entryY,
    int maxArrows,
    int maxBombs,
    int maxMagicPowder,
    int romDirection,
    int animationState,
    int wreckingBallRoom,
    int wreckingBallX,
    int wreckingBallY
) {

    /** Values emitted by the shipped disassembly's .initNewGame path. */
    public static NewGameStartProfile romDefaults() {
        return new NewGameStartProfile(
            0x10,  // MAP_HOUSE
            0xA3,  // ROOM_INDOOR_B_MARIN_HOUSE
            0x50,
            0x60,
            0x30,  // wMaxArrows
            0x30,  // wMaxBombs
            0x20,  // wMaxMagicPowder
            0x03,  // DIRECTION_DOWN in the ROM encoding
            0x00,  // standing animation state
            0x16,  // wWreckingBallRoom
            0x50,  // wWreckingBallPosX
            0x27   // wWreckingBallPosY
        );
    }

    /** Applies the fresh-file portion represented by the current player model. */
    public void initializePlayerState(PlayerState playerState) {
        Objects.requireNonNull(playerState, "playerState");
        playerState.initializeNewGame(maxArrows, maxBombs, maxMagicPowder);
    }
}
