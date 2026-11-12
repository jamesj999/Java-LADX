package linksawakening.entity;

import static linksawakening.entity.LinkRomConstants.*;

import java.util.Objects;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
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
public final class Link implements linksawakening.equipment.RocsFeather.JumpTarget {

    public static final int SPRITE_SIZE = 16;
    public static final int SUB_PIXEL_SHIFT = 4;

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
                new linksawakening.entity.Link.Services(
                        playerState, itemRegistry, GameplaySoundSink.none(), LinkTunicPalette.greenCompatibility()));
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
                new linksawakening.entity.Link.Services(
                        playerState, itemRegistry, soundSink, LinkTunicPalette.greenCompatibility()));
    }

    public Link(
            InputState inputState,
            InputConfig inputConfig,
            RomTables romTables,
            OverworldCollision collision,
            LinkSpriteSheet spriteSheet,
            Services arguments) {

        assembly = new LinkAssembly(
                new LinkServices(inputState, inputConfig, romTables, collision, spriteSheet, arguments));
        linkAnimator = assembly.linkAnimator.get();
        linkSideView = assembly.linkSideView.get();
        linkStateMachine = assembly.linkStateMachine.get();
        linkEquipment = assembly.linkEquipment.get();
        linkEntityEffects = assembly.linkEntityEffects.get();
        linkMotion = assembly.linkMotion.get();
        linkAirborne = assembly.linkAirborne.get();
        linkSwimming = assembly.linkSwimming.get();
        linkDamage = assembly.linkDamage.get();
        linkAnimationState = assembly.linkAnimationState.get();
        linkItemState = assembly.linkItemState.get();
        linkGroundMotion = assembly.linkGroundMotion.get();
        linkSignals = assembly.linkSignals.get();
        linkRecovery = assembly.linkRecovery.get();
        linkServices = assembly.linkServices.get();
        linkControls = assembly.linkControls.get();
        dialogGotItemSequence = assembly.dialogGotItemSequence.get();

        roles = new LinkRoles(assembly);
    }
    /** Immutable water splash values. */
    public record WaterSplash(int romX, int romY) {}
    /** Immutable screen shake request values. */
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
    public record GrabFlags(int airborneState, boolean motionBlocked, boolean applyHeldLinkPose) {
        private static final GrabFlags[] BYTE_VALUES = cachedValues();

        /** Reuses immutable byte combinations; invalid bytes retain downstream validation order. */
        public static GrabFlags of(int airborneState, boolean motionBlocked, boolean applyHeldLinkPose) {
            if ((airborneState & ~255) != 0) return new GrabFlags(airborneState, motionBlocked, applyHeldLinkPose);
            return BYTE_VALUES[(airborneState << 2) | (motionBlocked ? 2 : 0) | (applyHeldLinkPose ? 1 : 0)];
        }

        private static GrabFlags[] cachedValues() {
            GrabFlags[] values = new GrabFlags[1024];
            for (int i = 0; i < values.length; i++) values[i] = new GrabFlags(i >>> 2, (i & 2) != 0, (i & 1) != 0);
            return values;
        }
    }

    final LinkAssembly assembly;
    final LinkAnimator linkAnimator;
    final LinkSideView linkSideView;
    final LinkStateMachine linkStateMachine;
    final LinkEquipment linkEquipment;
    final LinkEntityEffects linkEntityEffects;
    final LinkMotion linkMotion;
    final LinkAirborne linkAirborne;
    final LinkSwimming linkSwimming;
    final LinkDamage linkDamage;
    final LinkAnimationState linkAnimationState;
    final LinkItemState linkItemState;
    final LinkGroundMotion linkGroundMotion;
    final LinkSignals linkSignals;
    final LinkRecovery linkRecovery;
    final LinkServices linkServices;
    final LinkControls linkControls;
    final DialogGotItemSequence dialogGotItemSequence;
    private final LinkRoles roles;

    /** Advances one frame through the retained motion orchestration. */
    public void update() {
        motion().update();
    }

    // External compatibility: world/room/GameplayRouteStage1 still passes Link as JumpTarget.
    /** @deprecated Use {@code airborne().useRocsFeather()} or pass {@code airborne()} as JumpTarget. */
    @Deprecated(forRemoval = true)
    @Override
    public void useRocsFeather() {
        airborne().useRocsFeather();
    }

    public LinkSideViewRole sideView() {
        return roles.sideView();
    }

    public LinkMotionRole motion() {
        return roles.motion();
    }

    public LinkStatusRole status() {
        return roles.status();
    }

    public LinkEquipmentRole equipment() {
        return roles.equipment();
    }

    public LinkAnimationRole animation() {
        return roles.animation();
    }

    public LinkActionsRole actions() {
        return roles.actions();
    }

    public LinkPositionRole position() {
        return roles.position();
    }

    public LinkAirborneRole airborne() {
        return roles.airborne();
    }

    public LinkControlsRole controls() {
        return roles.controls();
    }
}
