package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PairoddProjectileObjectCollisionTest {

    @Test
    void matchesApplySwordIntersectionPhysicsFlagBoundaries() {
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x01));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x10));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x4F));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x52));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x7B));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x90));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x9F));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0xD0));
        assertTrue(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0xFF));

        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x00));
        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x02));
        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x50));
        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x51));
        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x7C));
        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0x8F));
        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0xA0));
        assertFalse(PairoddProjectileObjectCollision.collidesWithPhysicsFlag(0xCF));
    }
}
