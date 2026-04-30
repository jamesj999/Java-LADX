package linksawakening.gameplay;

import linksawakening.entity.Link;

public final class LinkDialogPosition {

    private LinkDialogPosition() {
    }

    public static int dialogYFromTopLeft(int linkTopLeftY) {
        return linkTopLeftY + Link.SPRITE_SIZE;
    }
}
