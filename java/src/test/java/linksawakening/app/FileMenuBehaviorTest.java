package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.*;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.render.RenderScreen;
import linksawakening.save.SaveRamStore;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the real headless file/menu and opening-story boundaries. */
@RequiresRom
class FileMenuBehaviorTest {
    static final class Scenario implements AutoCloseable {
        final GameSession game = new GameSession((pcm, rate) -> {});
        final SessionContext c;

        Scenario() {
            try (var rom = linksawakening.testing.TestRom.open()) {
                assertTrue(rom.read() >= 0);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            game.initializeHeadlessNewGame();
            c = game.context;
        }

        void store(SaveRamStore store) {
            c.saveState.saveRamStore = store;
        }

        void press(String key) {
            game.headlessButton(key, true);
            game.stepWithoutRender();
            game.headlessButton(key, false);
            game.stepWithoutRender();
        }

        void frames(int count) {
            for (int i = 0; i < count; i++) game.stepWithoutRender();
        }

        public void close() {
            game.cleanup();
        }
    }

    @Test
    void committingAFilePersistsThenReturnsToSelection(@TempDir Path dir) throws Exception {
        try (var s = new Scenario()) {
            var path = dir.resolve("game.sav");
            s.store(SaveRamStore.open(path));
            s.c.router().startFileSelection();
            s.press("a");
            s.press("a");
            s.press("start");
            assertEquals(RenderScreen.FILE_MENU, s.c.currentScreen().renderScreen());
            assertEquals(1, SaveRamStore.open(path).saveFilesMask());
            assertFalse(s.c.screens.fileMenuController.snapshot().creation());
            s.press("a");
            assertEquals(RenderScreen.OVERWORLD, s.c.currentScreen().renderScreen());
        }
    }

    @Test
    void newGameResetsPersistentWorldStateBeforeHouseEntitiesAreLoaded() {
        try (var s = new Scenario()) {
            s.c.roomSession().story().setTarinFlag(1);
            s.c.playerState().setShieldLevel(1);
            s.c.playerState().setSwordLevel(1);
            s.c.saves().startNewGame();
            assertEquals(0, s.c.roomSession().story().tarinFlag());
            assertEquals(0, s.c.playerState().shieldLevel());
            assertEquals(0, s.c.playerState().swordLevel());
            assertEquals(0xA3, s.c.roomSession().rooms().currentRoomId());
            assertTrue(s.c.roomSession().rooms().activeRoom().entities().loadedEntities().stream()
                    .anyMatch(e -> e.type() == 0x3F));
            s.game.stepWithoutRender();
            assertTrue(s.c.link().isMarinWakeUpBedVisible());
        }
    }

    @Test
    void savedAndNewGamePositionsUseRomOamCoordinates() {
        try (var s = new Scenario()) {
            assertEquals(0x50, s.c.link().roomEntryRomPositionX());
            assertEquals(0x60, s.c.link().roomEntryRomPositionY());
            var store = SaveRamStore.inMemory();
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            store.writeSpawnLocation(0, 1, 0x10, 0xA3, 0x48, 0x68, 0);
            s.c.saves().startSavedGame(store.readSlot(0));
            assertEquals(0x48, s.c.link().roomEntryRomPositionX());
            assertEquals(0x68, s.c.link().roomEntryRomPositionY());
            assertEquals(0x40, s.c.link().pixelX());
            assertEquals(0x58, s.c.link().pixelY());
        }
    }

    @Test
    void newGameWakeOpensTheRomBackedMarinDialog() {
        try (var s = new Scenario()) {
            s.frames(520);
            assertTrue(s.c.dialogController().isActive());
            for (int i = 0; i < 200 && !s.c.dialogController().isWaitingForPageAdvance(); i++)
                s.game.stepWithoutRender();
            String expected = s.c.dialogTextLoader().load(new SignpostDialogRef(0, 1));
            assertFalse(s.c.dialogController().visibleText().isBlank());
            assertTrue(
                    expected.replaceAll("\\s+", "")
                            .contains(s.c.dialogController().visibleText().replaceAll("\\s+", "")),
                    s.c.dialogController().visibleText());
            assertTrue(s.c.link().isMarinWakeUpBedVisible());
        }
    }

    @Test
    void wakeConsumesMenuAndSaveKeysBeforeThoseActions() {
        try (var s = new Scenario()) {
            s.press("start");
            s.press("start+a+b+select");
            assertEquals(RenderScreen.OVERWORLD, s.c.currentScreen().renderScreen());
            assertFalse(s.c.inventoryMenu().isTransitioning());
            assertTrue(s.c.story().marinWakeUpBlocksKeyInput());
        }
    }

    @Test
    void heldDirectionReleasesTheBedAndFollowUpUsesOnlyA() {
        try (var s = new Scenario()) {
            s.frames(520);
            for (int i = 0; i < 600 && s.c.dialogController().isActive(); i++) s.press("a");
            s.c.inputState().tickEdges();
            s.game.headlessButton("right", true);
            s.c.story().tickNewGameWakeUp();
            s.game.headlessButton("right", false);
            assertTrue(s.c.newGameWakeUpMotion().complete());
            s.frames(64);
            assertFalse(s.c.link().isMarinWakeUpBedVisible());
            assertFalse(s.c.story().tryOpenMarinFollowUpDialog(s.c.inputConfig().bKey()));
            var marin = s.c.roomSession().rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(e -> e.type() == 0x3E)
                    .findFirst()
                    .orElseThrow();
            s.c.link().setPixelPosition(marin.x() - 8, marin.y() - 8);
            s.c.link().setRomDirection(2);
            assertTrue(s.c.story().tryOpenMarinFollowUpDialog(s.c.inputConfig().aKey()));
            assertTrue(s.c.dialogController().isActive());
        }
    }

