package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GenieMotionTest {
    @Test
    void initializesGenieJarInPrivateStateZeroWithNormalJarProperties() {
        GenieMotion motion = new GenieMotion();

        GenieMotion.JarState state = motion.initialize(3);

        assertEquals(0, state.privateState1());
        assertEquals(0x06, state.health());
        assertEquals(0x91, state.physicsFlags());
        assertEquals(0x8C, state.hitboxFlags());
        assertEquals(0, motion.privateState1(3));
    }

    @Test
    void state0BelowNormalBuildThresholdHoldsHealthAndHarmlessHitbox() {
        GenieMotion motion = new GenieMotion();

        GenieMotion.Update update = motion.advanceState0(
            0, 0x48, 0x30, 0x00, 0x02, true, true);

        assertEquals(0, update.jar().privateState1());
        assertEquals(0x20, update.jar().health());
        assertEquals(0x81, update.jar().physicsFlags());
        assertEquals(0x80, update.jar().hitboxFlags());
        assertNull(update.bodySpawn());
        assertFalse(update.jarSmashed());
        assertEquals(-1, update.noiseSfx());
    }

    @Test
    void thresholdSpawnsBodyAtJarXAndWrappedYMinus18WithSourceFields() {
        GenieMotion motion = new GenieMotion();

        GenieMotion.Update update = motion.advanceState0(
            0, 0x48, 0x10, 0x04, 0x03, true, true);

        assertTrue(update.jarSmashed());
        assertTrue(update.sourceUnloaded());
        assertEquals(0x29, update.noiseSfx());
        assertEquals(new GenieMotion.BodySpawnRequest(
            0x48, 0xF8, 0x02, 0x27, 0x08), update.bodySpawn());
        assertEquals(new GenieMotion.RockSpawnRequest(
            0x05, 0x48, 0x0C, 0x00, 0x0F, 0xC4), update.rockSpawn());
        assertTrue(update.rockSpawnSucceeded());

        GenieMotion.Update repeated = motion.advanceState0(
            0, 0x48, 0x10, 0x04, 0x03, true, true);
        assertNull(repeated.bodySpawn());
        assertNull(repeated.rockSpawn());
        assertFalse(repeated.jarSmashed());
        assertFalse(repeated.sourceUnloaded());
        assertEquals(-1, repeated.noiseSfx());
    }

    @Test
    void clearedSlotCanRunTheJarThresholdTransitionAgainAfterReuse() {
        GenieMotion motion = new GenieMotion();
        motion.advanceState0(2, 0x48, 0x30, 0, 3, true, true);

        motion.clear(2);
        GenieMotion.Update reused = motion.advanceState0(
            2, 0x50, 0x40, 0, 3, true, true);

        assertTrue(reused.jarSmashed());
        assertEquals(0x50, reused.bodySpawn().x());
    }

    @Test
    void bodySpawnCanSucceedWhileRockSpawnFailsWithoutUnloadingTheJar() {
        GenieMotion motion = new GenieMotion();

        GenieMotion.Update update = motion.advanceState0(
            0, 0x48, 0x30, 0x04, 0x03, true, false);

        assertEquals(new GenieMotion.BodySpawnRequest(
            0x48, 0x18, 0x02, 0x27, 0x08), update.bodySpawn());
        assertEquals(new GenieMotion.RockSpawnRequest(
            0x05, 0x48, 0x2C, 0x00, 0x0F, 0xC4), update.rockSpawn());
        assertFalse(update.rockSpawnSucceeded());
        assertFalse(update.jarSmashed());
        assertFalse(update.sourceUnloaded());
        assertEquals(0x29, update.noiseSfx());
    }

    @Test
    void exhaustedEntitySlotsLeaveJarAndEffectsUnchanged() {
        GenieMotion motion = new GenieMotion();
        motion.advanceState0(0, 0x48, 0x30, 0x04, 0x02, true, true);

        GenieMotion.Update update = motion.advanceState0(
            0, 0x48, 0x30, 0x04, 0x03, false, true);

        assertEquals(0, update.jar().privateState1());
        assertEquals(0x20, update.jar().health());
        assertEquals(0x81, update.jar().physicsFlags());
        assertEquals(0x80, update.jar().hitboxFlags());
        assertNull(update.bodySpawn());
        assertNull(update.rockSpawn());
        assertFalse(update.jarSmashed());
        assertFalse(update.sourceUnloaded());
        assertEquals(-1, update.noiseSfx());
        assertEquals(0, motion.privateState1(0));
    }
}
