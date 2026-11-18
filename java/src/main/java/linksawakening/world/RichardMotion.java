package linksawakening.world;

/** Richard's offer, leaf hand-in and passage beneath the villa (bank 06). */
final class RichardMotion {
    private enum State { INITIAL, TALK, ANSWER, STEP_ASIDE, PASSAGE }
    private State state = State.INITIAL;
    private int inertia;
    private int direction;
    private int steps;
    private boolean movingAside;
    private int dialog = -1;
    private boolean warp;

    RoomEntity advance(RoomEntity entity, int frame, int linkX, int linkY, int leaves, int choice) {
        int x = entity.x();
        if ((frame & 31) == 0) direction = TarinRaccoonMotion.directionToLink(entity, linkX, linkY);
        int variant = (3 - direction) * 2 + ((++inertia >>> 4) & 1);
        switch (state) {
            case INITIAL -> {
                state = leaves >= 6 ? State.PASSAGE : State.TALK;
                if (state == State.PASSAGE) {
                    x = 0x58;
                    direction = 3;
                }
            }
            case ANSWER -> {
                dialog = choice == 0 ? 0x3B : 0x3C;
                state = State.TALK;
            }
            case STEP_ASIDE -> {
                // Speed $F8 advances half a pixel left per tick, starting with a borrow.
                if ((steps++ & 1) == 0) x--;
                if (steps == 0x20) state = State.PASSAGE;
            }
            case PASSAGE -> {
                warp = ((linkX - 0x78 + 2) & 255) < 4 && ((linkY - 0x20 + 5) & 255) < 10;
            }
        }
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, entity.y(),
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(), entity.spriteTileOffset());
    }

    void startConversation(int bowWow, int leaves, int roomStatus) {
        if (state == State.PASSAGE) {
            dialog = leaves == 6 ? 0x3E : 0x3D;
        } else if (state == State.TALK) {
            if (bowWow != 0) dialog = 0x2D;
            else if ((roomStatus & 0x10) == 0 || leaves == 0) {
                dialog = 0x3A;
                state = State.ANSWER;
            } else if (leaves < 5) dialog = 0x3F;
            else {
                dialog = 0x3D;
                movingAside = true;
                state = State.STEP_ASIDE;
            }
        }
    }

    boolean pushesLink() { return !movingAside; }
    boolean handingInLeaves() { return state == State.STEP_ASIDE && steps == 0; }
    boolean warpRequested() { return warp; }
    int takeDialog() {
        int result = dialog;
        dialog = -1;
        return result;
    }
}
