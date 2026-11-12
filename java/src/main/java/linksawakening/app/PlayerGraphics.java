package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.entity.LinkTunicPalette;
import linksawakening.equipment.SwordPalette;
import linksawakening.equipment.SwordSpriteSheet;

/** ROM-backed player and equipment graphics loaded during wiring. */
public record PlayerGraphics(
        LinkSpriteSheet linkSpriteSheet,
        SwordSpriteSheet swordSpriteSheet,
        SwordPalette swordPalette,
        LinkTunicPalette linkTunicPalette) {
    public PlayerGraphics {
        Objects.requireNonNull(linkSpriteSheet);
        Objects.requireNonNull(swordSpriteSheet);
        Objects.requireNonNull(swordPalette);
        Objects.requireNonNull(linkTunicPalette);
    }
}
