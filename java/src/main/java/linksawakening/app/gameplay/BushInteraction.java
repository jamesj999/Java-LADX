package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.Warp;

/** Focused frame interactions using the current session collaborators. */
public final class BushInteraction {
    private final linksawakening.app.SessionContext context;

    public BushInteraction(linksawakening.app.SessionContext context) {
        this.context = java.util.Objects.requireNonNull(context);
    }

    public RoomSession roomSession() {
        return context.bushes_roomSession();
    }

    public OverworldBushInteraction overworldBushInteraction() {
        return context.bushes_overworldBushInteraction();
    }

    public Link link() {
        return context.bushes_link();
    }

    public EquipmentController equipmentController() {
        return context.bushes_equipmentController();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return context.bushes_gameplaySoundSink();
    }

    public TransientVfxSystem transientVfxSystem() {
        return context.bushes_transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return context.bushes_droppableRupeeSystem();
    }

    private static final int OBJECT_GROUND_STAIRS = 0xC6;

    public void maybeCutBushWithSword() {
        if (roomSession() == null
                || !roomSession().hasActiveRoom()
                || overworldBushInteraction() == null
                || link() == null) {
            return;
        }
        ActiveRoom room = roomSession().activeRoom();

        Sword sword = equipmentController().activeSword();
        if (sword == null || !sword.staticCollisionActive()) {
            return;
        }

        int location = overworldBushInteraction()
                .swordHitRoomObjectLocationForCollisionIndex(
                        true, sword.staticCollisionMapIndex(link().direction()), link().pixelX(), link().pixelY());
        if (location < 0) {
            return;
        }

        if (roomSession().mapCategory() != Warp.CATEGORY_OVERWORLD) {
            if (roomSession().breakIndoorSwordCrystal(location)) {
                link().clearRomAttackStepAnimationCountdown();
            }
            return;
        }

        int areaIndex = ROOM_OBJECTS_BASE + location;
        if (areaIndex < 0 || areaIndex >= room.roomObjectsArea().length) {
            return;
        }
        int originalObjectId = room.roomObjectsArea()[areaIndex];

        OverworldBushInteraction.CutResult cutResult = overworldBushInteraction()
                .cutObjectAtLocation(
                        location,
                        room.roomId(),
                        true,
                        room.roomObjectsArea(),
                        room.renderValues(),
                        room.gbcOverlay(),
                        room.tileIds(),
                        room.tileAttrs());
        if (!cutResult.changed()) {
            return;
        }

        gameplaySoundSink().play(cutResult.soundEvent());

        if (transientVfxSystem() != null && cutResult.bushLeavesVisible()) {
            transientVfxSystem()
                    .spawn(
                            TransientVfxType.BUSH_LEAVES,
                            overworldBushInteraction().effectOriginXForLocation(location),
                            overworldBushInteraction().effectOriginYForLocation(location));
        }

        if (droppableRupeeSystem() != null) {
            droppableRupeeSystem()
                    .maybeSpawnFromBush(
                            originalObjectId,
                            overworldBushInteraction().effectOriginXForLocation(location),
                            overworldBushInteraction().effectOriginYForLocation(location));
        }

        if (areaIndex >= 0
                && areaIndex < room.roomObjectsArea().length
                && room.roomObjectsArea()[areaIndex] == OBJECT_GROUND_STAIRS) {
            room.replaceFirstWarpTile(location);
        }
    }
}
