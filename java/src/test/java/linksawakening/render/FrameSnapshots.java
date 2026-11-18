package linksawakening.render;
/** Captures room-plane ownership for test/capture inputs, then fills the production pipeline. */
final class FrameSnapshots {
    private FrameSnapshots() {}

    static GameFrameState copy(GameFrameState source) {
        GameFrameState copy = new GameFrameState().copyFrom(source);
        if (source.roomInputs.tiles != null)
            copy.withRoom(source.roomInputs.snapshot(), source.scrollController(), source.transitionController());
        return copy;
    }
}
