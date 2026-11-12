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
        int wreckingBallY) {
    /** @deprecated Use {@link Builder} for named construction. */
    @Deprecated(forRemoval = true)
    public NewGameStartProfile {}

    /** Values emitted by the shipped disassembly's .initNewGame path. */
    public static NewGameStartProfile romDefaults() {
        return new Builder()
                .mapId(0x10) // MAP_HOUSE
                .roomId(0xA3) // ROOM_INDOOR_B_MARIN_HOUSE
                .entryX(0x50)
                .entryY(0x60)
                .maxArrows(0x30)
                .maxBombs(0x30)
                .maxMagicPowder(0x20)
                .romDirection(0x03) // ROM direction down
                .animationState(0x00)
                .wreckingBallRoom(0x16)
                .wreckingBallX(0x50)
                .wreckingBallY(0x27)
                .build();
    }

    /** Applies the fresh-file portion represented by the current player model. */
    public void initializePlayerState(PlayerState playerState) {
        Objects.requireNonNull(playerState, "playerState");
        playerState
                .playerInitialization()
                .initializeNewGame(fromBcd(maxArrows), fromBcd(maxBombs), fromBcd(maxMagicPowder));
    }

    private static int fromBcd(int value) {
        return (value >>> 4) * 10 + (value & 0x0F);
    }

    /** Reusable named construction; build preserves the record's defensive copies. */
    public static final class Builder {
        private int mapId;
        private int roomId;
        private int entryX;
        private int entryY;
        private int maxArrows;
        private int maxBombs;
        private int maxMagicPowder;
        private int romDirection;
        private int animationState;
        private int wreckingBallRoom;
        private int wreckingBallX;
        private int wreckingBallY;

        public Builder mapId(int value) {
            mapId = value;
            return this;
        }

        public Builder roomId(int value) {
            roomId = value;
            return this;
        }

        public Builder entryX(int value) {
            entryX = value;
            return this;
        }

        public Builder entryY(int value) {
            entryY = value;
            return this;
        }

        public Builder maxArrows(int value) {
            maxArrows = value;
            return this;
        }

        public Builder maxBombs(int value) {
            maxBombs = value;
            return this;
        }

        public Builder maxMagicPowder(int value) {
            maxMagicPowder = value;
            return this;
        }

        public Builder romDirection(int value) {
            romDirection = value;
            return this;
        }

        public Builder animationState(int value) {
            animationState = value;
            return this;
        }

        public Builder wreckingBallRoom(int value) {
            wreckingBallRoom = value;
            return this;
        }

        public Builder wreckingBallX(int value) {
            wreckingBallX = value;
            return this;
        }

        public Builder wreckingBallY(int value) {
            wreckingBallY = value;
            return this;
        }

        public NewGameStartProfile build() {
            return new NewGameStartProfile(
                    mapId,
                    roomId,
                    entryX,
                    entryY,
                    maxArrows,
                    maxBombs,
                    maxMagicPowder,
                    romDirection,
                    animationState,
                    wreckingBallRoom,
                    wreckingBallX,
                    wreckingBallY);
        }
    }
}
