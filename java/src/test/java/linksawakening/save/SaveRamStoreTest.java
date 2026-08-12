package linksawakening.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        store.flush();

        assertEquals(SaveRamLayout.IMAGE_SIZE, Files.size(savePath));
        SaveRamStore reloaded = SaveRamStore.open(savePath);
        assertEquals(1, reloaded.saveFilesMask());
        assertArrayEquals(new int[] {1, 2, 3, 4, 5}, reloaded.savedNames()[0]);
        assertEquals(0x02, reloaded.readSlot(0).ocarinaSongFlags());
        assertEquals(1, reloaded.readSlot(0).selectedSongIndex());
    }

    private static byte[] pattern(int length, int start) {
        byte[] values = new byte[length];
        for (int index = 0; index < values.length; index++) {
            values[index] = (byte) (start + index);
        }
        return values;
    }
}
