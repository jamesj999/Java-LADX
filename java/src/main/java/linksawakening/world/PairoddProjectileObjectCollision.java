package linksawakening.world;

/** Bank-$03 ApplySwordIntersectionWithObjects predicate for Pairodd's $58 path. */
final class PairoddProjectileObjectCollision {
    private PairoddProjectileObjectCollision() {
    }

    /**
     * Returns whether the source routine terminates a Pairodd projectile for
     * the selected object's physics byte. The $FF branch unloads the entity
     * directly, so it is included in this termination predicate.
     */
    static boolean collidesWithPhysicsFlag(int physicsFlag) {
        int flag = physicsFlag & 0xFF;
        if (flag == 0x00) {
            return false;
        }
        if (flag == 0xFF) {
            return true;
        }
        // The D0-D3 branch is checked before the generic range tests. Pairodd
        // does not establish thrown-object state, so the ordinary projectile
        // path reaches the collision write for these ledge values.
        if (flag >= 0xD0 && flag < 0xD4) {
            return true;
        }
        if (flag >= 0x7C && flag < 0x90) {
            return false;
        }
        if (flag >= 0xA0) {
            return false;
        }
        if (flag == 0x50 || flag == 0x51) {
            return false;
        }
        return flag == 0x01 || flag >= 0x10;
    }
}
