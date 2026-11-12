package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.object.BombObjectInteraction;
import linksawakening.world.object.OverworldBushInteraction;

/** RoomObjectMutations operations for the live room session. */
final class RoomObjectMutations {
    private final Dependencies dependencies;
    private final RoomGroundMutations roomGroundMutations;
    private final RoomBombObjectInteractions roomBombObjectInteractions;
    private final RoomBombWallMutations roomBombWallMutations;
    private final RoomProjectileObjectInteractions roomProjectileObjectInteractions;
    private final RoomTorchMutations roomTorchMutations;

    RoomObjectMutations(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomGroundMutations = new RoomGroundMutations(new RoomGroundMutations.Dependencies(
                dependencies.entityBridge(),
                dependencies.entityFrame(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.roomPlayerItemActions(),
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw(),
                dependencies.terrain(),
                () -> this));
        roomBombObjectInteractions = new RoomBombObjectInteractions(new RoomBombObjectInteractions.Dependencies(
                dependencies.entityBridge(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomState()));
        roomBombWallMutations = new RoomBombWallMutations(new RoomBombWallMutations.Dependencies(
                dependencies.entityBridge(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                dependencies.romTables(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomTileRedraw(),
                dependencies.terrain()));
        roomProjectileObjectInteractions =
                new RoomProjectileObjectInteractions(new RoomProjectileObjectInteractions.Dependencies(
                        dependencies.entityBridge(),
                        dependencies.location(),
                        dependencies.overworldBushInteraction(),
                        dependencies.overworldCollision(),
                        dependencies.roomObjectMutations(),
                        dependencies.roomSessionViews(),
                        dependencies.roomTileRedraw(),
                        dependencies.transientVfxSystem()));
        roomTorchMutations = new RoomTorchMutations(new RoomTorchMutations.Dependencies(
                dependencies.entityFrame(),
                dependencies.indoorTorchPaletteEffect(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw(),
                dependencies.terrain()));
    }

    /**
     * ApplyLinkMotionState ($00:1794) calls func_002_753A after XY movement.
     * $02:7587-7589 copies the last GetObjectUnderLink result, not this
     * frame's pre-movement coordinates. Entity pushes, airborne frames and
     * skipped probes can make that older than one frame.
     */
    boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return roomGroundMutations.tickCrackedFloor(frame, dialogGotItemActive);
    }

    /** ApplyLinkGroundMotion's landing call ($02:4588), before XY and the $7589 copy. */
    boolean applyCrackedFloorGroundPhysics(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return roomGroundMutations.applyCrackedFloorGroundPhysics(frame, dialogGotItemActive);
    }

    /** GetObjectUnderLink $02:7512-7536, translated from ROM center/bottom to Java top-left. */
    void sampleLinkGround(int pixelX, int pixelY) {
        roomGroundMutations.sampleLinkGround(pixelX, pixelY);
    }

    static int linkGroundLocation(int pixelX, int pixelY) {
        // hLinkPositionX = Java X+8; hLinkPositionY = Java Y+16, then subtract 4.
        // AND $F0 after byte arithmetic also preserves the ROM's wrap behavior.
        return ((pixelY + 12) & 0xF0) | (((pixelX + 8) & 0xF0) >>> 4);
    }

    /** Only the control-flow decision; movement, damage and water effects stay with Link. */
    static boolean reachesDefaultGroundPhysics(
            int physics, boolean bypassEntry, int slowWalkingSpeed, int itemUsageContext) {
        if (!bypassEntry) {
            if (physics == 0 || physics == 0xFF) return true; // $75EE/$763A
            if (physics == 0xE0 || physics >= 0xF0 || physics == 0x50 || physics == 0x51) return false;
        }
        // label_002_76C0 is also the entry used by dialogs/transitions ($75C4).
        if (physics == 0x08 || physics == 0x09) return true; // $76D2/$76E1
        if (physics == 0x07 || physics == 0x0B) {
            if (slowWalkingSpeed != 0) return false; // $76F1 -> current handler $7C50
            if (itemUsageContext == 0x80) return true; // raft: $76F9 -> $7757
            return false; // swimming/recovery and entry into water return before Default
        }
        return physics != 0x06; // grass $7752 returns; shallow water falls through $779F
    }

    /** CheckStaticSwordCollision $00:161A-1679; RevealObjectUnderObject $14:5584-55E3. */
    boolean breakIndoorSwordCrystal(int location) {
        return roomGroundMutations.breakIndoorSwordCrystal(location);
    }

    void applyBombObjectInteractions(List<BombExplosionEvent> events) {
        roomBombObjectInteractions.applyBombObjectInteractions(events);
    }

    void applyBasicBombObjectCandidate(BombObjectInteraction.Candidate candidate) {
        roomBombObjectInteractions.applyBasicBombObjectCandidate(candidate);
    }

    void applyBoomerangObjectInteractions(List<BoomerangObjectRequest> requests) {
        roomProjectileObjectInteractions.applyBoomerangObjectInteractions(requests);
    }

    void applyMagicRodObjectInteractions(List<MagicRodObjectRequest> requests) {
        roomProjectileObjectInteractions.applyMagicRodObjectInteractions(requests);
    }

    void applyMagicPowderObjectInteractions(List<MagicPowderObjectRequest> requests) {
        roomTorchMutations.applyMagicPowderObjectInteractions(requests);
    }

    boolean writeMagicPowderTorchObject(int location, int expectedObject, int replacementObject, boolean redraw) {
        return roomTorchMutations.writeMagicPowderTorchObject(location, expectedObject, replacementObject, redraw);
    }

    void retainTorchTiles(int areaIndex) {
        roomTorchMutations.retainTorchTiles(areaIndex);
    }

    void applyRetainedTorchTiles() {
        roomTorchMutations.applyRetainedTorchTiles();
    }

    int countUnlitTorches() {
        return roomTorchMutations.countUnlitTorches();
    }

    void copyIndoorTorchPalettesToActiveRoom() {
        roomTorchMutations.copyIndoorTorchPalettesToActiveRoom();
    }

    void applyPuzzleBombObjectCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot, int sourceZ) {
        roomBombObjectInteractions.applyPuzzleBombObjectCandidate(candidate, sourceSlot, sourceZ);
    }

    void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        roomBombWallMutations.applyOutdoorBombableCaveDoorCandidate(candidate, sourceSlot);
    }

    void writeBombPuzzleObject(int location, int objectId) {
        roomBombWallMutations.writeBombPuzzleObject(location, objectId);
    }

    void refreshOverworldCollisionAfterObjectMutation() {
        roomBombWallMutations.refreshOverworldCollisionAfterObjectMutation();
    }

    void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        roomBombWallMutations.applyIndoorBombableWallCandidate(candidate, sourceSlot);
    }

    void queuePuzzleSolvedJingle(int sourceSlot) {
        roomBombWallMutations.queuePuzzleSolvedJingle(sourceSlot);
    }

    boolean applyIndoorBombableBlockCandidate(BombObjectInteraction.Candidate candidate) {
        return roomBombWallMutations.applyIndoorBombableBlockCandidate(candidate);
    }

    int colorShellObjectAt(RoomEntity entity, int relativeOffset) {
        return roomBombWallMutations.colorShellObjectAt(entity, relativeOffset);
    }

    void writeColorShellObject(RoomEntity entity, int objectId) {
        roomBombWallMutations.writeColorShellObject(entity, objectId);
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RomTables romTables,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomPlayerItemActions> roomPlayerItemActions,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            TransientVfxSystem transientVfxSystem) {}
}
