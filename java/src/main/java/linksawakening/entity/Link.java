package linksawakening.entity;

import static linksawakening.entity.LinkRomConstants.*;

import java.util.Objects;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.GroundStatus;
import linksawakening.state.LinkMotionState;
import linksawakening.state.PlayerState;

/**
 * Link's state and per-frame update, ported from the LADX disassembly's
 * {@code LinkMotionDefault} path (bank2.asm:161).
 *
 * <p>Position is stored in sub-pixels (1 pixel = 16 sub-pixels), matching the
 * scale of the original {@code HorizontalIncrementForLinkPosition} /
 * {@code VerticalIncrementForLinkPosition} tables.
 *
 * <p>The 16x16 AABB used for both rendering and collision has its origin at
 * Link's top-left.
 */
public final class Link extends LinkRoomView implements RocsFeather.JumpTarget {

    /** @deprecated Use {@link Direction#DOWN}. */
    @Deprecated(forRemoval = true)
    public static final int DIRECTION_DOWN = 0;
    /** @deprecated Use {@link Direction#UP}. */
    @Deprecated(forRemoval = true)
    public static final int DIRECTION_UP = 1;
    /** @deprecated Use {@link Direction#LEFT}. */
    @Deprecated(forRemoval = true)
    public static final int DIRECTION_LEFT = 2;
    /** @deprecated Use {@link Direction#RIGHT}. */
    @Deprecated(forRemoval = true)
    public static final int DIRECTION_RIGHT = 3;

    public static final int SPRITE_SIZE = 16;
    public static final int SUB_PIXEL_SHIFT = 4;
    /** @deprecated Use {@link GroundStatus#NORMAL}. */
    @Deprecated(forRemoval = true)
    public static final int GROUND_STATUS_NORMAL = 0x00;
    /** @deprecated Use {@link GroundStatus#PIT}. */
    @Deprecated(forRemoval = true)
    public static final int GROUND_STATUS_PIT = 0x07;
    /** @deprecated Use {@link LinkMotionState#DEFAULT}. */
    @Deprecated(forRemoval = true)
    public static final int LINK_MOTION_DEFAULT = 0x00;
    /** @deprecated Use {@link LinkMotionState#PASS_OUT}. */
    @Deprecated(forRemoval = true)
    public static final int LINK_MOTION_PASS_OUT = 0x07;
    /** @deprecated Use {@link LinkMotionState#SWIMMING}. */
    @Deprecated(forRemoval = true)
    public static final int LINK_MOTION_SWIMMING = 0x01;
    /** @deprecated Use {@link LinkMotionState#RECOVER}. */
    @Deprecated(forRemoval = true)
    public static final int LINK_MOTION_RECOVER = 0x08;

    public Link(
            InputState inputState,
            InputConfig inputConfig,
            RomTables romTables,
            OverworldCollision collision,
            LinkSpriteSheet spriteSheet,
            PlayerState playerState,
            ItemRegistry itemRegistry) {
        this(
                inputState,
                inputConfig,
                romTables,
                collision,
                spriteSheet,
                playerState,
                itemRegistry,
                GameplaySoundSink.none(),
                LinkTunicPalette.greenCompatibility());
    }

    public Link(
            InputState inputState,
            InputConfig inputConfig,
            RomTables romTables,
            OverworldCollision collision,
            LinkSpriteSheet spriteSheet,
            PlayerState playerState,
            ItemRegistry itemRegistry,
            GameplaySoundSink soundSink) {
        this(
                inputState,
                inputConfig,
                romTables,
                collision,
                spriteSheet,
                playerState,
                itemRegistry,
                soundSink,
                LinkTunicPalette.greenCompatibility());
    }
    /** @deprecated Use the Services argument bundle overload. */
    @Deprecated(forRemoval = true)
    public Link(
            InputState inputState,
            InputConfig inputConfig,
            RomTables romTables,
            OverworldCollision collision,
            LinkSpriteSheet spriteSheet,
            PlayerState playerState,
            ItemRegistry itemRegistry,
            GameplaySoundSink soundSink,
            LinkTunicPalette tunicPalette) {
        this(
                inputState,
                inputConfig,
                romTables,
                collision,
                spriteSheet,
                new Services(playerState, itemRegistry, soundSink, tunicPalette));
    }

    public Link(
            InputState inputState,
            InputConfig inputConfig,
            RomTables romTables,
            OverworldCollision collision,
            LinkSpriteSheet spriteSheet,
            Services arguments) {
        PlayerState playerState = arguments.playerState();
        ItemRegistry itemRegistry = arguments.itemRegistry();
        GameplaySoundSink soundSink = arguments.soundSink();
        LinkTunicPalette tunicPalette = arguments.tunicPalette();

        LinkServices services = new LinkServices();
        services.inputState = inputState;
        services.inputConfig = inputConfig;
        services.romTables = romTables;
        services.collision = collision;
        services.spriteSheet = spriteSheet;
        services.playerState = playerState;
        services.itemRegistry = itemRegistry;
        services.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        services.tunicPalette = Objects.requireNonNull(tunicPalette, "tunicPalette");
        assembly = new LinkAssembly(services);
        bindComponents();
    }

    public record WaterSplash(int romX, int romY) {}

    public record ScreenShakeRequest(int countdown, int phase) {}

    public void apply(LinkCommand command) {
        Objects.requireNonNull(command, "command").applyTo(this);
    }

    static int signedByte(int value) {
        int normalized = value & 0xFF;
        return normalized < 0x80 ? normalized : normalized - 0x100;
    }

    static void validateRomByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    static int romDirectionForJavaDirection(int javaDirection) {
        return Direction.fromJava(javaDirection).romIndex();
    }

    static int javaDirectionForRomDirection(int romDirection) {
        return Direction.fromRom(romDirection).javaIndex();
    }

    /** Arguments shared by Link operations. */
    public record Services(
            PlayerState playerState,
            ItemRegistry itemRegistry,
            GameplaySoundSink soundSink,
            LinkTunicPalette tunicPalette) {}

    /** Held-Link grab flags, preserving the source write order. */
    public record GrabFlags(int airborneState, boolean motionBlocked, boolean applyHeldLinkPose) {}
}
