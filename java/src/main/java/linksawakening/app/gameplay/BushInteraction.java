package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.*;
import linksawakening.app.debug.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
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
import linksawakening.world.transition.*;
import linksawakening.world.transition.Warp;

/** Focused frame interactions using the current session collaborators. */
public final class BushInteraction {
    private final WorldServices world;
    private final PlayerServices player;
    private final VisualServices visual;
    private final GameplaySounds sounds;

    public BushInteraction(WorldServices world, PlayerServices player, VisualServices visual, GameplaySounds sounds) {
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.visual = Objects.requireNonNull(visual);
        this.sounds = Objects.requireNonNull(sounds);
    }

    public RoomSession roomSession() {
        return world.roomSession();
    }

    public OverworldBushInteraction overworldBushInteraction() {
        return world.overworldBushInteraction();
    }

    public Link link() {
        return player.link();
    }

    public EquipmentController equipmentController() {
        return player.equipmentController();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return sounds.sink();
    }

    public TransientVfxSystem transientVfxSystem() {
        return visual.transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return visual.droppableRupeeSystem();
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
