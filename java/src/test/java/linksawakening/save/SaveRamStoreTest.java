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

        assertEquals(1 << 2, store.saveFilesMask());
        assertArrayEquals(new int[] {9, 8, 7, 6, 5}, store.savedNames()[2]);
        assertEquals(0x04, store.readSlot(2).ocarinaSongFlags());
        assertEquals(0, store.readSlot(2).selectedSongIndex());
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
}
