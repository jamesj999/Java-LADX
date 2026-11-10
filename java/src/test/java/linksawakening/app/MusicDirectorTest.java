package linksawakening.app;

import linksawakening.audio.music.*;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresRom
final class MusicDirectorTest {
    @Test
    void swordIntroUsesItsRomLoopWithoutHostTrackReplacement() throws Exception {
        byte[] rom;
        try (var stream = TestRom.open()) { rom = stream.readAllBytes(); }
        try (var player = new FrameAudioPlayer(rom, 8000, (samples, rate) -> {})) {
            var controller = new GameplayMusicController(AreaMusicResolver.fromRom(rom),
                MusicCatalog.fromRom(rom), player);
            var director = new MusicDirector(player, controller);
            director.playDirectMusic(MusicTrackIds.MUSIC_OVERWORLD_INTRO);
            for (int frame = 0; frame < 2400; frame++) director.tick();
            assertTrue(player.isMusicPlaying());
            assertEquals(MusicTrackIds.MUSIC_OVERWORLD_INTRO, controller.currentTrackId());
            assertEquals(MusicTrackIds.MUSIC_OVERWORLD, controller.defaultTrackId());
        }
    }
    @Test
    void liveRoomLoadsDistinguishScrollingFromWarps() throws Exception {
        byte[] rom;
        try (var stream = TestRom.open()) { rom = stream.readAllBytes(); }
        try (var transport = new FrameAudioPlayer(rom, 8000, (samples, rate) -> {})) {
            var controller = new GameplayMusicController(AreaMusicResolver.fromRom(rom),
                MusicCatalog.fromRom(rom), transport);
            var director = new MusicDirector(transport, controller);
            var state = new linksawakening.state.PlayerState();
            var holder = new linksawakening.world.room.RoomSession[1];
            var rooms = new linksawakening.world.room.RoomSession(rom, new linksawakening.gpu.GPU(),
                new linksawakening.world.room.RoomLoader(rom),
                new linksawakening.world.room.OverworldTilesetTable(rom),
                new linksawakening.physics.OverworldCollision(linksawakening.rom.RomTables.loadFromRom(rom)),
                new linksawakening.vfx.TransientVfxSystem(16), null,
                room -> director.selectMusicForLoadedRoom(room, state, holder[0]));
            holder[0] = rooms;
            rooms.loadInitialOverworld(0xF2);
            state.setSwordLevel(1);
            director.playDirectMusic(0x31);
            var scroll = new linksawakening.world.transition.ScrollController();
            rooms.startAdjacentOverworldScroll(scroll, linksawakening.world.transition.ScrollController.UP, 64, 0);
            assertEquals(0xE2, rooms.currentRoomId());
            assertEquals(0x31, controller.currentTrackId());
            assertEquals(0x05, rooms.entityDefaultMusicTrack());
            rooms.loadWarpDestination(new linksawakening.world.transition.Warp(0, 0, 0xE2, 64, 64, -1));
            assertEquals(0x05, controller.currentTrackId());
        }
    }

}
