package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BeachOpeningRuntimeTest {
    private static final int BEACH_ROOM = 0xF2;
    private static final int ENTITY_SWORD_SHIELD_PICKUP = 0x31;
    private static final int ENTITY_SLEEPY_TOADSTOOL = 0x3A;
    private static final int ENTITY_OWL_EVENT = 0x41;

    @Test
    void beachRoomLoadsSwordAndOwlUntilTheirRoomEventsAreSet() throws IOException {
        byte[] rom = loadRom();
        EntityRoomLoader loader = new EntityRoomLoader(rom);
        byte[] statuses = new byte[0x100];

        RoomEntitySnapshot fresh = loader.load(
            EntityRoomLoader.RoomTable.OVERWORLD, BEACH_ROOM, 0, -1, statuses);
        assertTrue(hasType(fresh, ENTITY_SWORD_SHIELD_PICKUP));
        assertTrue(hasType(fresh, ENTITY_OWL_EVENT));

        statuses[BEACH_ROOM] = 0x10;
        RoomEntitySnapshot swordCollected = loader.load(
            EntityRoomLoader.RoomTable.OVERWORLD, BEACH_ROOM, 0, -1, statuses);
        assertFalse(hasType(swordCollected, ENTITY_SWORD_SHIELD_PICKUP));
        assertTrue(hasType(swordCollected, ENTITY_OWL_EVENT));

        statuses[BEACH_ROOM] = 0x30;
        RoomEntitySnapshot openingComplete = loader.load(
            EntityRoomLoader.RoomTable.OVERWORLD, BEACH_ROOM, 0, -1, statuses);
        assertFalse(hasType(openingComplete, ENTITY_SWORD_SHIELD_PICKUP));
        assertFalse(hasType(openingComplete, ENTITY_OWL_EVENT));
    }

    @Test
    void swordPickupUsesTheRomFanfareSequenceBeforeGrantingTheSword() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 1, ENTITY_SWORD_SHIELD_PICKUP, 0x58, 0x60, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SWORD_SHIELD_PICKUP,
                EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.setEntityRoomId(BEACH_ROOM);
        runtime.setEntityRoomStatus(0);
        runtime.setChestPlayerLevels(0, 0, 0);

        runtime.tick(0, 0x20, 0x20, () -> 0);
        RoomEntity sword = runtime.snapshot().slots().get(0);
        assertEquals(0x84, sword.spriteDefinition().variant(0).first().tile());

        EntityPickupEvent pickup = runtime.collectIfNeeded(
            1, sword.x(), sword.y(), false, true, 3, 0);
        assertNotNull(pickup);
        assertEquals(0, pickup.persistentClearMask());
        runtime.tick(2, 0x58, 0x60, () -> 0);
        assertEquals(0x0F, runtime.consumePendingMusicTrack());
        assertTrue(runtime.consumePendingSwordPickupRewards().isEmpty());

        boolean foundSwordDialog = false;
        boolean foundOverworldIntro = false;
        boolean foundNormalOverworld = false;
        boolean foundSpinNoise = false;
        boolean foundSpinPose = false;
        List<RoomEntityRuntime.SwordPickupRewardEvent> rewards = List.of();
        for (int frame = 3; frame < 0x240 && rewards.isEmpty(); frame++) {
            runtime.tick(frame, 0x58, 0x60, () -> 0);
            foundSwordDialog |= runtime.consumePendingDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x09B);
            int musicTrack = runtime.consumePendingMusicTrack();
            foundOverworldIntro |= musicTrack == 0x31;
            foundNormalOverworld |= musicTrack == 0x05;
            foundSpinNoise |= runtime.consumePendingEntityEvents().stream()
                .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                    && event.soundId() == 0x03);
            foundSpinPose |= !runtime.consumePendingLinkSwordSpinPoseRequests().isEmpty();
            rewards = runtime.consumePendingSwordPickupRewards();
        }

        assertTrue(foundSwordDialog);
        assertTrue(foundOverworldIntro);
        assertTrue(foundNormalOverworld);
        assertTrue(foundSpinNoise, "rewards=" + rewards + ", status="
            + runtime.snapshot().slots().get(0).status() + ", state="
            + runtime.swordPickupStateForTest(0) + ", slow="
            + runtime.slowTransitionCountdownForTest(0));
        assertTrue(foundSpinPose);
        assertEquals(List.of(new RoomEntityRuntime.SwordPickupRewardEvent(0)), rewards);
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void sleepyToadstoolUsesTheRomHeldItemSequenceBeforeGrantingItsReward()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_SLEEPY_TOADSTOOL, 0x58, 0x60, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SLEEPY_TOADSTOOL,
                EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.setEntityRoomId(0x50);
        runtime.setToadstoolPlayerState(false, 0);
        runtime.setOwlDefaultMusicResolver(roomId -> 0x09);

        EntityPickupEvent pickup = runtime.collectIfNeeded(
            1, 0x58, 0x60, false, true, 3, 0);
        assertNotNull(pickup);
        assertEquals(0x01, pickup.persistentClearMask());
        assertEquals(0x10, runtime.consumePendingMusicTrack());

        boolean heldAboveLink = false;
        boolean linkBlocked = false;
        boolean dialogOpened = false;
        List<RoomEntityRuntime.ToadstoolRewardEvent> rewards = List.of();
        for (int frame = 2; frame < 0x100 && rewards.isEmpty(); frame++) {
            runtime.tick(frame, 0x48, 0x50, () -> 0);
            heldAboveLink |= !runtime.consumePendingLinkHeldItemPoseRequests().isEmpty();
            linkBlocked |= !runtime.consumePendingLinkMotionBlockRequests().isEmpty();
            dialogOpened |= runtime.consumePendingDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x00F);
            rewards = runtime.consumePendingToadstoolRewards();
        }

        assertTrue(heldAboveLink);
        assertTrue(linkBlocked);
        assertTrue(dialogOpened);
        assertEquals(List.of(new RoomEntityRuntime.ToadstoolRewardEvent(0)), rewards);
        assertEquals(0x09, runtime.consumePendingMusicTrack());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void sleepyToadstoolUnloadsWhenAlreadyOwnedOrPowderIsPresent() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);

        RoomEntityRuntime owned = toadstoolRuntime(catalog, rom);
        owned.setToadstoolPlayerState(true, 0);
        owned.tick(0, 0, 0, () -> 0);
        assertEquals(EntityStatus.DISABLED, owned.snapshot().slots().get(0).status());

        RoomEntityRuntime powdered = toadstoolRuntime(catalog, rom);
        powdered.setToadstoolPlayerState(false, 1);
        powdered.tick(0, 0, 0, () -> 0);
        assertEquals(EntityStatus.DISABLED, powdered.snapshot().slots().get(0).status());
    }

    @Test
    void beachOwlTriggersInTheRomRectangleAndCompletesItsDialogEvent() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_OWL_EVENT, 0x08, 0x10, EntityStatus.ACTIVE,
            catalog.forOwlEvent(false), 0)), false, () -> 0, catalog,
            new RomEnemyCombatTables(rom));
        runtime.setEntityRoomId(BEACH_ROOM);
        runtime.setEntityRoomStatus(0);

        runtime.tick(0, 0x3F, 0x44, () -> 0);
        assertEquals(0, runtime.owlEventStateForTest(0));
        assertEquals(-1, runtime.consumePendingMusicTrack());

        runtime.tick(1, 0x40, 0x44, () -> 0);
        assertEquals(1, runtime.owlEventStateForTest(0));
        assertEquals(0x22, runtime.consumePendingMusicTrack());

        boolean dialogOpened = false;
        boolean linkBlocked = false;
        int frame = 2;
        while (!dialogOpened && frame < 0x300) {
            runtime.tick(frame++, 0x58, 0x60, () -> 0);
            dialogOpened |= runtime.consumePendingDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0D9);
            linkBlocked |= !runtime.consumePendingLinkMotionBlockRequests().isEmpty();
            assertTrue(runtime.consumePendingOwlEventCompletions().isEmpty());
        }

        assertTrue(dialogOpened);
        assertTrue(linkBlocked);
        for (int countdown = 0x0F; countdown > 0; countdown--) {
            runtime.tick(frame++, 0x58, 0x60, () -> 0);
            assertTrue(runtime.consumePendingOwlEventCompletions().isEmpty());
        }
        runtime.tick(frame, 0x58, 0x60, () -> 0);
        assertEquals(List.of(new RoomEntityRuntime.OwlEventCompletion(0)),
            runtime.consumePendingOwlEventCompletions());
    }

    @Test
    void forestOwlRequiresTheSwordAndStartsItsFlightIntoTheRoom() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime swordless = owlRuntime(catalog, rom);
        swordless.setEntityRoomId(0x80);
        swordless.setChestPlayerLevels(0, 0, 0);

        swordless.tick(0, 0x50, 0x50, () -> 0);

        assertEquals(EntityStatus.DISABLED, swordless.snapshot().slots().get(0).status());
        assertEquals(-1, swordless.consumePendingMusicTrack());

        RoomEntityRuntime armed = owlRuntime(catalog, rom);
        armed.setEntityRoomId(0x80);
        armed.setChestPlayerLevels(0, 1, 0);
        armed.setOwlDialogResolver(new OwlEventDialogResolver(rom)::globalDialogId);

        armed.tick(0, 0x50, 0x50, () -> 0);
        assertEquals(1, armed.owlEventStateForTest(0));
        assertEquals(0x22, armed.consumePendingMusicTrack());
        armed.tick(1, 0x50, 0x50, () -> 0);
        assertEquals(List.of(new RoomEntityRuntime.LinkFacingRequest(0, 0)),
            armed.consumePendingLinkFacingRequests());
        assertTrue(armed.consumePendingEntityEvents().stream().anyMatch(event ->
            event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D));
        assertEquals(EntitySpriteDefinition.Shape.PAIR,
            armed.snapshot().slots().get(0).spriteDefinition().shape());
        armed.tick(2, 0x50, 0x50, () -> 0);
        assertFalse(armed.consumePendingEntityEvents().stream().anyMatch(event ->
            event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D));

        armed.tick(8, 0x50, 0x50, () -> 0);
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE,
            armed.snapshot().slots().get(0).spriteDefinition().shape());
    }

    private static RoomEntityRuntime owlRuntime(EntitySpriteHandlerCatalog catalog, byte[] rom) {
        return RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_OWL_EVENT, 0x58, 0x50, EntityStatus.ACTIVE,
            catalog.forOwlEvent(false), 0)), false, () -> 0, catalog,
            new RomEnemyCombatTables(rom));
    }

    private static RoomEntityRuntime toadstoolRuntime(EntitySpriteHandlerCatalog catalog,
                                                       byte[] rom) {
        return RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_SLEEPY_TOADSTOOL, 0x58, 0x60, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SLEEPY_TOADSTOOL,
                EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
    }

    private static boolean hasType(RoomEntitySnapshot snapshot, int type) {
        return snapshot.loadedEntities().stream().anyMatch(entity -> entity.type() == type);
    }

    private static RoomEntitySnapshot snapshot(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = BeachOpeningRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
