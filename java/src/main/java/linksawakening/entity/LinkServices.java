package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.LinkPlayerState;

/** LinkServices owns its Link state and operations. */
final class LinkServices {
    InputState inputState;
    InputConfig inputConfig;
    RomTables romTables;
    OverworldCollision collision;
    LinkSpriteSheet spriteSheet;
    LinkPlayerState playerState;
    ItemRegistry itemRegistry;
    LinkTunicPalette tunicPalette;
    GameplaySoundSink soundSink;
}
