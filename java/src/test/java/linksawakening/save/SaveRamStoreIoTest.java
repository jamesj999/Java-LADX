package linksawakening.save;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SaveRamStoreIoTest {
    @TempDir
    Path directory;

    @Test
    void failedReplacementKeepsDestinationAndRemovesTemporaryImage() throws Exception {
        Path path = directory.resolve("game.sav");
        var store = SaveRamStore.open(path);
        Files.createDirectory(path);
        Files.writeString(path.resolve("keep"), "original");
        assertThrows(IOException.class, store::flush);
        assertEquals("original", Files.readString(path.resolve("keep")));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void parentCreationFailureDoesNotOverwriteExistingFile() throws Exception {
        Path parent = directory.resolve("parent");
        var store = SaveRamStore.open(parent.resolve("game.sav"));
        Files.writeString(parent, "original");
        assertThrows(IOException.class, store::flush);
        assertEquals("original", Files.readString(parent));
    }

    @Test
    void onlyMissingImagesStartEmptyWhileReadFailuresPropagate() throws Exception {
        Path path = directory.resolve("missing.sav");
        assertThrows(
                java.nio.file.AccessDeniedException.class,
                () -> SaveRamStore.open(path, p -> {
                    throw new java.nio.file.AccessDeniedException(p.toString());
                }));
        assertEquals(
                0,
                SaveRamStore.open(path, p -> {
                            throw new java.nio.file.NoSuchFileException(p.toString());
                        })
                        .saveFilesMask());
    }
}
