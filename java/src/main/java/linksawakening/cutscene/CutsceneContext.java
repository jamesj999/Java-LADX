package linksawakening.cutscene;
/** Contract for cutscene context. */
public interface CutsceneContext {

    void setScene(String sceneId);

    void showDialog(String text);

    boolean isDialogActive();
}
