package linksawakening.entity;

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

/** Required services for a Link actor; inert actors use explicit empty services. */
public record LinkDependencies(
        InputState inputState,
        InputConfig inputConfig,
        LinkMotionTables linkMotionTables,
        SwordTables swordTables,
        OverworldCollision collision,
        LinkSpriteSheet spriteSheet,
        PlayerState playerState,
        ItemRegistry itemRegistry,
        GameplaySoundSink soundSink,
        LinkTunicPalette tunicPalette) {
    public LinkDependencies {
        Objects.requireNonNull(inputState, "inputState");
        Objects.requireNonNull(inputConfig, "inputConfig");
        Objects.requireNonNull(linkMotionTables, "linkMotionTables");
        Objects.requireNonNull(swordTables, "swordTables");
        Objects.requireNonNull(collision, "collision");
        Objects.requireNonNull(spriteSheet, "spriteSheet");
        Objects.requireNonNull(playerState, "playerState");
        Objects.requireNonNull(itemRegistry, "itemRegistry");
        Objects.requireNonNull(soundSink, "soundSink");
        Objects.requireNonNull(tunicPalette, "tunicPalette");
    }

    /** Adapts legacy nullable constructor arguments at the construction boundary only. */
    static LinkDependencies legacy(
            InputState input,
            InputConfig config,
            RomTables tables,
            OverworldCollision collision,
            LinkSpriteSheet sprites,
            Link.Services services) {
        return new LinkDependencies(
                Objects.requireNonNullElseGet(input, InputState::new),
                Objects.requireNonNullElseGet(config, () -> new InputConfig(-1, -1, -1, -1, -1, -1, -1)),
                tables == null ? LinkMotionTables.stationary() : tables.linkMotionTables(),
                tables == null ? SwordTables.animationOnly() : tables.swordTables(),
                Objects.requireNonNullElseGet(collision, OverworldCollision::openWorld),
                Objects.requireNonNullElseGet(sprites, LinkSpriteSheet::invisible),
                Objects.requireNonNullElseGet(services.playerState(), PlayerState::new),
                Objects.requireNonNullElseGet(services.itemRegistry(), ItemRegistry::new),
                services.soundSink(),
                services.tunicPalette());
    }
}
