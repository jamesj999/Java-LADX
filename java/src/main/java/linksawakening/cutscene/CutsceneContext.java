package linksawakening.cutscene;

public interface CutsceneContext {

    void setScene(String sceneId);

    void showDialog(String text);

    boolean isDialogActive();
}
