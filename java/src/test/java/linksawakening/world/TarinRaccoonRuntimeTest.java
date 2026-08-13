package linksawakening.world;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TarinRaccoonRuntimeTest {
    private static final int ROOM_MYSTERIOUS_WOODS = 0x51;
    private static final int ENTITY_TARIN = 0x3F;

    @Test
    void room51LoadsTheOutdoorRaccoonWithItsRomSprite() {
        RoomSession session = newSession();

        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);

        RoomEntity raccoon = raccoon(session);
        assertEquals(0x78, raccoon.x());
        assertEquals(0x40, raccoon.y());
        assertTrue(raccoon.spriteDefinition().supported());
        assertEquals(0x05, raccoon.spriteDefinition().bank());
        assertEquals(0x4912, raccoon.spriteDefinition().address());
    }

    @Test
    void roomStatusEventOneUnloadsTheOutdoorRaccoonOnItsFirstAndLaterLiveTicks() {
        RoomSession completed = newSession();
        byte[] completedOverworld = new byte[0x100];
        completedOverworld[ROOM_MYSTERIOUS_WOODS] = 0x10;
        completed.restoreRoomStatuses(completedOverworld, new byte[0x100],
            new byte[0x100], new byte[0x20]);

        completed.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);

        assertTrue(hasRaccoon(completed));
        completed.tickEntities(0, 0x50, 0x60, 0, 0);
        assertFalse(hasRaccoon(completed));

        RoomSession live = newSession();
        live.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        live.tickEntities(0, 0x50, 0x60, 0, 0);
        assertTrue(hasRaccoon(live));

        live.restoreRoomStatuses(completedOverworld, new byte[0x100],
            new byte[0x100], new byte[0x20]);
        live.tickEntities(1, 0x50, 0x60, 0, 0);

        assertFalse(hasRaccoon(live));
    }

    @Test
    void normalEntityPassWithoutQualifyingTarinClearsTheLostState() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x60, 0, 1);
        session.tickEntities(1, 0x78, 0x1F, 0, 1);
        assertTrue(session.shouldGetLostInMysteriousWoods());

        session.loadOverworld(0x52);
        session.tickEntities(2, 0x78, 0x1F, 0, 1);

        assertFalse(session.shouldGetLostInMysteriousWoods());
    }

    @Test
    void crossingTheNorthThresholdSetsLostStateAndQueuesDialog021Once() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.setChestPlayerLevels(1, 1, 0);
        session.tickEntities(0, 0x50, 0x60, 0, 0);

        session.tickEntities(8, 0x78, 0x1F, 0, 0);

        assertTrue(session.shouldGetLostInMysteriousWoods());
        assertEquals(3, raccoon(session).spriteVariant());
        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x21)),
            session.consumeEntityDialogRequests());

        session.tickEntities(9, 0x78, 0x1E, 0, 0);

        assertTrue(session.shouldGetLostInMysteriousWoods());
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.tickEntities(0x10, 0x78, 0x30, 0, 0);

        assertFalse(session.shouldGetLostInMysteriousWoods());
        assertEquals(1, raccoon(session).spriteVariant());
    }

    @Test
    void nearbyFacingActionQueuesDialog00dOnlyWhenDialogAndAttackGatesAreClear() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(true, false);
        session.setEntityAttackStepAnimationCountdown(1);

        session.tickEntities(1, 0x78, 0x50, 0, 1);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityAttackStepAnimationCountdown(0);
        session.setEntityDialogActive(true);
        session.tickEntities(2, 0x78, 0x50, 0, 1);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityDialogActive(false);
        session.tickEntities(3, 0x78, 0x50, 0, 1);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x0D)),
            session.consumeEntityDialogRequests());
    }

    @Test
    void bOnlyDoesNotQueueDialog00d() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(false, true);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void airborneLinkDoesNotQueueDialog00d() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(true, false);

        session.tickEntities(1, 0x78, 0x50, 1, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void appearingInventoryDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.setEntityTalkState(true, 0, 0x80);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void appearingInventoryDefersDialog021WithoutSuppressingTheLostFlag() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x60, 0, 1);
        session.setEntityTalkState(true, 0, 0x78);

        session.tickEntities(1, 0x78, 0x1F, 0, 1);

        assertTrue(session.shouldGetLostInMysteriousWoods());
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityTalkState(false, 0, 0x80);
        session.tickEntities(2, 0x78, 0x1E, 0, 1);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x21)),
            session.consumeEntityDialogRequests());
    }

    @Test
    void dialogCooldownDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.setEntityTalkState(false, 1, 0x80);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void nonGameplayWindowPositionDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.setEntityTalkState(false, 0, 0x70);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    private static RoomSession actionReadySession() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(true, false);
        return session;
    }

    private static RoomEntity raccoon(RoomSession session) {
        return session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == ENTITY_TARIN)
            .findFirst()
            .orElseThrow();
    }

    private static boolean hasRaccoon(RoomSession session) {
        return session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == ENTITY_TARIN);
    }

    private static RoomSession newSession() {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        return new RoomSession(rom, new GPU(), new RoomLoader(rom),
            new OverworldTilesetTable(rom), new OverworldCollision(romTables),
            new TransientVfxSystem(16), null);
    }

    private static byte[] loadRom() {
        try (var stream = TarinRaccoonRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
