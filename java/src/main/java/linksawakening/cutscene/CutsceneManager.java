package linksawakening.cutscene;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import linksawakening.dialog.DialogController;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;

/** Owns the active IntroHandler presentation and its sound sink. */
public final class CutsceneManager implements CutsceneContext {

    private final DialogController dialogController;
    private final Consumer<String> sceneLoader;
    private final CutsceneController controller;
    private final IntroPlayback playback;
    private IntConsumer noiseSound = id -> {};

    public void setIntroNoiseSound(IntConsumer sink) {
        noiseSound = Objects.requireNonNull(sink);
        playback.setNoiseSound(noiseSound);
    }

    private String currentScene = "";

    public CutsceneManager(DialogController dialogController, Consumer<String> sceneLoader) {
        this.dialogController = Objects.requireNonNull(dialogController);
        this.sceneLoader = Objects.requireNonNull(sceneLoader);
        this.controller = new CutsceneController(this);
        playback = new IntroPlayback(this::setScene);
    }

    public void startIntro() {
        IntroSequence introSequence = new IntroSequence();
        playback.start(introSequence, noiseSound);
        controller.start(CutsceneScript.of());
        setScene(introSequence.sceneId());
    }

    public void startIntro(Rom romData, Function<String, BackgroundScene> backgroundProvider) {
        IntroSequence introSequence = new IntroSequence(romData, backgroundProvider);
        playback.start(introSequence, noiseSound);
        controller.start(CutsceneScript.of());
        setScene(introSequence.sceneId());
    }

    public void tick() {
        if (playback.tick()) return;
        if (controller.isActive()) {
            controller.tick();
        }
    }

    public boolean isActive() {
        return playback.active() || controller.isActive();
    }

    public boolean skipIntroToTitle() {
        return playback.skip();
    }

    public boolean isShowingTitleScene() {
        return IntroCutsceneScript.SCENE_TITLE.equals(currentScene);
    }

    public int scrollX() {
        return playback.frame().scrollX();
    }

    public int scrollY() {
        return playback.frame().scrollY();
    }

    public int[] lineScrollX(int height) {
        return resizeLineScroll(playback.frame().lineScrollX(), height);
    }

    public List<IntroSprite> sprites() {
        return playback.frame().sprites();
    }

    public int titleRevealRows() {
        return playback.frame().titleRevealRows();
    }

    public IntroFrameSnapshot frameSnapshot() {
        return playback.snapshotOrNull();
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
