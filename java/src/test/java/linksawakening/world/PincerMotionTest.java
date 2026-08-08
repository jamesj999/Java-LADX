package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PincerMotionTest {

    @Test
    void quantizesTheRomVectorIntoTheDirectionalSpriteVariant() {
        assertEquals(2, PincerMotion.lungeVariantForVector(0x00, 0x1F));
        assertEquals(4, PincerMotion.lungeVariantForVector(0x1F, 0x00));
        assertEquals(6, PincerMotion.lungeVariantForVector(0x00, 0xE1));
        assertEquals(9, PincerMotion.lungeVariantForVector(0xE1, 0x1F));
    }

    @Test
    void stateZeroStoresTheHoleAndAdvancesToHiddenState() {
        PincerMotion motion = new PincerMotion();
        RoomEntity entity = entity(0, 64, 80, 0);

        PincerMotion.Update update = motion.advance(entity, 0, 0, 64, 80);

        assertEquals(1, update.state());
        assertEquals(64, motion.holeX(0));
        assertEquals(80, motion.holeY(0));
        assertEquals(64, update.entity().x());
        assertEquals(80, update.entity().y());
    }

    @Test
    void hiddenStateEnablesProjectileNoclipAndStartsTheLungeWindowNearLink() {
        PincerMotion motion = new PincerMotion();
        RoomEntity entity = entity(0, 64, 80, 0);
        motion.advance(entity, 0, 0, 64, 80);

        PincerMotion.Update update = motion.advance(entity, 0, 0, 64, 80);

        assertEquals(2, update.state());
        assertEquals(0x30, update.transitionCountdown());
        assertEquals(0x40, update.physicsFlags() & 0x40);
        assertEquals(0, update.entity().spriteVariant());
    }

    @Test
    void prepareStateSetsTheSourceSpeedAtCountdownTenAndEntersLungeAtZero() {
        PincerMotion motion = new PincerMotion();
        EntitySpriteDefinition definition = EntitySpriteDefinition.unsupported(0xB0);
        RoomEntity entity = new RoomEntity(0, 0, 0xB0, 64, 80, EntityStatus.ACTIVE,
            definition, 0);
        motion.forceStateForTest(0, 2, 0x10, 0x40);

        PincerMotion.Update prepared = motion.advance(entity, 0x10, 0x40, 96, 80);
        assertEquals(2, prepared.state());
        assertEquals(0x10, prepared.transitionCountdown());
        assertEquals(0x18, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(1, prepared.entity().spriteVariant());
        assertEquals(2, motion.lungeVariant(0));

        motion.forceStateForTest(0, 2, 0, 0x40);
        PincerMotion.Update lunging = motion.advance(entity, 0, 0x40, 96, 80);
        assertEquals(3, lunging.state());
        assertEquals(0x18, lunging.transitionCountdown());
        assertEquals(0, lunging.physicsFlags() & 0x40);
        assertEquals(2, lunging.entity().spriteVariant());
    }

    @Test
    void returningStateUsesTheStoredHoleAndLoopsToHiddenState() {
        PincerMotion motion = new PincerMotion();
        RoomEntity entity = entity(0, 64, 80, 0);
        motion.forceStateForTest(0, 5, 0, 0);
        motion.setHoleForTest(0, 64, 80);

        PincerMotion.Update update = motion.advance(entity, 0, 0, 64, 80);

        assertEquals(1, update.state());
        assertEquals(0x20, update.transitionCountdown());
        assertEquals(65, update.entity().x());
        assertEquals(81, update.entity().y());
        assertEquals(0, update.entity().spriteVariant());
    }

    private static RoomEntity entity(int slot, int x, int y, int variant) {
        return new RoomEntity(slot, 0, 0xB0, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xB0), variant);
    }
}
