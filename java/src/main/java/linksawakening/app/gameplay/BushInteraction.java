package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.GameplaySounds;
import linksawakening.app.PlayerServices;
import linksawakening.app.VisualServices;
import linksawakening.app.WorldServices;
import linksawakening.equipment.Sword;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.ActiveRoom;
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

    private static final int OBJECT_GROUND_STAIRS = 0xC6;

    public void maybeCutBushWithSword() {
        if (!world.roomSession().rooms().hasActiveRoom()) {
            return;
        }
        ActiveRoom room = world.roomSession().rooms().activeRoom();

        Sword sword = player.equipmentController().activeSword();
        if (sword == null || !sword.staticCollisionActive()) {
            return;
        }

        int location = world.overworldBushInteraction()
                .swordHitRoomObjectLocationForCollisionIndex(
                        true,
                        sword.staticCollisionMapIndex(player.link().direction()),
                        player.link().pixelX(),
                        player.link().pixelY());
        if (location < 0) {
            return;
        }

        if (world.roomSession().rooms().mapCategory() != Warp.CATEGORY_OVERWORLD) {
            if (world.roomSession().terrain().breakIndoorSwordCrystal(location)) {
                player.link().clearRomAttackStepAnimationCountdown();
            }
            return;
        }

        int areaIndex = ROOM_OBJECTS_BASE + location;
        if (areaIndex < 0 || areaIndex >= room.roomObjectsArea().length) {
            return;
        }
        int originalObjectId = room.roomObjectsArea()[areaIndex];

        OverworldBushInteraction.CutResult cutResult = world.overworldBushInteraction()
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

        applyCut(room, location, areaIndex, originalObjectId, cutResult);
    }

    private void applyCut(
            ActiveRoom room,
            int location,
            int areaIndex,
            int originalObjectId,
            OverworldBushInteraction.CutResult cutResult) {
        sounds.sink().play(cutResult.soundEvent());

        if (cutResult.bushLeavesVisible()) {
            visual.transientVfxSystem()
                    .spawn(
                            TransientVfxType.BUSH_LEAVES,
                            world.overworldBushInteraction().effectOriginXForLocation(location),
                            world.overworldBushInteraction().effectOriginYForLocation(location));
        }

        {
            visual.droppableRupeeSystem()
                    .maybeSpawnFromBush(
                            originalObjectId,
                            world.overworldBushInteraction().effectOriginXForLocation(location),
                            world.overworldBushInteraction().effectOriginYForLocation(location));
        }

        if (areaIndex >= 0
                && areaIndex < room.roomObjectsArea().length
                && room.roomObjectsArea()[areaIndex] == OBJECT_GROUND_STAIRS) {
            room.replaceFirstWarpTile(location);
        }
    }
}
