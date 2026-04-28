package linksawakening.cutscene;

public final class IntroCutsceneScript {

    public static final String SCENE_SEA = "intro-sea";
    public static final String SCENE_LINK_FACE = "intro-link-face";
    public static final String SCENE_BEACH = "intro-beach";
    public static final String SCENE_TITLE = "title";

    public static final int SEA_DURATION_FRAMES = 160;
    public static final int LINK_FACE_DURATION_FRAMES = 160;
    public static final int BEACH_DURATION_FRAMES = 160;
    public static final int TITLE_DURATION_FRAMES = 160;

    private IntroCutsceneScript() {
    }

    public static CutsceneScript create() {
        return CutsceneScript.of(
            CutsceneStep.run(context -> context.setScene(SCENE_SEA)),
            CutsceneStep.waitFrames(SEA_DURATION_FRAMES + 1),
            CutsceneStep.run(context -> context.setScene(SCENE_LINK_FACE)),
            CutsceneStep.waitFrames(LINK_FACE_DURATION_FRAMES),
            CutsceneStep.run(context -> context.setScene(SCENE_BEACH)),
            CutsceneStep.waitFrames(BEACH_DURATION_FRAMES),
            CutsceneStep.run(context -> context.setScene(SCENE_TITLE)),
            CutsceneStep.waitFrames(TITLE_DURATION_FRAMES)
        );
    }
}
