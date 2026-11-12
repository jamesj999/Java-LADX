package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import java.util.Objects;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.LinkMotionTables;
import linksawakening.rom.RomTables;
import linksawakening.rom.SwordTables;
import linksawakening.state.PlayerState;

/** Construction-time services shared by Link components; nullable test collaborators stay explicit. */
final class LinkServices {
    final InputState inputState;
    final InputConfig inputConfig;
    final LinkMotionTables linkMotionTables;

    final SwordTables swordTables;
    final OverworldCollision collision;
    final LinkSpriteSheet spriteSheet;
    final PlayerState playerState;
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
        this.linkMotionTables = romTables == null ? null : romTables.linkMotionTables();
        this.swordTables = romTables == null ? null : romTables.swordTables();
        this.collision = collision;
        this.spriteSheet = spriteSheet;
        this.playerState = arguments.playerState();
        this.itemRegistry = arguments.itemRegistry();
        this.soundSink = Objects.requireNonNull(arguments.soundSink(), "soundSink");
        this.tunicPalette = Objects.requireNonNull(arguments.tunicPalette(), "tunicPalette");
    }
}
