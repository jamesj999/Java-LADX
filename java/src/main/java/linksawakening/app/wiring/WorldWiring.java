package linksawakening.app.wiring;

import linksawakening.app.services.RomServices;
import linksawakening.app.services.VisualServices;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.state.PlayerState;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSpriteSheet;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.OverworldTilesetTable;
import linksawakening.world.room.RoomLoadListener;
import linksawakening.world.room.RoomLoader;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomSessionConfig;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Constructs the room runtime and its presentation collaborators. Stateless factory used during session
 * initialization.
 */
final class WorldWiring {
    private WorldWiring() {}

    static VisualServices visuals(Rom romData, RomRandomByteSource random, byte[] framebuffer) {
        int[] shield = RomBank.loadPalettes(romData, new RomAddress(0x05, 0x4C94), 1)[0];
        var sheet = TransientVfxSpriteSheet.loadFromRom(romData);
        return new VisualServices(
                new TransientVfxSystem(16),
                new DroppableRupeeSystem(16, random),
                new CutLeavesEffectRenderer(sheet),
                shield,
                framebuffer);
    }

    static RoomSession rooms(
            RomServices rom,
            VisualServices visuals,
            OverworldCollision collision,
            RomRandomByteSource random,
            RoomLoadListener music,
            GameplaySoundSink sounds) {
        Rom romData = rom.romData();
        var roomSession = new RoomSession(new RoomSessionConfig(
                romData,
                rom.gpu(),
                new RoomLoader(romData),
                new OverworldTilesetTable(romData),
                collision,
                new RoomSessionConfig.Presentation(visuals.transientVfxSystem(), visuals.droppableRupeeSystem(), music),
                random));
        roomSession.entityState().setColorShellSoundSink(sounds);
        return roomSession;
    }

    static RoomTransitionCoordinator transitions(
            RoomSession rooms,
            PlayerState player,
            TransitionController transition,
            ScrollController scroll,
            GameplaySoundSink sounds) {
        rooms.progress().onMapBoundaryWarp(player.equipmentLevels()::clearActivePowerUpForWarp);
        return new RoomTransitionCoordinator(rooms, new RoomBoundaryController(), transition, scroll, sounds);
    }
}
