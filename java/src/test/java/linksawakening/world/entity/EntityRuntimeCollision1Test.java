package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeCollision1Test extends EntityRuntimeTestSupport {

    @Test
    void friendlyNpcHandlersRestoreLinksPreEntityPositionOnContact() {
        RoomEntity tarin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3F)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x3F))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(tarin))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 0, 0x00, 0x00, () -> 0, null, new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                0x50,
                0x60,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false)));

        assertEquals(
                List.of(new LinkFinalPositionRequest(0, true, true, false)),
                runtime.consumePendingLinkFinalPositionRequests());
    }

    @Test
    void friendlyNpcPushUsesTheRomAirPermittedCollisionEntry() {
        RoomEntity tarin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3F)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x3F))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(tarin))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 0, 0x00, 0x00, () -> 0, null, new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                0x50,
                0x60,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x50, 0x60, 1, 0, 3, false)));

        assertEquals(
                List.of(new LinkFinalPositionRequest(0, true, true, false)),
                runtime.consumePendingLinkFinalPositionRequests());
    }

    @Test
    void friendlyNpcContactUsesTheRomNpcHitbox() {
        RoomEntity marin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3E)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x3E))
                .build();

        // HITBOX_NPC is ($08,$06,$06,$08), so this edge contact still
        // collides; the ordinary enemy box ($08,$05,$08,$05) would miss it.
        assertTrue(RoomEntityCombatRules.overlapsLink(marin, 0x59, 0x60));
    }

    @Test
    void largeFriendlyNpcsUseTheRomBigNpcHitbox() {
        RoomEntity bear = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xB5)
                .at(0x40, 0x50)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0xB5))
                .build();

        assertTrue(RoomEntityCombatRules.overlapsLink(bear, 0x4D, 0x50));
    }

    @Test
    void ordinaryEnemyContactCarriesTheRomLinkRecoilResponse() {
        RoomEntity octorok = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x50)
                .spriteDefinition(pairDefinition(0x09, 2))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(octorok)).build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x58, false, true, false, 0, 0, 0, 0));

        assertEquals(1, events.size());
        assertEquals(0x04, events.get(0).linkDamage());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(0, 0x14, 0x10),
                events.get(0).linkCollisionResponse());
    }

    @Test
    void specialEnemyHandlersDoNotReceiveTheGenericLinkRecoilResponse() {
        assertFalse(RoomEntityCombatRules.usesStandardLinkContactResponse(0x1C));
        // Beetles only use the special branch for shield contact ($03:6F2A).
        assertTrue(RoomEntityCombatRules.usesStandardLinkContactResponse(0x2C));
        assertFalse(RoomEntityCombatRules.usesStandardLinkContactResponse(0x55));
        assertFalse(RoomEntityCombatRules.usesStandardLinkContactResponse(0x9C));
    }

    @Test
    void rejectsPowerRecoilDeathForNonDyingEntities() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(body)
                .spriteVariant(1)
                .powerRecoilDeath(true)
                .build());
    }

    @Test
    void hydratesPowerRecoilDeathStateFromAnInitialDyingEntity() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        RoomEntity initialEntity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(2)
                .powerRecoilDeath(true)
                .build();
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(snapshot(initialEntity)).build();

        assertTrue(runtime.enemies().powerRecoilDeath(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
    }

    @Test
    void disablingARecoilDeathClearsItsInternalPowerState() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x19)
                        .at(64, 64)
                        .spriteDefinition(keeseDefinition())
                        .build()))
                .build();

        runtime.resolveCombat(new EntityVerticalContactFrame(
                0, 120, 120, false, true, true, 72, 1, 72, 1, new EnemyAttackContext(1, false, true, false, false)));
        assertTrue(runtime.enemies().powerRecoilDeath(0));

        for (int frame = 0; frame < 0x40; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }

        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
        assertFalse(runtime.enemies().powerRecoilDeath(0));
    }

    @Test
    void richBackgroundProbeSuppliesIgnoreHitsToLegacyRecoilMovement() {
        RoomEntity initial = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x40)
                .spriteDefinition(pairDefinition(0x0B, 2))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(initial)).build();
        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.configuration().setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        });

        List<EntityCombatEvent> events = runtime.resolveCombat(
                new EntitySwordContactFrame(0, 0x40, 0x40, false, false, true, 0x30, 0x30, 0x30, 0x30));
        assertEquals(1, events.size());
        assertTrue(runtime.enemies().enemyRecoilActive(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> true));

        assertTrue(
                observedIgnoreHits.contains(0x09),
                () -> "observed=" + observedIgnoreHits
                        + " x=" + runtime.inspection().snapshot().slots().get(0).x()
                        + " status="
                        + runtime.inspection().snapshot().slots().get(0).status()
                        + " recoil=" + runtime.enemies().enemyRecoilActive(0)
                        + " countdown=" + runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0x3D, runtime.inspection().snapshot().slots().get(0).x());
    }

    @Test
    void ledgeCollisionStateUsesRomResetBytesAndClearsWithTheEntity() {
        RoomEntity initial = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x40)
                .spriteDefinition(pairDefinition(0x09, 2))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(initial)).build();

        assertEquals(0xFF, runtime.thrownDirection(0));
        assertEquals(0x00, runtime.enemies().ledgeTransitionTimer(0));

        EntityRuntimeFixture.debug(runtime).setThrownDirectionForTest(0, 0x02);
        EntityRuntimeFixture.debug(runtime).setLedgeTransitionTimerForTest(0, 0x07);
        assertEquals(0x02, runtime.thrownDirection(0));
        assertEquals(0x07, runtime.enemies().ledgeTransitionTimer(0));

        runtime.clearEntity(0);
        assertEquals(0xFF, runtime.thrownDirection(0));
        assertEquals(0x00, runtime.enemies().ledgeTransitionTimer(0));
    }

    @Test
    void aPowerRecoilSwordHitMarksTheRomPowerDeathPresentation() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x19)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        List<EntityCombatEvent> events = runtime.resolveCombat(new EntityVerticalContactFrame(
                0, 120, 120, false, true, true, 72, 1, 72, 1, new EnemyAttackContext(1, false, true, false, false)));

        assertEquals(1, events.size());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(0));
        assertEquals(0x20, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0x40, runtime.enemies().dyingCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertTrue(runtime.inspection().snapshot().slots().get(0).powerRecoilDeath());
        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(0x40 - 11, runtime.enemies().dyingCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertTrue(runtime.enemies().enemyIgnoreHitsCountdown(0) > 0);
    }

    @Test
    void maskedIronMaskRejectsARearSwordHitWithTheRomPokeResponse() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x24)
                        .at(0x50, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        // Java direction 1 is ROM UP, while the freshly initialized Iron Mask
        // is facing ROM RIGHT (direction 0).
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 0xC0, () -> 0, null, null, 0, 1, 0));
        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 0xC0, 0xC0, false, true, true, 0x58, 1, 0x58, 1));

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertTrue(event.swordHit());
        assertEquals(0, event.enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x50, 0x50), event.swordPokeVfx());
        assertEquals(2, runtime.enemies().enemyHealth(0));
        assertEquals(0x10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0xF0, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedXForTest(0));
        assertEquals(0xF0, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedYForTest(0));
    }

    @Test
    void clearingARecoilingEnemyClearsAllRecoilStateBeforeSlotReuse() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0B)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertTrue(runtime.enemies().enemyRecoilActive(0));

        assertEquals(1, runtime.clearEntity(0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertFalse(runtime.enemies().enemyRecoilActive(0));
        assertEquals(0, runtime.enemies().enemyRecoilSpeedX(0));
        assertEquals(0, runtime.enemies().enemyRecoilSpeedY(0));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void dyingCleanupClearsRecoilStateWhenTheRomDeathCountdownExpires() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x09)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertTrue(runtime.enemies().enemyRecoilActive(0));

        for (int frame = 0; frame < 0x40; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertFalse(runtime.enemies().enemyRecoilActive(0));
        assertEquals(0, runtime.enemies().enemyRecoilSpeedX(0));
        assertEquals(0, runtime.enemies().enemyRecoilSpeedY(0));
    }

    @Test
    void romBurningStatusUsesTheSharedSpecialDamageValueAndExpiresIntoDeath() throws IOException {
        byte[] rom = romWithSwordResult(0x09, 0xFE);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x09)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x09, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(0xFE, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(
                EntityStatus.BURNING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x60, runtime.enemies().transitionCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.get(0).secondarySoundChannel());
        assertEquals(0x12, events.get(0).secondarySoundId());
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        for (int frame = 0; frame < 0x60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x1F, runtime.enemies().dyingCountdown(0));
        assertEquals(3, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertFalse(runtime.inspection().snapshot().slots().get(0).powerRecoilDeath());
        assertEquals(0x04, runtime.enemies().physicsFlags(0));
        List<EntityCombatEvent> expiryEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, expiryEvents.size());
        assertEquals(
                EntityCombatEvent.SoundChannel.NOISE, expiryEvents.getFirst().soundChannel());
        assertEquals(0x13, expiryEvents.getFirst().soundId());
    }

    @Test
    void unknownRomSpecialDamageRemainsEventDataWithoutChangingHealthOrStatus() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(romWithSwordResult(0x09, 0xFD));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x09)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x09, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(0xFD, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(EntityCombatEvent.SoundChannel.NONE, events.get(0).secondarySoundChannel());
        assertEquals(-1, events.get(0).secondarySoundId());
    }

    @Test
    void romCombatTablesDriveInitialHealthAndContactDamage() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x09)
                                .at(64, 64)
                                .spriteDefinition(pairDefinition(0x09, 2))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x0B)
                                .at(96, 64)
                                .spriteDefinition(pairDefinition(0x0B, 2))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(2, runtime.enemies().enemyHealth(1));

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());
    }

    @Test
    void attackContextUsesRomSwordUpgradeDamageInTheRuntime() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0B)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x0B, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.resolveCombat(new EntityVerticalContactFrame(
                0, 120, 120, false, true, true, 72, 1, 72, 1, new EnemyAttackContext(2, false, false, false, false)));

        assertEquals(1, events.size());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(2, events.get(0).enemyDamage());
        assertEquals(-1, events.get(0).enemySpecialAction());
    }

    @Test
    void zeroRomSwordResultKeepsHealthAndReportsOnlyTheBump() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x15)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x15, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(4, runtime.enemies().enemyHealth(0));
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(-1, events.get(0).enemySpecialAction());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x09, events.get(0).soundId());
    }

    @Test
    void collisionTypeIsExplicitPerTickAndRevealKeepsHiddenPresentationForThatTick() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);

        RoomEntityRuntime compatibilityRuntime = RoomEntityRuntime.builder(
                        snapshot(ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        compatibilityRuntime.tick(EntityTestFrames.frame(compatibilityRuntime, 0, 0x50, 0x50, sequence(0x00)));
        assertEquals(
                -1, compatibilityRuntime.inspection().snapshot().slots().get(0).spriteVariant());

        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        RoomEntity before = runtime.inspection().snapshot().slots().get(0);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, 0x08, sequence(0x00)));

        RoomEntity revealTick = runtime.inspection().snapshot().slots().get(0);
        assertEquals(-1, revealTick.spriteVariant());
        assertEquals(before.x(), revealTick.x());
        assertEquals(before.y(), revealTick.y());
        assertTrue(
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x50, 0x50, false, true, true, 0x58, 1, 0x58, 1))
                        .isEmpty());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, 0, sequence(0x00)));
        RoomEntity visible = runtime.inspection().snapshot().slots().get(0);
        assertTrue(visible.spriteVariant() >= 0);
        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x50, 0x50, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
    }

    @Test
    void visibleGiantUsesTheExistingLethalCombatStatusPath() throws IOException {
        byte[] rom = romWithSwordResult(0x11, 0x08);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(ghiniEntity(catalog, 0, 0x11, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, 0x01, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x90, 0x90, 0, sequence(0x00)));
        List<EntityCombatEvent> events = runtime.resolveCombat(new EntityVerticalContactFrame(
                0, 0x90, 0x90, false, false, true, 0x58, 1, 0x58, 1, EnemyAttackContext.standard()));

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void swordHitAtLinksExactPositionRecoilsUpAndLeftThroughTheLiveRuntime() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x20)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x20, 2))
                        .build()))
                .build();
        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 64, 64, false, false, true, 72, 1, 72, 1));
        assertEquals(1, events.size());
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 64, sequence(0)));
        assertEquals(61, runtime.inspection().snapshot().slots().getFirst().x());
        assertEquals(61, runtime.inspection().snapshot().slots().getFirst().y());
    }

    @Test
    void bankSixNormalEnemiesConfigureTheRomSharedSwordRecoil() {
        int[] types = {0x19, 0x0D, 0x15, 0x1A, 0x16, 0x17, 0x1B, 0x1C};
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 3);
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                            .sourceLoadOrder(0)
                            .type(type)
                            .at(64, 64)
                            .spriteDefinition(definition)
                            .build()))
                    .build();

            List<EntityCombatEvent> events =
                    runtime.resolveCombat(new EntitySwordContactFrame(0, 72, 72, false, true, true, 72, 1, 72, 1));

            assertEquals(1, events.size(), "type=" + Integer.toHexString(type));
            assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0), "type=" + Integer.toHexString(type));
            assertTrue(runtime.enemies().enemyRecoilActive(0), "type=" + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedX(0), "type=" + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedY(0), "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void antiFairyReversesTheMovingAxisAtARomBackgroundCollisionPoint() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x15)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x02)));

        RoomEntityBackgroundCollision leftWall = (entity, direction, nextX, nextY) -> direction == 1;
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00), leftWall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x0C, runtime.enemies().antiFairySpeedX(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
    }
}
