package linksawakening.render;

import java.util.ArrayList;
import java.util.List;
import linksawakening.dialog.DialogController;
import linksawakening.entity.*;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.*;
import linksawakening.input.*;
import linksawakening.rom.*;
import linksawakening.state.PlayerState;
import linksawakening.ui.*;
import linksawakening.vfx.*;
import linksawakening.world.entity.*;
import linksawakening.world.room.*;
import linksawakening.world.transition.*;

/** Synthetic room with actual HUD, Link, ten entities, three VFX slots and an open dialog. */
final class SceneConstructionFixture {
    final GPU gpu = new GPU();
    final byte[] output = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
    final GameFrameState state;
    private final ReusableGameFrameState template;

    SceneConstructionFixture() {
        this(false);
    }

    SceneConstructionFixture(boolean highEntityTypes) {
        byte[] bytes = new byte[0x100000];
        int linkTiles = RomSymbols.LinkCharacter2Tiles.withGbcBank().offset();
        for (int i = 0; i < 4096; i += 2) bytes[linkTiles + i] = (byte) 255;
        Rom rom = Rom.of(bytes);
        for (int tile = 0; tile < GPU.TILE_COUNT; tile++)
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++) gpu.getTile(tile).setPixel(x, y, (tile + x + y) % 3 + 1);
        int[][] palettes = new int[8][4];
        for (int p = 0; p < 8; p++) for (int c = 0; c < 4; c++) palettes[p][c] = p * 0x10101 + c * 0x30303;
        var pair = new EntitySpriteDefinition(
                0x35,
                6,
                0x4000,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x40, 0),
                        new EntitySpriteDefinition.OamAttribute(0x42, 1))));
        var rectangle = new EntitySpriteDefinition(
                0x36,
                6,
                0x4000,
                EntitySpriteDefinition.Shape.RECTANGLE,
                0,
                List.of(),
                List.of(List.of(
                        new EntitySpriteDefinition.RectangleSprite(
                                0, 0, new EntitySpriteDefinition.OamAttribute(0x44, 1)),
                        new EntitySpriteDefinition.RectangleSprite(
                                0, 8, new EntitySpriteDefinition.OamAttribute(0x46, 2)),
                        new EntitySpriteDefinition.RectangleSprite(
                                16, 0, new EntitySpriteDefinition.OamAttribute(0x48, 3)))));
        var entities = new ArrayList<RoomEntity>();
        for (int i = 0; i < 10; i++)
            entities.add(new RoomEntity(
                    i,
                    i,
                    (highEntityTypes ? 0x90 : 0x35) + i,
                    16 + i * 12,
                    36 + (i % 2) * 8,
                    EntityStatus.ACTIVE,
                    i == 5 ? rectangle : pair,
                    0,
                    0,
                    0,
                    0,
                    -1,
                    false));
        while (entities.size() < 16) entities.add(RoomEntity.disabled(entities.size()));
        var selection =
                new EntitySpriteSelection(EntityRoomLoader.RoomTable.OVERWORLD, 0x92, 0, new int[4], true, palettes);
        var roomEntities = new RoomEntitySnapshot(entities, selection, gpu.snapshotEntityTiles());
        var room = new RoomRenderSnapshot(
                new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT],
                new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT],
                palettes,
                roomEntities);
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var player = new PlayerState();
        var link = new Link(input, config, null, null, LinkSpriteSheet.loadFromRom(rom), player, new ItemRegistry());
        link.position().setPixelPosition(40, 40);
        var inventory = new InventoryController(
                input, config, new InventoryMenu(InventoryTilemapLoader.loadFromRom(rom), player));
        var vfx = new TransientVfxSystem(3);
        vfx.spawn(TransientVfxType.BUSH_LEAVES, 60, 32);
        vfx.spawn(TransientVfxType.WATER_SPLASH, 90, 32);
        vfx.spawn(TransientVfxType.POOF, 120, 32);
        var dialog = new DialogController(16);
        dialog.open("A", DialogController.BoxPosition.BOTTOM);
        for (int i = 0; i < 80; i++) dialog.tick();
        state = GameFrameState.empty()
                .withScreen(RenderScreen.OVERWORLD)
                .withRoom(room, new ScrollController(), new TransitionController())
                .withLink(link)
                .withInventoryController(inventory)
                .withDialogController(dialog)
                .withTransientVfx(
                        vfx, new CutLeavesEffectRenderer(TransientVfxSpriteSheet.loadFromRom(rom)), palettes[0]);
        template = new ReusableGameFrameState().copyFrom(state);
    }

    ReusableGameFrameState refill(ReusableGameFrameState target, int frame) {
        var room = template.roomInputs;
        return target.reset()
                .withScreen(RenderScreen.OVERWORLD)
                .withRoomData(room.tiles, room.attributes, room.palettes, room.entities, null, null, room.grassPalette)
                .withRoomControllers(state.scrollController(), state.transitionController())
                .withLink(state.link())
                .withInventoryController(state.inventoryController())
                .withDialogController(state.dialogController())
                .withTransientVfx(
                        state.transientVfxSystem(), state.cutLeavesEffectRenderer(), state.transientVfxPalette())
                .withFrameCounter(frame);
    }

    void configure(GameFrameSceneBuilder builder) {
        builder.setDialogWindowPainter(SceneConstructionFixture::syntheticWindow);
    }

    private static void syntheticWindow(HardwareFrame frame, DialogController dialog, int[] palette) {
        // Engine-authored checker glyph and window: no original font/resource is read.
        int boxY = dialog.boxY();
        for (int y = 0; y < 40; y++)
            for (int x = 0; x < 144; x++)
                if (dialog.isBoxRowVisible(y / 8)) frame.backgroundPixel(x + 8, y + boxY, 3, palette, -1, 1);
        if (!dialog.visibleText().isEmpty())
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++)
                    frame.backgroundPixel(16 + x, boxY + 8 + y, (x + y) % 2 == 0 ? 0 : 3, palette, -1, 1);
    }
}
