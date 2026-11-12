package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage5 {
    static void run(GameplayRouteState route) throws Exception {

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x24, route.session.rooms().currentRoomId());
        assertEquals(
                5,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7E)
                        .count());
        assertTrue(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x23, route.session.rooms().currentRoomId());
        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x24, route.session.rooms().currentRoomId());
        assertFalse(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7E));
        assertTrue(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));

        route.bottleGrottoEntrance = route.session.rooms().activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x01 && warp.destRoom() == 0x36)
                .findFirst()
                .orElseThrow();
        assertEquals(0x13, route.bottleGrottoEntrance.tileLocation());
        route.entrancePath = reachableWarpPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), route.bottleGrottoEntrance.tileLocation());
        assertNotNull(
                route.entrancePath,
                "No collision-valid path from room $24's west entry " + "to Bottle Grotto warp $13\n"
                        + collisionGrid(route.collision));
        for (int[] position : route.entrancePath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        assertEquals(0x13, Warp.packTileLocation(route.link.pixelX(), route.link.pixelY()));
        route.link.setPixelPosition(0x30, 0x10); // align both probes with the entrance lane
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }

        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x01, route.session.rooms().activeRoom().mapId());
        assertEquals(0x36, route.session.rooms().currentRoomId());
        assertEquals(0x48, route.link.pixelX());
        assertEquals(0x6C, route.link.pixelY());
        assertEquals(0x3A, route.session.rooms().indoorRoomPositionForSave());
        assertEquals(0x01, route.session.story().bowWowState());
        assertFalse(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x61)
                        .count());
        assertEquals(
                4,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2D)
                        .count());
        route.bottleGrottoExit = route.session.rooms().activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x00 && warp.destRoom() == 0x24)
                .findFirst()
                .orElseThrow();
        assertEquals(0x38, route.bottleGrottoExit.destX());
        assertEquals(0x22, route.bottleGrottoExit.destY());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x31, route.session.rooms().currentRoomId());
        assertEquals(0x25, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0xAB, objectAt(route.session, 0x34));
        assertEquals(0xAB, objectAt(route.session, 0x35));
        assertEquals(0x08, route.session.rooms().activeRoom().shutterDoorMask() & 0x08);
        route.firstTorchApproach =
                reachablePowderApproach(route.collision, route.link.pixelX(), route.link.pixelY(), 0x34);
        assertNotNull(route.firstTorchApproach, collisionGrid(route.collision));
        for (int[] position : route.firstTorchApproach.path()) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        assertTrue(route.session
                .playerItems()
                .sprinkleMagicPowder(
                        route.link.romEntityX(), route.link.romEntityY(), 0, route.firstTorchApproach.romDirection()));
        assertTrue(route.playerState.consumeMagicPowder());
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        route.secondTorchApproach =
                reachablePowderApproach(route.collision, route.link.pixelX(), route.link.pixelY(), 0x35);
        assertNotNull(route.secondTorchApproach, collisionGrid(route.collision));
        for (int[] position : route.secondTorchApproach.path()) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        assertTrue(route.session
                .playerItems()
                .sprinkleMagicPowder(
                        route.link.romEntityX(), route.link.romEntityY(), 0, route.secondTorchApproach.romDirection()));
        assertTrue(route.playerState.consumeMagicPowder());
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(2, route.session.components.roomSessionViews().roomTriggerCountForTest());
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoom().shutterDoorMask() & 0x08);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x32, route.session.rooms().currentRoomId());
        assertEquals(0x81, route.session.components.roomSessionViews().activeRoomEventForTest());
        route.bottleStalfosSlots = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1A || entity.type() == 0x1E)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.bottleStalfosSlots.size());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.romEntityX(),
                        route.link.romEntityY(),
                        0,
                        0,
                        Link.DIRECTION_RIGHT,
                        false);
        for (int stalfosSlot : route.bottleStalfosSlots) {
            for (int hit = 0;
                    hit < 8
                            && route.session
                                            .rooms()
                                            .activeRoom()
                                            .entities()
                                            .slots()
                                            .get(stalfosSlot)
                                            .status()
                                    == EntityStatus.ACTIVE;
                    hit++) {
                RoomEntity stalfos =
                        route.session.rooms().activeRoom().entities().slots().get(stalfosSlot);
                List<int[]> combatPath =
                        reachableEntityContactPath(route.collision, route.link.pixelX(), route.link.pixelY(), stalfos);
                assertNotNull(combatPath, collisionGrid(route.collision));
                for (int[] position : combatPath) {
                    route.link.setPixelPosition(position[0], position[1]);
                }
                route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                stalfosSlot ^ 1,
                                route.link.romEntityX(),
                                route.link.romEntityY(),
                                false,
                                0,
                                true,
                                new SwordBox(true, stalfos.x(), 0x08, stalfos.y(), 0x08),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                for (int recovery = 0; recovery < 0x20; recovery++) {
                    route.session
                            .entities()
                            .tickEntitiesWithProjectileEvents(
                                    route.frame++,
                                    route.link.romEntityX(),
                                    route.link.romEntityY(),
                                    0,
                                    0,
                                    Link.DIRECTION_RIGHT,
                                    false);
                }
            }
        }
        route.firstBottleKeyDeadline = route.frame + 0x300;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.firstBottleKeyDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++,
                            route.link.romEntityX(),
                            route.link.romEntityY(),
                            0,
                            0,
                            Link.DIRECTION_RIGHT,
                            false);
        }
        assertEquals(
                0,
                route.session.components.roomSessionViews().activeRoomEventForTest(),
                route.session.rooms().activeRoom().entities().loadedEntities().toString());
        route.firstBottleKey = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        route.bottleKeyLandingDeadline = route.frame + 0x200;
        while (route.firstBottleKey.z() != 0 && route.frame < route.bottleKeyLandingDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++,
                            route.link.romEntityX(),
                            route.link.romEntityY(),
                            0,
                            0,
                            Link.DIRECTION_DOWN,
                            false);
            route.firstBottleKey =
                    route.session.rooms().activeRoom().entities().slots().get(route.firstBottleKey.slot());
        }
        assertEquals(0, route.firstBottleKey.z());
        route.keyPath = reachableEntityContactPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), route.firstBottleKey);
        assertNotNull(route.keyPath, collisionGrid(route.collision));
        for (int[] position : route.keyPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.bottleKeyPickupFrame = (route.firstBottleKey.slot() & 0x01) == 0 ? route.frame | 1 : route.frame & ~1;
        assertNotNull(route.session
                .entities()
                .collectEntityIfNeeded(
                        route.bottleKeyPickupFrame,
                        route.link.romEntityX(),
                        route.link.romEntityY(),
                        false,
                        true,
                        Link.DIRECTION_DOWN,
                        0));
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(18, route.playerState.magicPowderCount());

        // Room $33 is the first post-key Bottle Grotto room.  Enter it through
        // the live collision/scroll path so its ROM-authored state is part of
        // the same ordered-play trace.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x33, route.session.rooms().currentRoomId());
        assertEquals(0x00, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());
        route.room33RaisedBlockLocation = 0x62;
        assertEquals(0xDC, objectAt(route.session, route.room33RaisedBlockLocation));
        assertTrue(
                route.session
                        .components
                        .roomSessionViews()
                        .linkCollisionPointBlockedForTest(
                                (route.room33RaisedBlockLocation & 0x0F) << 4, route.room33RaisedBlockLocation & 0xF0),
                "room $33 raised $DC must block before the switch animation at "
                        + String.format("%02X", route.room33RaisedBlockLocation));
        assertEquals(
                6,
                java.util.stream.IntStream.range(0, 6)
                        .filter(offset -> objectAt(route.session, 0x62 + offset) == 0xDC)
                        .count());
        assertEquals(0xC0, objectAt(route.session, 0x23));
        assertEquals(0xC0, objectAt(route.session, 0x26));
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x66)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x42)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x16)
                        .count());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());

        route.crystalSwitch = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.switchPath = reachableEntityContactPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), route.crystalSwitch);
        assertNotNull(route.switchPath, collisionGrid(route.collision));
        for (int[] position : route.switchPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.liveSword = new Sword(route.romTables, null);
        route.liveSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.liveSword.tick(false);
        }
        route.link.setDirection(Link.DIRECTION_RIGHT);
        route.initialSwordBox = route.liveSword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        route.link.setPixelPosition(
                route.crystalSwitch.x() - (route.initialSwordBox.x() - route.link.pixelX()),
                route.crystalSwitch.y() - (route.initialSwordBox.y() - route.link.pixelY()));
        route.swordBox = route.liveSword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        assertTrue(route.swordBox.active());
        assertTrue(
                linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                        route.crystalSwitch,
                        route.swordBox.x(),
                        route.swordBox.width(),
                        route.swordBox.y(),
                        route.swordBox.height()),
                "sword=" + route.swordBox + " crystal=" + route.crystalSwitch);
        route.switchHit = route.session
                .entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        route.frame++,
                        route.link.romEntityX(),
                        route.link.romEntityY(),
                        false,
                        0,
                        true,
                        new SwordBox(
                                true,
                                route.swordBox.x(),
                                route.swordBox.width(),
                                route.swordBox.y(),
                                route.swordBox.height()),
                        linksawakening.world.entity.EnemyAttackContext.standard()));
        assertEquals(EntityStatus.ACTIVE, route.crystalSwitch.status());
        assertTrue(
                route.switchHit.stream()
                        .anyMatch(event ->
                                event.slot() == route.crystalSwitch.slot() && event.type() == 0x66 && event.swordHit()),
                route.switchHit.toString());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        assertEquals(0x01, route.session.components.roomSessionViews().switchableObjectAnimationStageForTest());
        assertTrue(route.session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x66
                        && event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                        && event.soundId() == 0x0E));

        assertTrue(route.session.components.roomSessionViews().linkCollisionPointBlockedForTest(0x20, 0x60));
        for (int stage = 0; stage < 9; stage++) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x02, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());
        assertEquals(0x00, route.session.components.roomSessionViews().switchableObjectAnimationStageForTest());
        assertFalse(
                route.session
                        .components
                        .roomSessionViews()
                        .linkCollisionPointBlockedForTest(
                                (route.room33RaisedBlockLocation & 0x0F) << 4, route.room33RaisedBlockLocation & 0xF0),
                "room $33 raised $DC must pass after the switch animation");

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x34, route.session.rooms().currentRoomId());
        assertEquals(0x81, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(
                2,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x8F)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2E)
                        .count());
        assertEquals(0x02, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        assertEquals(
                2,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x8F)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2E)
                        .count());

        route.maskedMimicSlots = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x8F)
                .map(RoomEntity::slot)
                .toList();
        route.defeatedMaskedMimics = 0;
        for (int maskedMimicSlot : route.maskedMimicSlots) {
            for (int hit = 0; hit < 2; hit++) {
                RoomEntity maskedMimic =
                        route.session.rooms().activeRoom().entities().slots().get(maskedMimicSlot);
                assertEquals(EntityStatus.ACTIVE, maskedMimic.status());
                int linkEntityX = maskedMimic.x();
                int linkEntityY = (maskedMimic.y() + 0x20) & 0xFF;
                // The first handler tick mirrors Link's down input. The next
                // sees Link behind that facing and selects vulnerable options $08.
                route.session.components.entityInputs().setEntityPressedButtonsMask(0x08);
                tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
                tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
                maskedMimic =
                        route.session.rooms().activeRoom().entities().slots().get(maskedMimicSlot);
                List<EntityCombatEvent> hitEvents = route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                route.frame++,
                                linkEntityX,
                                linkEntityY,
                                false,
                                0,
                                true,
                                new SwordBox(true, maskedMimic.x() + 0x08, 0x08, maskedMimic.y() + 0x08, 0x08),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                assertTrue(
                        hitEvents.stream()
                                .anyMatch(event -> event.slot() == maskedMimicSlot
                                        && event.swordHit()
                                        && event.enemyDamage() == 1),
                        hitEvents.toString());
                route.session.components.entityInputs().setEntityPressedButtonsMask(0);
                for (int recovery = 0; recovery < 0x30; recovery++) {
                    route.session
                            .entities()
                            .tickEntitiesWithProjectileEvents(
                                    route.frame++,
                                    route.link.romEntityX(),
                                    route.link.romEntityY(),
                                    0,
                                    0,
                                    route.link.direction(),
                                    false);
                }
            }
            int mimicDeathDeadline = route.frame + 0x200;
            while (route.session
                                    .rooms()
                                    .activeRoom()
                                    .entities()
                                    .slots()
                                    .get(maskedMimicSlot)
                                    .status()
                            != EntityStatus.DISABLED
                    && route.frame < mimicDeathDeadline) {
                tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            }
            assertEquals(
                    EntityStatus.DISABLED,
                    route.session
                            .rooms()
                            .activeRoom()
                            .entities()
                            .slots()
                            .get(maskedMimicSlot)
                            .status());
            route.defeatedMaskedMimics++;
            if (route.defeatedMaskedMimics == 1) {
                assertEquals(0x81, route.session.components.roomSessionViews().activeRoomEventForTest());
                assertEquals(
                        0,
                        route.session.rooms().activeRoom().entities().loadedEntities().stream()
                                .filter(entity -> entity.type() == 0x30)
                                .count());
            }
        }
        route.session.components.entityInputs().setEntityPressedButtonsMask(0);
        route.mimicEventDeadline = route.frame + 0x300;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.mimicEventDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertTrue(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x2E));
        route.secondBottleKey = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        route.secondKeyLandingDeadline = route.frame + 0x200;
    }
}
