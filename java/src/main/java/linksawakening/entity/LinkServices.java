package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import java.util.Objects;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.LinkPlayerState;

/** Construction-time services shared by Link components; nullable test collaborators stay explicit. */
final class LinkServices {
    final InputState inputState;
    final InputConfig inputConfig;
    final RomTables romTables;
    final OverworldCollision collision;
    final LinkSpriteSheet spriteSheet;
    final LinkPlayerState playerState;
    final ItemRegistry itemRegistry;
    final LinkTunicPalette tunicPalette;
    final GameplaySoundSink soundSink;

    LinkServices(
            InputState inputState,
            InputConfig inputConfig,
            RomTables romTables,
            OverworldCollision collision,
            LinkSpriteSheet spriteSheet,
            Link.Services arguments) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.romTables = romTables;
        this.collision = collision;
        this.spriteSheet = spriteSheet;
        this.playerState = arguments.playerState();
        this.itemRegistry = arguments.itemRegistry();
        this.soundSink = Objects.requireNonNull(arguments.soundSink(), "soundSink");
        this.tunicPalette = Objects.requireNonNull(arguments.tunicPalette(), "tunicPalette");
    }
}
