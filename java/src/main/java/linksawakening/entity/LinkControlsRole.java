package linksawakening.entity;

/** Controls operations from LinkMotionDefault and its related ROM routines. */
public interface LinkControlsRole {
    boolean buttonWasPressed(int key);

    boolean playerHasFlippers();

    int buildJoypadMask();

    int romPressedButtonsMask();
}
