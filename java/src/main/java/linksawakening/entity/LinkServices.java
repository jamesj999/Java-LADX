package linksawakening.entity;

import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.LinkMotionTables;
import linksawakening.rom.SwordTables;
import linksawakening.state.PlayerState;

/** Validated construction-time services shared by Link components. */
final class LinkServices {
    final InputState inputState;
    final InputConfig inputConfig;
    final LinkMotionTables linkMotionTables;
    final SwordTables swordTables;
    final OverworldCollision collision;
    final LinkSpriteSheet spriteSheet;
    final PlayerState playerState;
    final ItemRegistry itemRegistry;
    final GameplaySoundSink soundSink;
    final LinkTunicPalette tunicPalette;

    LinkServices(LinkDependencies dependencies) {
        this.inputState = dependencies.inputState();
        this.inputConfig = dependencies.inputConfig();
        this.linkMotionTables = dependencies.linkMotionTables();
        this.swordTables = dependencies.swordTables();
        this.collision = dependencies.collision();
        this.spriteSheet = dependencies.spriteSheet();
        this.playerState = dependencies.playerState();
        this.itemRegistry = dependencies.itemRegistry();
        this.soundSink = dependencies.soundSink();
        this.tunicPalette = dependencies.tunicPalette();
    }
}