    @Test
    void unshieldedHouseSaveRestoresTarinSequenceAndTalkUsesOnlyA() {
        try (var s = new Scenario()) {
            var store = SaveRamStore.inMemory();
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            store.writeSpawnLocation(0, 1, 0x10, 0xA3, 0x48, 0x68, 0);
            var calls = new ArrayList<String>();
            s.c.story()
                    .restoreNewGameHouseRuntimeIfNeeded(
                            store.readSlot(0), new linksawakening.app.story.OpeningRestoration() {
                                public void restoreWake(linksawakening.startup.MarinWakeUpMotion m) {
                                    assertTrue(m.complete());
                                    calls.add("wake");
                                }

                                public void restoreShield(linksawakening.startup.TarinShieldMotion m) {
                                    calls.add("shield");
                                }

                                public void refreshPresentation() {
                                    calls.add("presentation");
                                }
                            });
            assertEquals(List.of("wake", "presentation", "shield"), calls);
            s.c.saves().startSavedGame(store.readSlot(0));
            assertNotNull(s.c.newGameTarinShieldMotion());
            assertFalse(s.c.story().tryOpenTarinShieldDialog(s.c.inputConfig().bKey()));
        }
    }

    @Test
    void fileMenuReadsPersistentMaskAndNames() {
        try (var s = new Scenario()) {
            var store = SaveRamStore.inMemory();
            int[] name = {1, 2, 3, 4, 5};
            store.createNewGame(0, name);
            s.store(store);
            s.c.router().startFileSelection();
            assertArrayEquals(name, s.c.screens.fileMenuController.snapshot().nameBytes());
            s.press("a");
            assertEquals(RenderScreen.OVERWORLD, s.c.currentScreen().renderScreen());
            s.c.router().startFileSelection();
            s.press("down");
            s.press("a");
            assertTrue(s.c.screens.fileMenuController.snapshot().creation(), "Second slot remains empty");
        }
    }

    @Test
    void copyAndEraseFlushAndRebuildMenu(@TempDir Path dir) throws Exception {
        try (var s = new Scenario()) {
            var path = dir.resolve("game.sav");
            var store = SaveRamStore.open(path);
            store.createNewGame(0, new int[] {1, 2, 0, 0, 0});
            s.store(store);
            s.c.router().startFileSelection();
            var original = s.c.screens.fileMenuController;
            s.c.saves().persistFileMenuMutation(() -> store.copySlot(0, 1));
            assertEquals(3, SaveRamStore.open(path).saveFilesMask());
            assertNotSame(original, s.c.screens.fileMenuController);
            original = s.c.screens.fileMenuController;
            s.c.saves().persistFileMenuMutation(() -> store.eraseSlot(0));
            assertEquals(2, SaveRamStore.open(path).saveFilesMask());
            assertNotSame(original, s.c.screens.fileMenuController);
            assertEquals(RenderScreen.FILE_MENU, s.c.currentScreen().renderScreen());
        }
    }

    @Test
    void saveWriterPersistsPlayerAndRoomProgress(@TempDir Path dir) throws Exception {
        try (var s = new Scenario()) {
            var path = dir.resolve("game.sav");
            var store = SaveRamStore.open(path);
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            s.store(store);
            s.c.screens.currentSaveSlot = 0;
            s.c.playerState().setRupees(237);
            s.c.playerState().setShieldLevel(1);
            s.c.roomSession().story().setTarinFlag(1);
            s.c.saves().saveCurrentPlayerState();
            var saved = SaveRamStore.open(path).readSlot(0);
            assertEquals(237, saved.rupees());
            assertEquals(1, saved.shieldLevel());
            assertEquals(1, saved.tarinFlag());
            assertArrayEquals(s.c.roomSession().dungeon().indoorARoomStatusSnapshot(), saved.indoorARoomStatus());
            assertArrayEquals(s.c.roomSession().dungeon().dungeonItemFlagsSnapshot(), saved.dungeonItemFlags());
            assertTrue(saved.spawnPositionX() != 0);
        }
    }

    @Test
    void savedFileRestoresProgressBeforeLoadingItsRoom() {
        try (var s = new Scenario()) {
            var store = SaveRamStore.inMemory();
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            store.writeSpawnLocation(0, 1, 0x10, 0xA3, 0x48, 0x68, 0);
            store.writeTarinFlag(0, 1);
            byte[] a = new byte[256];
            a[0xA3] = (byte) 0x80;
            store.writeRoomStatuses(0, new byte[256], a, new byte[256], new byte[32]);
            s.c.saves().startSavedGame(store.readSlot(0));
            assertEquals(0x10, s.c.roomSession().rooms().mapId());
            assertEquals(0xA3, s.c.roomSession().rooms().currentRoomId());
            assertEquals(1, s.c.roomSession().story().tarinFlag());
            assertArrayEquals(a, s.c.roomSession().dungeon().indoorARoomStatusSnapshot());
            assertEquals(0x48, s.c.link().roomEntryRomPositionX());
        }
    }
}
