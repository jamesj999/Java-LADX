package linksawakening.app.gameplay;


import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.ActiveRoom;
import linksawakening.world.OverworldBushInteraction;
import linksawakening.world.RoomSession;
import linksawakening.world.Warp;


import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;


import java.util.function.*;

/** Focused frame interactions using the current session collaborators. */
public record BushInteraction(RoomSession roomSession, OverworldBushInteraction overworldBushInteraction, Link link, EquipmentController equipmentController, GameplaySoundSink gameplaySoundSink, TransientVfxSystem transientVfxSystem, DroppableRupeeSystem droppableRupeeSystem) {
    private static final int OBJECT_GROUND_STAIRS = 0xC6;
    public void maybeCutBushWithSword() {
        if (roomSession == null
            || !roomSession.hasActiveRoom()
            || roomSession.mapCategory() != Warp.CATEGORY_OVERWORLD
            || overworldBushInteraction == null
            || link == null) {
            return;
        }
        ActiveRoom room = roomSession.activeRoom();

        Sword sword = equipmentController.activeSword();
        if (sword == null || !sword.staticCollisionActive()) {
            return;
        }

        int location = overworldBushInteraction.swordHitRoomObjectLocationForCollisionIndex(
            true, sword.staticCollisionMapIndex(link.direction()), link.pixelX(), link.pixelY());
        if (location < 0) {
            return;
        }

        int areaIndex = ROOM_OBJECTS_BASE + location;
        if (areaIndex < 0 || areaIndex >= room.roomObjectsArea().length) {
            return;
        }
        int originalObjectId = room.roomObjectsArea()[areaIndex];

        OverworldBushInteraction.CutResult cutResult = overworldBushInteraction.cutObjectAtLocation(
            location,
            room.roomId(),
            true,
            room.roomObjectsArea(),
            room.renderValues(),
            room.gbcOverlay(),
            room.tileIds(),
            room.tileAttrs()
        );
        if (!cutResult.changed()) {
            return;
        }

        gameplaySoundSink.play(cutResult.soundEvent());

        if (transientVfxSystem != null && cutResult.bushLeavesVisible()) {
            transientVfxSystem.spawn(
                TransientVfxType.BUSH_LEAVES,
                overworldBushInteraction.effectOriginXForLocation(location),
                overworldBushInteraction.effectOriginYForLocation(location)
            );
        }

        if (droppableRupeeSystem != null) {
            droppableRupeeSystem.maybeSpawnFromBush(
                originalObjectId,
                overworldBushInteraction.effectOriginXForLocation(location),
                overworldBushInteraction.effectOriginYForLocation(location)
            );
        }

        if (areaIndex >= 0 && areaIndex < room.roomObjectsArea().length
            && room.roomObjectsArea()[areaIndex] == OBJECT_GROUND_STAIRS) {
            room.replaceFirstWarpTile(location);
        }
    }
}
