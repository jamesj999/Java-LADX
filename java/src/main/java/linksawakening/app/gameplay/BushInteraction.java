package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.SessionContext;
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
    private final SessionContext context;

    public BushInteraction(SessionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public RoomSession roomSession() {
        return context.roomSession();
    }

    public OverworldBushInteraction overworldBushInteraction() {
        return context.overworldBushInteraction();
    }

    public Link link() {
        return context.link();
    }

    public EquipmentController equipmentController() {
        return context.equipmentController();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return context.gameplaySoundSink();
    }

    public TransientVfxSystem transientVfxSystem() {
        return context.transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return context.droppableRupeeSystem();
    }

    private static final int OBJECT_GROUND_STAIRS = 0xC6;

    public void maybeCutBushWithSword() {
        if (!roomSession().rooms().hasActiveRoom() || overworldBushInteraction() == null) {
            return;
        }
        ActiveRoom room = roomSession().rooms().activeRoom();

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

        if (roomSession().rooms().mapCategory() != Warp.CATEGORY_OVERWORLD) {
            if (roomSession().terrain().breakIndoorSwordCrystal(location)) {
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

        if (cutResult.bushLeavesVisible()) {
            transientVfxSystem()
                    .spawn(
                            TransientVfxType.BUSH_LEAVES,
                            overworldBushInteraction().effectOriginXForLocation(location),
                            overworldBushInteraction().effectOriginYForLocation(location));
        }

        {
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
