package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WingedOctorokMotionTest {

    @Test
    void stateZeroUsesTheRomRandomWalkTablesAndEntersStateOne() {
        WingedOctorokMotion motion = new WingedOctorokMotion();
        RoomEntity entity = entity(0, 0x40, 0x40, 0);

        WingedOctorokMotion.Update update = motion.advance(entity, 0, 0x20, 0x40,
            () -> 0x02, null, 0, 0, false, 0, 0);

        assertEquals(1, update.state());
        assertEquals(0x12, update.transitionCountdown());
        assertEquals(0, motion.direction(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0, update.entity().spriteVariant());
        assertTrue(update.rockSpawn() == null);
    }

    @Test
    void stateOneAtCountdownTenSpawnsTheRockOnlyWhenFacingLink() {
        WingedOctorokMotion motion = new WingedOctorokMotion();
        RoomEntity entity = entity(0, 0x40, 0x40, 0);
        motion.forceStateForTest(0, 1, 0x0A, 0, 0, 0);

        WingedOctorokMotion.Update update = motion.advance(entity, 0, 0x60, 0x40,
            () -> 0, null, 0x0A, 0, false, 0, 0);

        assertEquals(new WingedOctorokMotion.RockSpawn(0), update.rockSpawn());
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
    }

    @Test
    void stateTwoStoresTheAnimationFrameAndReturnsToStateOneAfterZWraps() {
        WingedOctorokMotion motion = new WingedOctorokMotion();
        RoomEntity entity = entity(0, 0x40, 0x40, 0);
        motion.forceStateForTest(0, 2, 0, 0, 0xFF, 0x01);

        WingedOctorokMotion.Update update = motion.advance(entity, 4, 0x40, 0x40,
            () -> 0, null, 0, 0, false, 0, 0);

        assertEquals(1, update.state());
        assertEquals(0x20, update.transitionCountdown());
        assertEquals(0, update.entity().z());
        assertEquals(1, motion.privateState2(0));
        assertEquals(0, motion.speedZ(0));
        assertTrue(update.skippedDefaultCollision());
    }

    @Test
    void swordSelectionHonorsTheRomBSlotPriority() {
        WingedOctorokMotion motion = new WingedOctorokMotion();
        RoomEntity entity = entity(0, 0x40, 0x40, 0);

        motion.forceStateForTest(0, 1, 0, 0, 0, 0);
        WingedOctorokMotion.Update aOnly = motion.advance(entity, 0, 0x50, 0x40,
            () -> 0, null, 0, 0, true, false, 0x00, 0x01);
        assertEquals(0, aOnly.state());
        assertTrue(!aOnly.jumped());

        motion.forceStateForTest(0, 1, 0, 0, 0, 0);
        WingedOctorokMotion.Update bHeld = motion.advance(entity, 0, 0x50, 0x40,
            () -> 0, null, 0, 0, false, true, 0x00, 0x01);
        assertEquals(2, bHeld.state());
        assertTrue(bHeld.jumped());
        assertTrue(bHeld.skippedDefaultCollision());
    }

    @Test
    void generatedRectUsesTheRomGpuTileAndStateSpecificFlipAttributes() {
        List<WingedOctorokOam.Entry> first = WingedOctorokOam.entries(3, 0,
            0x40, 0x50, 0);
        assertEquals(List.of(
            new WingedOctorokOam.Entry(3, 0x3C, 0x50, 0x22, 0x40),
            new WingedOctorokOam.Entry(3, 0x4C, 0x50, 0x22, 0x60)), first);

        List<WingedOctorokOam.Entry> second = WingedOctorokOam.entries(3, 1,
            0x40, 0x50, 0);
        assertEquals(0x00, second.get(0).attributes());
        assertEquals(0x20, second.get(1).attributes());
    }

    private static RoomEntity entity(int slot, int x, int y, int variant) {
        return new RoomEntity(slot, 0, 0xAE, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xAE), variant);
    }
}
