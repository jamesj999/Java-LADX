package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Advances scrolling before warp transitions and clears platform state at the gate. One instance is retained
 * for the owning session.
 */
public final class TransitionPhase implements GameplayFrame.Transitions {
    private final ScrollController scroll;
    private final TransitionController transition;
    private final Link link;
    private final int scrollSpeed;
    private final MusicDirector music;
    private final GPU gpu;

    public TransitionPhase(
            ScrollController scroll,
            TransitionController transition,
            Link link,
            int scrollSpeed,
            MusicDirector music,
            GPU gpu) {
        this.gpu = Objects.requireNonNull(gpu);
        this.music = Objects.requireNonNull(music);
        this.scroll = Objects.requireNonNull(scroll);
        this.transition = Objects.requireNonNull(transition);
        this.link = Objects.requireNonNull(link);
        this.scrollSpeed = scrollSpeed;
    }

    @Override
    public void run() {
        // ApplyRoomTransition writes $FF on the completion frame, before VBlank ($02:7989).
        if (scroll.tick(scrollSpeed)) gpu.resetFrameCountForRoomEntry();
        transition.tick();
        music.updateWarpSound(transition);
        if (scroll.isActive() || transition.isInputBlocked()) {
            link.sideView().clearSideViewPlatformState();
        }
    }

    @Override
    public boolean tileTransferOnlyFrame() {
        return scroll.tileTransferOnlyFrame();
    }
}
