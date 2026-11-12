package linksawakening.gameplay;

import linksawakening.entity.Link;

/** Link dialog position behavior and state. */
public final class LinkDialogPosition {

    private LinkDialogPosition() {}

    public static int dialogYFromTopLeft(int linkTopLeftY) {
        return linkTopLeftY + Link.SPRITE_SIZE;
    }
}
