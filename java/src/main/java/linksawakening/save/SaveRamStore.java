package linksawakening.save;

import linksawakening.world.FollowingNpcState;

import linksawakening.state.PlayerState;

import java.io.IOException;
import java.nio.file.Files;
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

    /** Copies current save data into an independent store with no backing file. */
    public SaveRamStore transientCopy() {
        return new SaveRamStore(null, image());
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

    public void writeWorldMapRoom(int slot, int room) {
        image.writeWorldMapRoom(slot, room);
    }

    public void writeDeathCount(int slot, int deathCount) {
        image.writeDeathCount(slot, deathCount);
    }

    public void writeBowWowState(int slot, int state) {
        image.writeBowWowState(slot, state);
    }

    public void writeQuestFlags(int slot, int richardSpokenFlag, FollowingNpcState state) {
        image.writeQuestFlags(slot, richardSpokenFlag, state);
    }

    public void writeTarinFlag(int slot, int flag) {
        image.writeTarinFlag(slot, flag);
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
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, image.bytes());
    }
}
