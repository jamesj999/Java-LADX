package linksawakening.entity;

import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;

/** Named test setup for Link, including deliberately absent collaborators. */
final class LinkTestBuilder {
    private InputState input;
    private InputConfig config;
    private RomTables tables;
    private OverworldCollision collision;
    private LinkSpriteSheet sprites;
    private PlayerState player;
    private ItemRegistry items;
    private GameplaySoundSink sounds = GameplaySoundSink.none();
    private LinkTunicPalette palette = LinkTunicPalette.greenCompatibility();

    LinkTestBuilder input(InputState value) {
        input = value;
        return this;
    }

    LinkTestBuilder config(InputConfig value) {
        config = value;
        return this;
    }

    LinkTestBuilder tables(RomTables value) {
        tables = value;
        return this;
    }

    LinkTestBuilder collision(OverworldCollision value) {
        collision = value;
        return this;
    }

    LinkTestBuilder sprites(LinkSpriteSheet value) {
        sprites = value;
        return this;
    }

    LinkTestBuilder player(PlayerState value) {
        player = value;
        return this;
    }

    LinkTestBuilder items(ItemRegistry value) {
        items = value;
        return this;
    }

    LinkTestBuilder sounds(GameplaySoundSink value) {
        sounds = value;
        return this;
    }

    LinkTestBuilder palette(LinkTunicPalette value) {
        palette = value;
        return this;
    }

    Link build() {
        return new Link(input, config, tables, collision, sprites, new Link.Services(player, items, sounds, palette));
    }
}
