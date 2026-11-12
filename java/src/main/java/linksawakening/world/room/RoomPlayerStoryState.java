package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

final class RoomPlayerStoryState {
    private final Dependencies dependencies;

    RoomPlayerStoryState(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        if ((shieldLevel & ~0xFF) != 0 || (swordLevel & ~0xFF) != 0 || (powerBraceletLevel & ~0xFF) != 0) {
            throw new IllegalArgumentException("Player upgrade levels must be unsigned bytes");
        }
        dependencies.followers().playerShieldLevel = shieldLevel;
        dependencies.followers().playerSwordLevel = swordLevel;
        dependencies.followers().playerBraceletLevel = powerBraceletLevel;
        dependencies.owner().get().configureHouseNpcStoryState();
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
            if (dependencies.location().activeRoom != null) {
                dependencies
                        .location()
                        .activeRoom
                        .replaceEntities(dependencies
                                .entityBridge()
                                .get()
                                .entityRuntime
                                .inspection()
                                .snapshot());
            }
        }
    }

    void setHouseNpcTradeItem(int tradeItem) {
        if ((tradeItem & ~0xFF) != 0) throw new IllegalArgumentException("Trade item must be a byte");
        dependencies.followers().houseNpcTradeItem = tradeItem;
        dependencies.owner().get().configureHouseNpcStoryState();
    }

    void configureHouseNpcStoryState() {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setHouseNpcStoryState(
                            dependencies.followers().tarinFlag,
                            dependencies.followers().houseNpcTradeItem,
                            Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[2]),
                            Byte.toUnsignedInt(dependencies.roomState().overworldRoomStatus[0xFD]));
        }
    }

    void setEntityDefaultMusicTrack(int trackId) {
        if ((trackId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Default music track must be an unsigned byte");
        }
        dependencies.followers().entityDefaultMusicTrack = trackId;
        dependencies.roomStoryState().get().updateWitchEnvironment();
    }

    void updateWitchEnvironment() {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setWitchEnvironment(
                            dependencies.indoorTorchPaletteEffect().effectAddress(),
                            dependencies.indoorTorchPaletteEffect().paletteDataFlags(),
                            dependencies.followers().entityDefaultMusicTrack);
        }
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomFollowerState followers,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            RoomLocationState location,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomStoryState> owner) {}
}
