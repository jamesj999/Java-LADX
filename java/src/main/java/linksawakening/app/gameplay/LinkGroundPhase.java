package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.world.room.LinkGroundInputs;
import linksawakening.world.room.RoomSession;

/**
 * Ground sampling at the pre- and post-motion ROM positions. Stateless utility; no session state is
 * retained.
 */
public final class LinkGroundPhase {
    private final FrameInputCache frameInputs;
    private final Link playerLink;
    private final RoomSession rooms;

    public LinkGroundPhase(FrameInputCache frameInputs, Link link, RoomSession roomSession) {
        this.frameInputs = Objects.requireNonNull(frameInputs);
        this.playerLink = Objects.requireNonNull(link);
        this.rooms = Objects.requireNonNull(roomSession);
    }

    public void tickCrackedFloor(boolean interactionBlocked) {
        LinkGroundPhase.this
                .rooms
                .terrain()
                .tickCrackedFloor(
                        groundFrame(
                                this.playerLink.position().pixelX(),
                                this.playerLink.position().pixelY(),
                                interactionBlocked),
                        dialogGotItemActive());
    }

    /** wDialogGotItem != 0 ($02:7854). Ported writers: Link's ApplyGotItem
     * sequence (UseMagicPowder $00:149E) and the witch presentation
     * ($05:4834, $05:48DB). */
    public boolean dialogGotItemActive() {
        return this.playerLink.animation().dialogGotItemActive()
                || this.rooms.entityState().gotItemPresentationActive();
    }

    public LinkGroundInputs groundFrame(int pixelX, int pixelY, boolean interactionBlocked) {
        return this.frameInputs.ground(pixelX, pixelY, interactionBlocked);
    }
}
