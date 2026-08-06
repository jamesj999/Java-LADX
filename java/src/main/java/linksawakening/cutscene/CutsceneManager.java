package linksawakening.cutscene;

import linksawakening.dialog.DialogController;
import linksawakening.scene.BackgroundScene;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CutsceneManager implements CutsceneContext {

    private final DialogController dialogController;
    private final Consumer<String> sceneLoader;
    private final CutsceneController controller;
    private IntroSequence introSequence;
    private IntroFrameSnapshot frameSnapshot;
    private String currentScene = "";

    public CutsceneManager(DialogController dialogController, Consumer<String> sceneLoader) {
        this.dialogController = Objects.requireNonNull(dialogController);
        this.sceneLoader = Objects.requireNonNull(sceneLoader);
        this.controller = new CutsceneController(this);
    }

    public void startIntro() {
        introSequence = new IntroSequence();
        frameSnapshot = introSequence.snapshot();
        controller.start(CutsceneScript.of());
        setScene(introSequence.sceneId());
    }

    public void startIntro(byte[] romData, Function<String, BackgroundScene> backgroundProvider) {
        introSequence = new IntroSequence(romData, backgroundProvider);
        frameSnapshot = introSequence.snapshot();
        controller.start(CutsceneScript.of());
        setScene(introSequence.sceneId());
    }

    public void tick() {
        if (introSequence != null && introSequence.isActive()) {
            String previousScene = introSequence.sceneId();
            introSequence.tick();
            frameSnapshot = introSequence.snapshot();
            String nextScene = introSequence.sceneId();
            if (!nextScene.equals(previousScene)) {
                setScene(nextScene);
            }
            return;
        }
        if (controller.isActive()) {
            controller.tick();
        }
    }

    public boolean isActive() {
        return (introSequence != null && introSequence.isActive()) || controller.isActive();
    }

    public boolean skipIntroToTitle() {
        if (introSequence == null || !introSequence.isActive()) {
            return false;
        }
        introSequence.skipToTitle();
        frameSnapshot = introSequence.snapshot();
        setScene(introSequence.sceneId());
        return true;
    }

    public boolean isShowingTitleScene() {
        return IntroCutsceneScript.SCENE_TITLE.equals(currentScene);
    }

    public int scrollX() {
        return frameSnapshot != null ? frameSnapshot.scrollX() : 0;
    }

    public int scrollY() {
        return frameSnapshot != null ? frameSnapshot.scrollY() : 0;
    }

    public int[] lineScrollX(int height) {
        return frameSnapshot != null ? resizeLineScroll(frameSnapshot.lineScrollX(), height) : null;
    }

    public java.util.List<IntroSprite> sprites() {
        return frameSnapshot != null ? frameSnapshot.sprites() : java.util.List.of();
    }

    public int titleRevealRows() {
        return frameSnapshot != null ? frameSnapshot.titleRevealRows() : 7;
    }

    public IntroFrameSnapshot frameSnapshot() {
        return frameSnapshot;
    }

    @Override
    public void setScene(String sceneId) {
        currentScene = sceneId;
        sceneLoader.accept(sceneId);
    }

    @Override
    public void showDialog(String text) {
        dialogController.open(text);
    }

    @Override
    public boolean isDialogActive() {
        return dialogController.isActive();
    }

    private static int[] resizeLineScroll(int[] lineScroll, int height) {
        if (lineScroll == null || lineScroll.length == 0) {
            return null;
        }
        int[] resized = new int[height];
        for (int index = 0; index < height; index++) {
            resized[index] = lineScroll[Math.min(index, lineScroll.length - 1)];
        }
        return resized;
    }
}
