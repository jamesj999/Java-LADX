package linksawakening.cutscene;

import linksawakening.dialog.DialogController;

import java.util.Objects;
import java.util.function.Consumer;

public final class CutsceneManager implements CutsceneContext {

    private final DialogController dialogController;
    private final Consumer<String> sceneLoader;
    private final CutsceneController controller;
    private IntroSequence introSequence;
    private String currentScene = "";

    public CutsceneManager(DialogController dialogController, Consumer<String> sceneLoader) {
        this.dialogController = Objects.requireNonNull(dialogController);
        this.sceneLoader = Objects.requireNonNull(sceneLoader);
        this.controller = new CutsceneController(this);
    }

    public void startIntro() {
        introSequence = new IntroSequence();
        controller.start(CutsceneScript.of());
        setScene(introSequence.sceneId());
    }

    public void tick() {
        if (introSequence != null && introSequence.isActive()) {
            String previousScene = introSequence.sceneId();
            introSequence.tick();
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

    public boolean isShowingTitleScene() {
        return IntroCutsceneScript.SCENE_TITLE.equals(currentScene);
    }

    public int scrollX() {
        return introSequence != null ? introSequence.scrollX() : 0;
    }

    public int scrollY() {
        return introSequence != null ? introSequence.scrollY() : 0;
    }

    public int[] lineScrollX(int height) {
        return introSequence != null ? introSequence.lineScrollX(height) : null;
    }

    public java.util.List<IntroSprite> sprites() {
        return introSequence != null ? introSequence.sprites() : java.util.List.of();
    }

    public int titleRevealRows() {
        return introSequence != null ? introSequence.titleRevealRows() : 7;
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
}
