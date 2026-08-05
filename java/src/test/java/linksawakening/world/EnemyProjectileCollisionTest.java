package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnemyProjectileCollisionTest {

    @Test
    void ignoresNonInteractiveAndAirborneLink() {
        RoomEntity rock = projectile(0x0A, 0x40, 0x50, 0);

        assertTrue(EnemyProjectileCollision.check(rock, 0,
            link(0x40, 0x50, 0, 0x02, 0, false)).isEmpty());
        assertTrue(EnemyProjectileCollision.check(rock, 0,
            link(0x40, 0x50, 1, 0x00, 0, false)).isEmpty());
    }

    @Test
    void usesTheRomHalfOpenWrappingWindowOnBothAxes() {
        int linkX = 0x04;
        int linkY = 0x02;
        for (int boundary : new int[] {0x00, 0x05, 0x06, 0x0B}) {
            RoomEntity rock = projectile(0x0A,
                positionForWindow(linkX, boundary), positionForWindow(linkY, boundary), 0);
            assertTrue(EnemyProjectileCollision.check(rock, 0,
                link(linkX, linkY, 0, 0x00, 0, false)).isPresent(),
                () -> "Expected collision at window value " + boundary);
        }

        RoomEntity xOutside = projectile(0x0A, positionForWindow(linkX, 0x0C), linkY, 0);
        assertTrue(EnemyProjectileCollision.check(xOutside, 0,
            link(linkX, linkY, 0, 0x00, 0, false)).isEmpty());

        RoomEntity yOutside = projectile(0x0A, linkX, positionForWindow(linkY, 0x0C), 0);
        assertTrue(EnemyProjectileCollision.check(yOutside, 0,
            link(linkX, linkY, 0, 0x00, 0, false)).isEmpty());
    }

    @Test
    void shieldBlocksOnlyTheOppositeProjectileDirection() {
        int[] reversedDirections = {1, 0, 3, 2};
        for (int projectileDirection = 0; projectileDirection < 4; projectileDirection++) {
            RoomEntity rock = projectile(0x0A, 0x40, 0x50, 0);
            EntityProjectileEvent blocked = EnemyProjectileCollision.check(rock,
                projectileDirection,
                link(0x40, 0x50, 0, 0x00, reversedDirections[projectileDirection], true))
                .orElseThrow();

            assertEquals(EntityProjectileEvent.Kind.SHIELD_BLOCK, blocked.kind());
            assertEquals(0xFF, blocked.collisionValue());
            assertEquals(0, blocked.linkDamage());
            assertEquals(EntityProjectileEvent.SoundChannel.JINGLE, blocked.soundChannel());
            assertEquals(0x16, blocked.soundId());
            assertFalse(blocked.remove());
        }
    }

    @Test
    void unshieldedContactReportsDamageAndKeepsTheRomTypeSpecificLifecycle() {
        EntityProjectileEvent rockHit = EnemyProjectileCollision.check(
            projectile(0x0A, 0x40, 0x50, 0), 0,
            link(0x40, 0x50, 0, 0x00, 0, false)).orElseThrow();
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, rockHit.kind());
        assertEquals(0x08, rockHit.linkDamage());
        assertEquals(0xFF, rockHit.collisionValue());
        assertEquals(EntityProjectileEvent.SoundChannel.WAVE, rockHit.soundChannel());
        assertEquals(0x03, rockHit.soundId());
        assertFalse(rockHit.remove());

        EntityProjectileEvent arrowHit = EnemyProjectileCollision.check(
            projectile(0x0C, 0x40, 0x50, 0), 0,
            link(0x40, 0x50, 0, 0x00, 0, false)).orElseThrow();
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, arrowHit.kind());
        assertEquals(0x08, arrowHit.linkDamage());
        assertEquals(0, arrowHit.collisionValue());
        assertTrue(arrowHit.remove());

        EntityProjectileEvent shieldFacingHit = EnemyProjectileCollision.check(
            projectile(0x0A, 0x40, 0x50, 0), 0,
            link(0x40, 0x50, 0, 0x00, 0, true)).orElseThrow();
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, shieldFacingHit.kind());
    }

    @Test
    void mirrorShieldReflectsLaserOnlyForTheRomDirectionWindow() {
        EntityProjectileEvent reflected = EnemyProjectileCollision.check(
            projectile(0x2B, 0x40, 0x50, 0), 2,
            new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0x00,
                0, true, 2, 0)).orElseThrow();

        assertEquals(EntityProjectileEvent.Kind.SHIELD_BLOCK, reflected.kind());
        assertEquals(0x02, reflected.collisionValue());
        assertEquals(0, reflected.linkDamage());
        assertEquals(EntityProjectileEvent.SoundChannel.JINGLE, reflected.soundChannel());
        assertEquals(0x07, reflected.soundId());
        assertFalse(reflected.remove());
        assertTrue(reflected.swordPokeVfx());
        assertEquals(0x40, reflected.swordPokeX());
        assertEquals(0x50, reflected.swordPokeY());
        assertEquals(0xF8, reflected.linkSpeedX());
        assertEquals(0x00, reflected.linkSpeedY());
        assertEquals(0x10, reflected.linkIgnoreCollisionCountdown());

        EntityProjectileEvent normalShield = EnemyProjectileCollision.check(
            projectile(0x2B, 0x40, 0x50, 0), 2,
            new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0x00,
                0, true, 1, 0)).orElseThrow();
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, normalShield.kind());
        assertEquals(0x08, normalShield.linkDamage());
        assertTrue(normalShield.remove());

        EntityProjectileEvent wrongDirection = EnemyProjectileCollision.check(
            projectile(0x2B, 0x40, 0x50, 0), 8,
            new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0x00,
                0, true, 2, 0)).orElseThrow();
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, wrongDirection.kind());
    }

    @Test
    void comparesLinkYWithTheProjectileVisualPositionAfterZSubtraction() {
        RoomEntity airborneRock = projectile(0x0A, 0x40, 0x58, 0x08);

        assertTrue(EnemyProjectileCollision.check(airborneRock, 0,
            link(0x40, 0x50, 0, 0x00, 0, false)).isPresent());
    }

    @Test
    void projectileCollisionIsSeparateFromGenericEnemyCombat() {
        assertFalse(RoomEntityCombatRules.supportsEnemyCollision(0x0A));
        assertFalse(RoomEntityCombatRules.supportsEnemyCollision(0x0C));
        assertTrue(EnemyProjectileCollision.check(
            projectile(0x0A, 0x40, 0x50, 0), 0,
            link(0x40, 0x50, 0, 0x00, 0, false)).isPresent());
    }

    @Test
    void runtimeEmitsProjectileEventsBeforeAdvancingTheSharedHandler() {
        RoomEntityRuntime rockRuntime = RoomEntityRuntime.from(snapshot(
            projectile(0x0A, 0x40, 0x50, 0)), false, () -> 0);

        var rockEvents = rockRuntime.tickWithProjectileEvents(
            0, 0x40, 0x50, () -> 0, null,
            link(0x40, 0x50, 0, 0x00, 1, true));

        assertEquals(1, rockEvents.size());
        assertEquals(EntityProjectileEvent.Kind.SHIELD_BLOCK, rockEvents.getFirst().kind());
        assertEquals(0x18, rockRuntime.enemyProjectileTransitionCountdown(0));

        var nextFrameEvents = rockRuntime.tickWithProjectileEvents(
            1, 0x40, 0x50, () -> 0, null,
            link(0x40, 0x50, 0, 0x00, 1, true));
        assertTrue(nextFrameEvents.isEmpty());

        RoomEntityRuntime arrowRuntime = RoomEntityRuntime.from(snapshot(
            projectile(0x0C, 0x40, 0x50, 0)), false, () -> 0);
        var arrowEvents = arrowRuntime.tickWithProjectileEvents(
            0, 0x40, 0x50, () -> 0, null,
            link(0x40, 0x50, 0, 0x00, 0, false));

        assertEquals(1, arrowEvents.size());
        assertTrue(arrowEvents.getFirst().remove());
        assertEquals(EntityStatus.DISABLED, arrowRuntime.snapshot().slots().get(0).status());
        assertEquals(0, arrowRuntime.enemyProjectileTransitionCountdown(0));
    }

    private static EnemyProjectileCollision.LinkState link(int x, int y, int z,
                                                              int motionState,
                                                              int direction,
                                                              boolean shield) {
        return new EnemyProjectileCollision.LinkState(x, y, z, motionState, direction, shield);
    }

    private static RoomEntity projectile(int type, int x, int y, int z) {
        return new RoomEntity(0, -1, type, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), -1, 0, 0, z);
    }

    private static int positionForWindow(int linkPosition, int windowValue) {
        return (linkPosition - (windowValue - 0x06)) & 0xFF;
    }

    private static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        RoomEntity[] slots = new RoomEntity[EntityRoomLoader.MAX_ENTITIES];
        for (int slot = 0; slot < slots.length; slot++) {
            slots[slot] = RoomEntity.disabled(slot);
        }
        for (RoomEntity entity : entities) {
            slots[entity.slot()] = entity;
        }
        return new RoomEntitySnapshot(java.util.Arrays.asList(slots), null, null);
    }
}
