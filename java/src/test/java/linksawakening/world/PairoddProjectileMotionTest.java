package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PairoddProjectileMotionTest {

    @Test
    void initializeSpawnUsesTheRomInfinityNormVectorOfLengthTwentyFour() {
        PairoddProjectileMotion motion = new PairoddProjectileMotion();
        RoomEntity source = entity(0x40, 0x40);

        motion.initializeSpawn(0, source, 0x50, 0x48);

        assertEquals(0x18, motion.speedX(0));
        assertEquals(0x0C, motion.speedY(0));
    }

    @Test
    void roomLoadedProjectileKeepsTheRandomDirectionUsedByShieldCollision() {
        PairoddProjectileMotion motion = new PairoddProjectileMotion();

        motion.initialize(0, () -> 0xFF);

        assertEquals(3, motion.direction(0));
    }

    @Test
    void projectileUsesBankFourFixedPointMovementAndFrameBitThreeAnimation() {
        PairoddProjectileMotion motion = new PairoddProjectileMotion();
        RoomEntity source = entity(0x40, 0x40);
        motion.initializeSpawn(0, source, 0x50, 0x48);

        RoomEntity frame0 = motion.advance(source, 0);
        RoomEntity frame1 = motion.advance(frame0, 1);
        RoomEntity frame8 = motion.advance(frame1, 8);

        assertEquals(0x41, frame0.x());
        assertEquals(0x40, frame0.y());
        assertEquals(0, frame0.spriteVariant());
        // Bank-$04 adds the high nibble and the accumulator carry on every tick;
        // $18 therefore advances by one pixel on tick one and two on tick two.
        assertEquals(0x43, frame1.x());
        assertEquals(0x41, frame1.y());
        assertEquals(1, frame8.spriteVariant());
    }

    @Test
    void clearResetsProjectileSpeedAndInitialization() {
        PairoddProjectileMotion motion = new PairoddProjectileMotion();
        motion.initializeSpawn(0, entity(0x40, 0x40), 0x50, 0x48);

        motion.clear(0);

        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, -1, 0x58, x, y, EntityStatus.ACTIVE,
            projectileDefinition(), 0);
    }

    private static EntitySpriteDefinition projectileDefinition() {
        return new EntitySpriteDefinition(0x58, 0x04, 0x5EF4,
            EntitySpriteDefinition.Shape.PAIR, 0, List.of(
                new EntitySpriteDefinition.Variant(attribute(0x7C), attribute(0x7C)),
                new EntitySpriteDefinition.Variant(attribute(0x7E), attribute(0x7E))));
    }

    private static EntitySpriteDefinition.OamAttribute attribute(int tile) {
        return new EntitySpriteDefinition.OamAttribute(tile, 0);
    }
}
