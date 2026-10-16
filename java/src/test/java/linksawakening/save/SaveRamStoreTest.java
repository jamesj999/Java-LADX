package linksawakening.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SaveRamStoreTest {

    @Test
    void inMemoryStoreUsesTheSameRawImageApi() throws Exception {
        SaveRamStore store = SaveRamStore.inMemory();

        store.createNewGame(2, new int[] {9, 8, 7, 6, 5});
        store.writeOcarinaState(2, 0x04, 0);
        store.writeRoomStatuses(2, pattern(0x100, 0x10), pattern(0x100, 0x40),
            pattern(0x100, 0x70), pattern(0x20, 0xA0));
        store.writeDungeonItemFlags(2, pattern(0x2D, 0x20), pattern(0x05, 0xE0));
        store.writeDungeonProgressFlags(2, pattern(0x08, 0x01));
        store.writeBowWowState(2, 0x80);
        store.writeTarinFlag(2, 1);

        assertEquals(1 << 2, store.saveFilesMask());
        assertArrayEquals(new int[] {9, 8, 7, 6, 5}, store.savedNames()[2]);
        assertEquals(0x04, store.readSlot(2).ocarinaSongFlags());
        assertEquals(0, store.readSlot(2).selectedSongIndex());
        assertArrayEquals(pattern(0x100, 0x10), store.readSlot(2).overworldRoomStatus());
        assertArrayEquals(pattern(0x20, 0xA0), store.readSlot(2).colorDungeonRoomStatus());
        assertArrayEquals(pattern(0x2D, 0x20), store.readSlot(2).dungeonItemFlags());
        assertArrayEquals(pattern(0x05, 0xE0), store.readSlot(2).colorDungeonItemFlags());
        assertArrayEquals(pattern(0x08, 0x01), store.readSlot(2).dungeonProgressFlags());
        assertEquals(0x80, store.readSlot(2).bowWowState());
        assertEquals(1, store.readSlot(2).tarinFlag());
    }

    @Test
    void storeExposesCopyAndEraseMutations() {
        SaveRamStore store = SaveRamStore.inMemory();
        store.createNewGame(0, new int[] {9, 8, 7, 6, 5});

        store.copySlot(0, 1);
        store.eraseSlot(0);

        assertEquals(1 << 1, store.saveFilesMask());
        assertArrayEquals(new int[] {9, 8, 7, 6, 5}, store.savedNames()[1]);
    }

    @Test
    void fileMenuMutationsFlushAndReopenWithRebuiltMaskAndNames(@TempDir Path tempDir)
        throws Exception {
        Path savePath = tempDir.resolve("azle.sav");
        SaveRamStore store = SaveRamStore.open(savePath);
        store.createNewGame(0, new int[] {9, 8, 7, 6, 5});
        store.createNewGame(1, new int[] {1, 2, 3, 4, 5});
        store.flush();

        store.copySlot(0, 2);
        store.eraseSlot(0);
        store.flush();

        SaveRamStore reopened = SaveRamStore.open(savePath);
        assertEquals((1 << 1) | (1 << 2), reopened.saveFilesMask());
        assertArrayEquals(new int[SaveRamLayout.NAME_LENGTH], reopened.savedNames()[0]);
        assertArrayEquals(new int[] {1, 2, 3, 4, 5}, reopened.savedNames()[1]);
        assertArrayEquals(new int[] {9, 8, 7, 6, 5}, reopened.savedNames()[2]);
    }

    @Test
    void missingHostFileStartsWithAnInitializedEmptyImageAndFlushesExactBytes(@TempDir Path tempDir)
        throws Exception {
        Path savePath = tempDir.resolve("nested").resolve("azle.sav");
        SaveRamStore store = SaveRamStore.open(savePath);
        assertEquals(0, store.saveFilesMask());

        store.createNewGame(0, new int[] {1, 2, 3, 4, 5});
        store.writeOcarinaState(0, 0x02, 1);
        store.writeTarinFlag(0, 2);
        store.writeRichardSpokenFlag(0, 2);
        store.flush();

        assertEquals(SaveRamLayout.IMAGE_SIZE, Files.size(savePath));
        SaveRamStore reloaded = SaveRamStore.open(savePath);
        assertEquals(1, reloaded.saveFilesMask());
        assertArrayEquals(new int[] {1, 2, 3, 4, 5}, reloaded.savedNames()[0]);
        assertEquals(0x02, reloaded.readSlot(0).ocarinaSongFlags());
        assertEquals(1, reloaded.readSlot(0).selectedSongIndex());
        assertEquals(2, reloaded.readSlot(0).tarinFlag());
        assertEquals(2, reloaded.readSlot(0).richardSpokenFlag());
    }

    @Test
    void flushReplacesCompleteImageWithoutOverwritingExistingReaders(@TempDir Path tempDir)
        throws Exception {
        Path path = tempDir.resolve("azle.sav");
        SaveRamStore store = SaveRamStore.open(path);
        store.createNewGame(0, new int[] {1, 2, 3, 4, 5});
        store.flush();
        byte[] previous = Files.readAllBytes(path);
        Path oldImage = tempDir.resolve("previous.sav");
        Files.createLink(oldImage, path);
        store.eraseSlot(0);
        store.flush();
        assertArrayEquals(previous, Files.readAllBytes(oldImage));
        assertArrayEquals(store.image().bytes(), Files.readAllBytes(path));
        try (var files = Files.list(tempDir)) {
            assertEquals(2, files.count());
        }
    }

    @Test
    void failedReplacementKeepsDestinationAndRemovesTemporaryImage(@TempDir Path tempDir)
        throws Exception {
        Path path = tempDir.resolve("azle.sav");
        SaveRamStore store = SaveRamStore.open(path);
        Files.createDirectory(path);
        Path marker = path.resolve("keep.txt");
        Files.writeString(marker, "unchanged");
        assertThrows(java.io.IOException.class, store::flush);
        assertEquals("unchanged", Files.readString(marker));
        try (var files = Files.list(tempDir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void atomicFlushPreservesSymlinkAndUpdatesItsTarget(@TempDir Path tempDir)
        throws Exception {
        Path target = tempDir.resolve("real.sav");
        SaveRamStore original = SaveRamStore.open(target);
        original.flush();
        Path linked = tempDir.resolve("linked.sav");
        Files.createSymbolicLink(linked, target.getFileName());
        SaveRamStore store = SaveRamStore.open(linked);
        store.createNewGame(0, new int[] {1, 2, 3, 4, 5});
        store.flush();
        assertTrue(Files.isSymbolicLink(linked));
        assertArrayEquals(store.image().bytes(), Files.readAllBytes(target));
    }

    private static byte[] pattern(int length, int start) {
        byte[] values = new byte[length];
        for (int index = 0; index < values.length; index++) {
            values[index] = (byte) (start + index);
        }
        return values;
    }
}
