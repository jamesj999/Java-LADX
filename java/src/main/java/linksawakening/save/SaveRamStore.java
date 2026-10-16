package linksawakening.save;

import linksawakening.state.PlayerState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.Objects;

/** Host persistence adapter for the source-shaped SRAM image. */
public final class SaveRamStore {

    private final Path path;
    private final SaveRamImage image;

    private SaveRamStore(Path path, SaveRamImage image) {
        this.path = path;
        this.image = image;
    }

    public static SaveRamStore open(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        SaveRamImage image;
        if (!Files.exists(path)) {
            image = SaveRamImage.empty();
        } else {
            byte[] bytes = Files.readAllBytes(path);
            image = bytes.length == SaveRamLayout.IMAGE_SIZE
                ? SaveRamImage.fromBytes(bytes)
                : SaveRamImage.empty();
            image.initializeInvalidSlots();
        }
        return new SaveRamStore(path, image);
    }

    public static SaveRamStore inMemory() {
        return new SaveRamStore(null, SaveRamImage.empty());
    }

    public static Path defaultPath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            throw new IllegalStateException("Java user.home is not configured");
        }
        return Path.of(userHome, ".linksawakening", "azle.sav");
    }

    public SaveRamImage image() {
        return SaveRamImage.fromBytes(image.bytes());
    }

    public int saveFilesMask() {
        return image.saveFilesMask();
    }

    public int[][] savedNames() {
        return image.savedNames();
    }

    public SaveSlotState readSlot(int slot) {
        return image.readSlot(slot);
    }

    public void createNewGame(int slot, int[] nameBytes) {
        image.createNewGame(slot, nameBytes);
    }

    public void copySlot(int sourceSlot, int targetSlot) {
        image.copySlot(sourceSlot, targetSlot);
    }

    public void eraseSlot(int slot) {
        image.eraseSlot(slot);
    }

    public void writeOcarinaState(int slot, int songFlags, int selectedSongIndex) {
        image.writeOcarinaState(slot, songFlags, selectedSongIndex);
    }

    public void writeRoomStatuses(int slot, byte[] overworldRoomStatus,
                                  byte[] indoorARoomStatus, byte[] indoorBRoomStatus,
                                  byte[] colorDungeonRoomStatus) {
        image.writeRoomStatuses(slot, overworldRoomStatus, indoorARoomStatus,
            indoorBRoomStatus, colorDungeonRoomStatus);
    }

    public void writeDungeonItemFlags(int slot, byte[] dungeonItemFlags,
                                      byte[] colorDungeonItemFlags) {
        image.writeDungeonItemFlags(slot, dungeonItemFlags, colorDungeonItemFlags);
    }

    public void writeDungeonProgressFlags(int slot, byte[] dungeonProgressFlags) {
        image.writeDungeonProgressFlags(slot, dungeonProgressFlags);
    }

    public void writeBowWowState(int slot, int state) {
        image.writeBowWowState(slot, state);
    }

    public void writeTarinFlag(int slot, int flag) {
        image.writeTarinFlag(slot, flag);
    }

    public void writeRichardSpokenFlag(int slot, int flag) {
        image.writeRichardSpokenFlag(slot, flag);
    }

    public void writeSpawnLocation(int slot, int isIndoor, int mapId, int mapRoom,
                                   int positionX, int positionY, int indoorRoom) {
        image.writeSpawnLocation(slot, isIndoor, mapId, mapRoom,
            positionX, positionY, indoorRoom);
    }

    public void writePlayerState(int slot, PlayerState playerState) {
        image.writePlayerState(slot, playerState);
    }

    public void flush() throws IOException {
        if (path == null) {
            return;
        }
        Path absolutePath = path.toAbsolutePath();
        // Retain the former Files.write behavior for save paths linked elsewhere.
        if (Files.isSymbolicLink(absolutePath)) {
            absolutePath = absolutePath.toRealPath();
        }
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        // Write a complete sibling image before replacing the old directory entry.
        // A failed write or move leaves the previous save intact.
        Path temporary = Files.createTempFile(parent, ".azle-save-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer data = ByteBuffer.wrap(image.bytes());
                while (data.hasRemaining()) {
                    channel.write(data);
                }
                channel.force(true);
            }
            Files.move(temporary, absolutePath, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
