package linksawakening.ui;

import java.util.Objects;
import linksawakening.gpu.GPU;
import linksawakening.render.HardwareFrame;
import linksawakening.rom.Rom;

/** Inventory ROM graphics capabilities, or an explicit overlay-only presentation. */
public final class InventoryGraphics {
    private final Instruments instruments;
    private final InventoryStatusRenderer status;
    private final InventoryQuestItems quest;
    private final Dungeon dungeon;
    private final TileTransfers transfers;

    private InventoryGraphics(Instruments instruments, Dungeon dungeon, TileTransfers transfers, Rom rom) {
        status = rom == null ? null : new InventoryStatusRenderer(rom);
        quest = rom == null ? null : new InventoryQuestItems(rom);
        this.instruments = Objects.requireNonNull(instruments, "instruments");
        this.dungeon = Objects.requireNonNull(dungeon, "dungeon");
        this.transfers = Objects.requireNonNull(transfers, "transfers");
    }

    public static InventoryGraphics fromRom(Rom rom) {
        Objects.requireNonNull(rom, "rom");
        return new InventoryGraphics(
                new InventoryInstrumentPanel(rom), new InventoryDungeonPanel(rom), new RomTransfers(rom), rom);
    }

    /** Draws the base inventory overlay without ROM panels or VRAM transfers. */
    public static InventoryGraphics overlayOnly() {
        return new InventoryGraphics(Empty.INSTANCE, Empty.INSTANCE, Empty.INSTANCE, null);
    }

    InventoryStatusRenderer status() {
        return status;
    }

    InventoryQuestItems quest() {
        return quest;
    }

    Instruments instruments() {
        return instruments;
    }

    Dungeon dungeon() {
        return dungeon;
    }

    TileTransfers transfers() {
        return transfers;
    }

    interface Instruments {
        default int[][] framePalettes(int[][] base, int frame) {
            return base;
        }

        default void render(byte[] buffer, int[] attributes, int[][] palettes, byte[] flags, int windowY) {}

        default void submit(HardwareFrame frame, int[] attributes, int[][] palettes, byte[] flags, int windowY) {}

        default void renderNavigation(byte[] buffer, int[][] palettes, int frame) {}

        default void submitNavigation(HardwareFrame model, int[][] palettes, int frame) {}
    }

    interface Dungeon {
        default void apply(int[] map, int[] attributes, int mapId, boolean collapsed, byte[] items, byte[] rooms) {}

        default void render(
                byte[] buffer,
                int[] map,
                int[] attributes,
                int[][] palettes,
                int offsetY,
                InventoryDungeonPanel.Presentation presentation) {}

        default void submit(
                HardwareFrame model,
                int[] map,
                int[] attributes,
                int[][] palettes,
                int offsetY,
                InventoryDungeonPanel.Presentation presentation) {}
    }

    interface TileTransfers {
        default void powder(GPU gpu, boolean hasToadstool) {}

        default void ocarina(GPU gpu) {}

        default void sharedEffects(GPU gpu) {}
    }

    private enum Empty implements Instruments, Dungeon, TileTransfers {
        INSTANCE
    }

    private record RomTransfers(Rom rom) implements TileTransfers {
        @Override
        public void powder(GPU gpu, boolean hasToadstool) {
            gpu.updatePowderInventoryTiles(rom, hasToadstool);
        }

        @Override
        public void ocarina(GPU gpu) {
            gpu.loadOcarinaSymbolsTiles(rom);
        }

        @Override
        public void sharedEffects(GPU gpu) {
            gpu.loadSharedVfxTiles(rom);
        }
    }
}
