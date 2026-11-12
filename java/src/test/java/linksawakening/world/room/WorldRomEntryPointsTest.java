package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Executable;
import java.util.stream.Stream;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.TransitionController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Keeps the app wiring migration source-compatible while its owners switch to Rom. */
class WorldRomEntryPointsTest {
    private record Entry(Class<?> owner, String method, Class<?>[] parameters) {
        Executable resolve(Class<?> romType) throws Exception {
            Class<?>[] signature = parameters.clone();
            signature[0] = romType;
            return method == null ? owner.getConstructor(signature) : owner.getMethod(method, signature);
        }

        public String toString() {
            return owner.getSimpleName();
        }
    }

    static Stream<Entry> entries() {
        return Stream.of(
                new Entry(TransitionController.class, "setRomData", new Class<?>[] {Rom.class}),
                new Entry(OverworldBushInteraction.class, null, new Class<?>[] {Rom.class, RomTables.class}),
                new Entry(RoomLoader.class, null, new Class<?>[] {Rom.class}),
                new Entry(OverworldTilesetTable.class, null, new Class<?>[] {Rom.class}),
                new Entry(RoomSessionConfig.class, null, new Class<?>[] {
                    Rom.class,
                    GPU.class,
                    RoomLoader.class,
                    OverworldTilesetTable.class,
                    OverworldCollision.class,
                    RoomSessionConfig.Presentation.class,
                    RomRandomByteSource.class
                }));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entries")
    void typedEntryAndDeprecatedByteDelegateRemainAvailable(Entry entry) throws Exception {
        assertNull(entry.resolve(Rom.class).getAnnotation(Deprecated.class));
        var legacy = entry.resolve(byte[].class).getAnnotation(Deprecated.class);
        assertNotNull(legacy);
        assertTrue(legacy.forRemoval());
    }

    @Test
    void typedWiringRetainsTheSuppliedPartialRomWithoutCopyingOrRealigning() {
        byte[] bytes = new byte[0x100001];
        bytes[RomSymbols.OverworldTilesetsTable.offset()] = 0x35;
        var rom = Rom.unchecked(bytes);
        var tables = RomTables.load(rom);
        var loader = new RoomLoader(rom);
        var tilesets = new OverworldTilesetTable(rom);
        var bushes = new OverworldBushInteraction(rom, tables);
        var transition = new TransitionController();
        transition.setRomData(rom);
        var config = new RoomSessionConfig(
                rom,
                new GPU(),
                loader,
                tilesets,
                new OverworldCollision(tables),
                new RoomSessionConfig.Presentation(null, null, room -> {}),
                new RomRandomByteSource());
        assertSame(rom, config.romData());
        assertSame(bytes, config.romData().unsafeBytes());
        assertSame(loader, config.roomLoader());
        assertSame(tilesets, config.overworldTilesetTable());
        assertEquals(0x35, tilesets.tilesetIdForRoom(0));
        assertEquals(-1, bushes.swordHitRoomObjectLocation(false, Direction.DOWN, 0, 0));
        assertFalse(transition.isActive());
    }
}
