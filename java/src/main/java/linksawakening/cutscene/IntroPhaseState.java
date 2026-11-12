package linksawakening.cutscene;

import static linksawakening.cutscene.IntroPaletteMath.*;
import static linksawakening.cutscene.IntroSequence.*;

/** Explicit intro stage and presentation counters shared with delayed frame publication. */
final class IntroPhaseState {

    private Stage stage = Stage.SEA;
    private int frameCounter;

    private int stageFrame;
    private boolean skipped;
    private int linkFaceTimer;
    private int titleRevealRows;
    private int titleHoldFrame;
    private int titleEffectFrame;
    private int topLineScroll;

    String sceneId() {
        return switch (stage) {
            case SEA, SEA_FADE -> IntroCutsceneScript.SCENE_SEA;
            case LINK_FACE -> IntroCutsceneScript.SCENE_LINK_FACE;
            case BEACH_LOAD -> stageFrame == 0 ? IntroCutsceneScript.SCENE_SEA : IntroCutsceneScript.SCENE_BEACH;
            case BEACH_FADE, BEACH -> IntroCutsceneScript.SCENE_BEACH;
            case TITLE_REVEAL, TITLE_WAIT, TITLE_DX, COMPLETE -> IntroCutsceneScript.SCENE_TITLE;
        };
    }

    Stage stage() {
        return stage;
    }

    Stage stage(Stage value) {
        stage = value;
        return value;
    }

    int frameCounter() {
        return frameCounter;
    }

    int frameCounter(int value) {
        frameCounter = value;
        return value;
    }

    int stageFrame() {
        return stageFrame;
    }

    int stageFrame(int value) {
        stageFrame = value;
        return value;
    }

    boolean skipped() {
        return skipped;
    }

    boolean skipped(boolean value) {
        skipped = value;
        return value;
    }

    int linkFaceTimer() {
        return linkFaceTimer;
    }

    int linkFaceTimer(int value) {
        linkFaceTimer = value;
        return value;
    }

    int titleRevealRows() {
        return titleRevealRows;
    }

    int titleRevealRows(int value) {
        titleRevealRows = value;
        return value;
    }

    int titleHoldFrame() {
        return titleHoldFrame;
    }

    int titleHoldFrame(int value) {
        titleHoldFrame = value;
        return value;
    }

    int titleEffectFrame() {
        return titleEffectFrame;
    }

    int titleEffectFrame(int value) {
        titleEffectFrame = value;
        return value;
    }

    int topLineScroll() {
        return topLineScroll;
    }

    int topLineScroll(int value) {
        topLineScroll = value;
        return value;
    }
}
