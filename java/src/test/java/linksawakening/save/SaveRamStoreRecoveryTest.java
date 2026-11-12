package linksawakening.save;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SaveRamStoreRecoveryTest {
    @TempDir
    Path directory;

    @Test
    void preservesWrongSizedImageBeforeReplacingIt() throws Exception {
        Path path = directory.resolve("game.sav");
        byte[] original = {1, 2, 3, 4};
        Files.write(path, original);
        var store = SaveRamStore.open(path);
        Path backup = store.corruptBackup().orElseThrow();
        assertTrue(backup.getFileName().toString().startsWith("game.sav.corrupt-"));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertArrayEquals(original, Files.readAllBytes(backup));
        store.flush();
        assertEquals(SaveRamLayout.IMAGE_SIZE, Files.size(path));
        assertArrayEquals(original, Files.readAllBytes(backup));
        try (var files = Files.list(directory)) {
            assertEquals(2, files.count());
        }
    }

    @Test
    void validImageNeedsNoBackupAndFlushesWholeImage() throws Exception {
        Path path = directory.resolve("nested/game.sav");
        var store = SaveRamStore.open(path);
        store.createNewGame(0, new int[] {1, 2, 3, 4, 5});
        store.flush();
        assertArrayEquals(store.image().bytes(), Files.readAllBytes(path));
        assertTrue(SaveRamStore.open(path).corruptBackup().isEmpty());
    }
}
