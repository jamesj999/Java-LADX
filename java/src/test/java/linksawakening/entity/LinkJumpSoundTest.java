package linksawakening.entity;

import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundEffectMap;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

@RequiresRom
final class LinkJumpSoundTest {
    @Test
    void directionalLedgeRequestsFallJingleOnLaunchAndFootstepNoiseOnLanding() throws Exception {
        var f = new Fixture(true);
        f.room[0x11 + 3 * 16 + 2] = 0x21;
        f.link.setPixelPosition(32, 32);
        f.input.onKeyEvent(f.config.downKey(), GLFW_PRESS);
        f.advance(35);
        assertEquals(List.of("5:LEDGE_FALL", "33:LINK_FOOTSTEP"), f.sounds);
    }

    @Test
    void overworldCliffPreservesBothRomLandingNoiseWrites() throws Exception {
        var f = new Fixture(false);
        f.room[0x11 + 3 * 16 + 2] = 0x43;
        f.link.setPixelPosition(32, 40);
        f.input.onKeyEvent(f.config.downKey(), GLFW_PRESS);
        f.advance(23);
        // $49B6 sets airborne again after $45AA on frame 20, so the
        // next default-motion update requests the same noise once more.
        // Independently observed at $02:45A8 in the local PyBoy trace.
        assertEquals(List.of("0:LEDGE_FALL", "20:LINK_FOOTSTEP", "21:LINK_FOOTSTEP"), f.sounds);
    }

    @Test
    void bedHopHasNoLaunchJingleAndLandsWithFootstepNoise() throws Exception {
        var f = new Fixture(true);
        f.link.setPixelPosition(48, 36);
        f.link.leaveMarinWakeUpBed();
        f.advance(22);
        assertEquals(List.of("19:LINK_FOOTSTEP"), f.sounds);
    }

    @Test
    void featherUsesItsOwnLaunchJingleAndTheSharedLandingNoise() throws Exception {
        var f = new Fixture(false);
        f.link.setPixelPosition(32, 32);
        f.link.useRocsFeather();
        f.advance(35);
        assertEquals(List.of("0:ROC_FEATHER_JUMP", "32:LINK_FOOTSTEP"), f.sounds);
    }

    @Test
    void landingSoundUsesRomGroundPhysicsAndRoomBottomGuards() throws Exception {
        for (int physics : new int[] {0, 5, 7, 0x0B, 0x50, 0x51}) {
            boolean indoor = physics == 0x0B || physics == 0x51;
            var f = new Fixture(indoor);
            int object = -1;
            for (int id = 0; id < 256; id++) {
                if (id != 0x61 && f.tables.objectPhysicsFlag(indoor ? RomTables.PHYSICS_TABLE_INDOORS1
                    : RomTables.PHYSICS_TABLE_OVERWORLD, id) == physics) {
                    object = id;
                    break;
                }
            }
            assertTrue(object >= 0, "ROM must contain physics " + physics);
            Arrays.fill(f.room, object);
            f.landAt(32, 32);
            assertEquals(physics == 5 || physics == 7 || physics == 0x0B
                ? List.of("0:WATER_SPLASH") : physics == 0 ? List.of("0:LINK_FOOTSTEP") : List.of(),
                f.sounds, "physics " + physics);
        }
        var well = new Fixture(false);
        Arrays.fill(well.room, 0x61);
        well.landAt(32, 32);
        assertTrue(well.sounds.isEmpty(), "$02:458D excludes wells");
        var bottom = new Fixture(false);
        bottom.landAt(32, 120);
        assertTrue(bottom.sounds.isEmpty(), "$02:4584 excludes ROM Y >= $88");
    }

    @Test
    void jumpEventsResolveToTheExactRomRequestRegistersAndIds() throws Exception {
        var f = new Fixture(false);
        var map = GameplaySoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(f.rom));
        assertEffect(map, GameplaySoundEvent.LEDGE_FALL, SoundEffectNamespace.JINGLE, 0x08);
        assertEffect(map, GameplaySoundEvent.ROC_FEATHER_JUMP, SoundEffectNamespace.JINGLE, 0x0D);
        assertEffect(map, GameplaySoundEvent.LINK_FOOTSTEP, SoundEffectNamespace.NOISE, 0x07);
        assertEffect(map, GameplaySoundEvent.WATER_SPLASH, SoundEffectNamespace.JINGLE, 0x0E);
    }

    private static void assertEffect(GameplaySoundEffectMap map, GameplaySoundEvent event,
                                     SoundEffectNamespace namespace, int id) {
        var effect = map.resolve(event).orElseThrow();
        assertEquals(namespace, effect.namespace());
        assertEquals(id, effect.id());
    }

    private static final class Fixture {
        final byte[] rom;
        final RomTables tables;
        final int[] room = new int[256];
        final InputState input = new InputState();
        final InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        final List<String> sounds = new ArrayList<>();
        final Link link;
        int frame;

        Fixture(boolean indoor) throws Exception {
            try (var in = TestRom.open()) { rom = in.readAllBytes(); }
            tables = RomTables.loadFromRom(rom);
            var collision = new OverworldCollision(tables);
            if (indoor) collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
            Arrays.fill(room, 0xFF);
            collision.setRoom(room);
            link = new Link(input, config, tables, collision, null, new PlayerState(),
                new ItemRegistry(), event -> sounds.add(frame + ":" + event));
        }

        void advance(int frames) {
            for (int i = 0; i < frames; i++, frame++) link.update();
        }

        void landAt(int x, int y) {
            link.setPixelPosition(x, y);
            link.linkAirborne.airborne = true;
            link.linkAirborne.zSubPixels = 16;
            link.linkAirborne.zVelocity = -16;
            advance(1);
        }
    }
}
