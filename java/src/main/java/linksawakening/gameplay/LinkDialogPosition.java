package linksawakening.gameplay;

import linksawakening.entity.Link;

/** Stateless conversion from Link’s top-left position to the ROM dialog placement coordinate. */
public final class LinkDialogPosition {

    private LinkDialogPosition() {}

    public static int dialogYFromTopLeft(int linkTopLeftY) {
        return linkTopLeftY + Link.SPRITE_SIZE;
    }
}
