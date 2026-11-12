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
            c.runtime().saves().saveRamStore = store;
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
            s.c.runtime().flows().router().startFileSelection();
            s.press("a");
            s.press("a");
            s.press("start");
            assertEquals(
                    RenderScreen.FILE_MENU, s.c.state().screens().currentScreen.renderScreen());
            assertEquals(1, SaveRamStore.open(path).saveFilesMask());
            assertFalse(s.c.state().screens().fileMenuController.snapshot().creation());
            s.press("a");
            assertEquals(
                    RenderScreen.OVERWORLD, s.c.state().screens().currentScreen.renderScreen());
        }
    }

    @Test
    void newGameResetsPersistentWorldStateBeforeHouseEntitiesAreLoaded() {
        try (var s = new Scenario()) {
            s.c.services().world().roomSession().story().setTarinFlag(1);
            s.c.services().player().playerState().equipmentLevels().setShieldLevel(1);
            s.c.services().player().playerState().equipmentLevels().setSwordLevel(1);
            s.c.runtime().flows().saves().startNewGame();
            assertEquals(0, s.c.services().world().roomSession().story().tarinFlag());
            assertEquals(
                    0, s.c.services().player().playerState().equipmentLevels().shieldLevel());
            assertEquals(
                    0, s.c.services().player().playerState().equipmentLevels().swordLevel());
            assertEquals(0xA3, s.c.services().world().roomSession().rooms().currentRoomId());
            assertTrue(s.c.services().world().roomSession().rooms().activeRoom().entities().loadedEntities().stream()
                    .anyMatch(e -> e.type() == 0x3F));
            s.game.stepWithoutRender();
            assertTrue(s.c.services().player().link().animation().isMarinWakeUpBedVisible());
        }
    }

    @Test
    void savedAndNewGamePositionsUseRomOamCoordinates() {
        try (var s = new Scenario()) {
            assertEquals(0x50, s.c.services().player().link().position().roomEntryRomPositionX());
            assertEquals(0x60, s.c.services().player().link().position().roomEntryRomPositionY());
            var store = SaveRamStore.inMemory();
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            store.writeSpawnLocation(0, 1, 0x10, 0xA3, 0x48, 0x68, 0);
            s.c.runtime().flows().saves().startSavedGame(store.readSlot(0));
            assertEquals(0x48, s.c.services().player().link().position().roomEntryRomPositionX());
            assertEquals(0x68, s.c.services().player().link().position().roomEntryRomPositionY());
            assertEquals(0x40, s.c.services().player().link().position().pixelX());
            assertEquals(0x58, s.c.services().player().link().position().pixelY());
        }
    }

    @Test
    void newGameWakeOpensTheRomBackedMarinDialog() {
        try (var s = new Scenario()) {
            s.frames(520);
            assertTrue(s.c.services().ui().dialogController().isActive());
            for (int i = 0; i < 200 && !s.c.services().ui().dialogController().isWaitingForPageAdvance(); i++)
                s.game.stepWithoutRender();
            String expected = s.c.services().ui().dialogTextLoader().load(new SignpostDialogRef(0, 1));
            assertFalse(s.c.services().ui().dialogController().visibleText().isBlank());
            assertTrue(
                    expected.replaceAll("\\s+", "")
                            .contains(s.c.services()
                                    .ui()
                                    .dialogController()
                                    .visibleText()
                                    .replaceAll("\\s+", "")),
                    s.c.services().ui().dialogController().visibleText());
            assertTrue(s.c.services().player().link().animation().isMarinWakeUpBedVisible());
        }
    }

    @Test
    void wakeConsumesMenuAndSaveKeysBeforeThoseActions() {
        try (var s = new Scenario()) {
            s.press("start");
            s.press("start+a+b+select");
            assertEquals(
                    RenderScreen.OVERWORLD, s.c.state().screens().currentScreen.renderScreen());
            assertFalse(s.c.services().ui().inventoryMenu().isTransitioning());
            assertTrue(s.c.runtime().flows().interactions().story().marinWakeUpBlocksKeyInput());
        }
    }

    @Test
    void heldDirectionReleasesTheBedAndFollowUpUsesOnlyA() {
        try (var s = new Scenario()) {
            s.frames(520);
            for (int i = 0; i < 600 && s.c.services().ui().dialogController().isActive(); i++) s.press("a");
            s.c.services().input().inputState().tickEdges();
            s.game.headlessButton("right", true);
            s.c.runtime().flows().interactions().story().tickNewGameWakeUp();
            s.game.headlessButton("right", false);
            assertTrue(s.c.state().opening().wakeUp().complete());
            s.frames(64);
            assertFalse(s.c.services().player().link().animation().isMarinWakeUpBedVisible());
            assertFalse(s.c.runtime()
                    .flows()
                    .interactions()
                    .story()
                    .tryOpenMarinFollowUpDialog(
                            s.c.services().input().inputConfig().bKey()));
            var marin = s.c.services().world().roomSession().rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(e -> e.type() == 0x3E)
                    .findFirst()
                    .orElseThrow();
            s.c.services().player().link().position().setPixelPosition(marin.x() - 8, marin.y() - 8);
            s.c.services().player().link().animation().setRomDirection(2);
            assertTrue(s.c.runtime()
                    .flows()
                    .interactions()
                    .story()
                    .tryOpenMarinFollowUpDialog(
                            s.c.services().input().inputConfig().aKey()));
            assertTrue(s.c.services().ui().dialogController().isActive());
        }
    }

    @Test
    void unshieldedHouseSaveRestoresTarinSequenceAndTalkUsesOnlyA() {
        try (var s = new Scenario()) {
            var store = SaveRamStore.inMemory();
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            store.writeSpawnLocation(0, 1, 0x10, 0xA3, 0x48, 0x68, 0);
            var calls = new ArrayList<String>();
            s.c.runtime()
                    .flows()
                    .interactions()
                    .story()
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
            s.c.runtime().flows().saves().startSavedGame(store.readSlot(0));
            assertNotNull(s.c.state().opening().shield());
            assertFalse(s.c.runtime()
                    .flows()
                    .interactions()
                    .story()
                    .tryOpenTarinShieldDialog(
                            s.c.services().input().inputConfig().bKey()));
        }
    }

    @Test
    void fileMenuReadsPersistentMaskAndNames() {
        try (var s = new Scenario()) {
            var store = SaveRamStore.inMemory();
            int[] name = {1, 2, 3, 4, 5};
            store.createNewGame(0, name);
            s.store(store);
            s.c.runtime().flows().router().startFileSelection();
            assertArrayEquals(
                    name, s.c.state().screens().fileMenuController.snapshot().nameBytes());
            s.press("a");
            assertEquals(
                    RenderScreen.OVERWORLD, s.c.state().screens().currentScreen.renderScreen());
            s.c.runtime().flows().router().startFileSelection();
            s.press("down");
            s.press("a");
            assertTrue(s.c.state().screens().fileMenuController.snapshot().creation(), "Second slot remains empty");
        }
    }

    @Test
    void copyAndEraseFlushAndRebuildMenu(@TempDir Path dir) throws Exception {
        try (var s = new Scenario()) {
            var path = dir.resolve("game.sav");
            var store = SaveRamStore.open(path);
            store.createNewGame(0, new int[] {1, 2, 0, 0, 0});
            s.store(store);
            s.c.runtime().flows().router().startFileSelection();
            var original = s.c.state().screens().fileMenuController;
            s.c.runtime().flows().saves().persistFileMenuMutation(() -> store.copySlot(0, 1));
            assertEquals(3, SaveRamStore.open(path).saveFilesMask());
            assertNotSame(original, s.c.state().screens().fileMenuController);
            original = s.c.state().screens().fileMenuController;
            s.c.runtime().flows().saves().persistFileMenuMutation(() -> store.eraseSlot(0));
            assertEquals(2, SaveRamStore.open(path).saveFilesMask());
            assertNotSame(original, s.c.state().screens().fileMenuController);
            assertEquals(
                    RenderScreen.FILE_MENU, s.c.state().screens().currentScreen.renderScreen());
        }
    }

    @Test
    void saveWriterPersistsPlayerAndRoomProgress(@TempDir Path dir) throws Exception {
        try (var s = new Scenario()) {
            var path = dir.resolve("game.sav");
            var store = SaveRamStore.open(path);
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            s.store(store);
            s.c.state().screens().currentSaveSlot = 0;
            s.c.services().player().playerState().wallet().setRupees(237);
            s.c.services().player().playerState().equipmentLevels().setShieldLevel(1);
            s.c.services().world().roomSession().story().setTarinFlag(1);
            s.c.runtime().flows().saves().saveCurrentPlayerState();
            var saved = SaveRamStore.open(path).readSlot(0);
            assertEquals(237, saved.rupees());
            assertEquals(1, saved.shieldLevel());
            assertEquals(1, saved.tarinFlag());
            assertArrayEquals(
                    s.c.services().world().roomSession().dungeon().indoorARoomStatusSnapshot(),
                    saved.indoorARoomStatus());
            assertArrayEquals(
                    s.c.services().world().roomSession().dungeon().dungeonItemFlagsSnapshot(),
                    saved.dungeonItemFlags());
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
            s.c.runtime().flows().saves().startSavedGame(store.readSlot(0));
            assertEquals(0x10, s.c.services().world().roomSession().rooms().mapId());
            assertEquals(0xA3, s.c.services().world().roomSession().rooms().currentRoomId());
            assertEquals(1, s.c.services().world().roomSession().story().tarinFlag());
            assertArrayEquals(a, s.c.services().world().roomSession().dungeon().indoorARoomStatusSnapshot());
            assertEquals(0x48, s.c.services().player().link().position().roomEntryRomPositionX());
        }
    }
}
