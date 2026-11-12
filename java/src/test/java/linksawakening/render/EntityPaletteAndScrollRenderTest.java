package linksawakening.render;

import static linksawakening.render.EntityRenderTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.room.RoomRenderSnapshot;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityPaletteAndScrollRenderTest {
    @Test
    void keepsIncomingRoomEntitiesOutsideTheViewportWhenScrollingLeftOrUp() {
        GPU gpu = new GPU();
        int color = 0x123456;
        int[][] palettes = {{0, color, 0, 0}};
        writeSolidTile(gpu, 0x30, 1);
        EntitySpriteDefinition single = new EntitySpriteDefinition(
                0x02,
                0x00,
                0x4000,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x30, 0x00), null)));
        EntitySpriteSelection selection = new EntitySpriteSelection(
                EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes);
        RoomEntity entity = new RoomEntity(0, 0, 0x02, 24, 32, EntityStatus.ACTIVE, single, 0, 0, 0, 0, -1, false);

        for (int direction : new int[] {ScrollController.LEFT, ScrollController.UP}) {
            ScrollController scroll = new ScrollController();
            scroll.start(direction, 0, 0, null, direction == ScrollController.LEFT ? 160 : 144);
            scroll.tick(8);

            byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            new EntityRenderLayer(snapshot(selection, entity), palettes, scroll).render(new RenderContext(buffer, gpu));

            int probeX = direction == ScrollController.LEFT ? 12 : 20;
            int probeY = direction == ScrollController.LEFT ? 16 : 8;
            assertEquals(
                    0,
                    pixelColor(buffer, probeX, probeY),
                    "incoming room entity leaked into the viewport for direction " + direction);
        }
    }

    @Test
    void rendersPreviousRoomEntitiesWithTheExitingRoomOffset() {
        GPU gpu = new GPU();
        int previousColor = 0x123456;
        int[][] palettes = {{0, previousColor, 0, 0}};
        writeSolidTile(gpu, 0x30, 1);
        writeSolidTile(gpu, 0x31, 1);
        EntitySpriteDefinition single = new EntitySpriteDefinition(
                0x02,
                0x00,
                0x4000,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x30, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x02, 8, 16, EntityStatus.ACTIVE, single, 0, 0, 0, 0, -1, false);
        EntitySpriteSelection selection = new EntitySpriteSelection(
                linksawakening.world.entity.EntityRoomLoader.RoomTable.OVERWORLD,
                0,
                0,
                new int[] {0xFF, 0xFF, 0xFF, 0xFF},
                true,
                palettes);
        RoomEntitySnapshot previousEntities = snapshot(selection, entity);
        RoomEntitySnapshot currentEntities = snapshot(selection);
        RoomRenderSnapshot previousRoom =
                new RoomRenderSnapshot(new int[] {0}, new int[] {0}, palettes, previousEntities);
        ScrollController scroll = new ScrollController();
        scroll.start(ScrollController.LEFT, 0, 0, previousRoom, 160);
        scroll.tick(8);

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(currentEntities, palettes, scroll).render(new RenderContext(buffer, gpu));

        assertEquals(previousColor, pixelColor(buffer, 12, 0));
    }

    @Test
    void preservesPreviousRoomEntityTilesAfterDestinationSheetsOverwriteVram() {
        GPU gpu = new GPU();
        int previousColor = 0x123456;
        int currentColor = 0x654321;
        int[][] palettes = {{0, previousColor, currentColor, 0}};
        writeSolidTile(gpu, 0x40, 1);
        writeSolidTile(gpu, 0x41, 1);
        EntitySpriteTileSnapshot previousTiles = gpu.snapshotEntityTiles();
        writeSolidTile(gpu, 0x40, 2);
        writeSolidTile(gpu, 0x41, 2);
        EntitySpriteTileSnapshot currentTiles = gpu.snapshotEntityTiles();

        EntitySpriteDefinition single = new EntitySpriteDefinition(
                0x02,
                0x00,
                0x4000,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x40, 0x00), null)));
        RoomEntity previousEntity =
                new RoomEntity(0, 0, 0x02, 8, 16, EntityStatus.ACTIVE, single, 0, 0, 0, 0, -1, false);
        RoomEntity currentEntity =
                new RoomEntity(0, 0, 0x02, 152, 16, EntityStatus.ACTIVE, single, 0, 0, 0, 0, -1, false);
        EntitySpriteSelection selection = new EntitySpriteSelection(
                linksawakening.world.entity.EntityRoomLoader.RoomTable.OVERWORLD,
                0,
                0,
                new int[] {0xFF, 0xFF, 0xFF, 0xFF},
                true,
                palettes);
        RoomEntitySnapshot previousEntities = snapshot(selection, previousTiles, previousEntity);
        RoomEntitySnapshot currentEntities = snapshot(selection, currentTiles, currentEntity);
        RoomRenderSnapshot previousRoom =
                new RoomRenderSnapshot(new int[] {0}, new int[] {0}, palettes, previousEntities);
        ScrollController scroll = new ScrollController();
        scroll.start(ScrollController.LEFT, 0, 0, previousRoom, 160);
        scroll.tick(40);

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(currentEntities, palettes, scroll).render(new RenderContext(buffer, gpu));

        assertEquals(previousColor, pixelColor(buffer, 44, 0));
        assertEquals(currentColor, pixelColor(buffer, 28, 0));
    }

    @Test
    void rendersEveryRockAndArrowPairVariantWithItsRomPaletteAndColumns() {
        GPU gpu = new GPU();
        int[][] palettes = new int[8][4];
        for (int palette = 0; palette < palettes.length; palette++) {
            palettes[palette] =
                    new int[] {0, 0x100000 + palette * 0x0100, 0x200000 + palette * 0x0100, 0x300000 + palette * 0x0100
                    };
        }
        List<EntitySpriteDefinition.Variant> rockVariants = new ArrayList<>();
        for (int variant = 0; variant < 2; variant++) {
            int firstTile = 0x20 + variant * 4;
            writeSolidTile(gpu, firstTile, 1);
            writeSolidTile(gpu, firstTile + 1, 1);
            writeSolidTile(gpu, firstTile + 2, 1);
            writeSolidTile(gpu, firstTile + 3, 1);
            rockVariants.add(new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(firstTile, variant + 1),
                    new EntitySpriteDefinition.OamAttribute(firstTile + 2, variant + 2)));
        }
        EntitySpriteDefinition rock =
                new EntitySpriteDefinition(0x0A, 0x03, 0x6A1E, EntitySpriteDefinition.Shape.PAIR, 0, rockVariants);
        List<EntitySpriteDefinition.Variant> arrowVariants = new ArrayList<>();
        for (int variant = 0; variant < 4; variant++) {
            int firstTile = 0x30 + variant * 4;
            writeSolidTile(gpu, firstTile, 1);
            writeSolidTile(gpu, firstTile + 1, 1);
            writeSolidTile(gpu, firstTile + 2, 1);
            writeSolidTile(gpu, firstTile + 3, 1);
            arrowVariants.add(new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(firstTile, variant + 1),
                    new EntitySpriteDefinition.OamAttribute(firstTile + 2, variant + 2)));
        }
        EntitySpriteDefinition arrow =
                new EntitySpriteDefinition(0x0C, 0x03, 0x6BC6, EntitySpriteDefinition.Shape.PAIR, 0, arrowVariants);
        for (int variant = 0; variant < rock.variantCount(); variant++) {
            byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            RoomEntity entity =
                    new RoomEntity(0, -1, 0x0A, 24, 32, EntityStatus.ACTIVE, rock, variant, 0, 0, 0, -1, false);
            new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
                    .render(new RenderContext(buffer, gpu));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 16, 16));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 24, 16));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 16, 24));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 24, 24));
        }
        for (int variant = 0; variant < arrow.variantCount(); variant++) {
            byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            RoomEntity entity =
                    new RoomEntity(0, -1, 0x0C, 64, 32, EntityStatus.ACTIVE, arrow, variant, 0, 0, 0, -1, false);
            new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
                    .render(new RenderContext(buffer, gpu));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 56, 16));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 64, 16));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 56, 24));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 64, 24));
        }
    }

    @Test
    void appliesProjectileTileOffsetZAndEightBySixteenEntityFlips() {
        GPU gpu = new GPU();
        int firstTop = 0x40;
        int firstBottom = 0x41;
        int secondTop = 0x42;
        int secondBottom = 0x43;
        writeSolidTile(gpu, firstTop, 1);
        writeSolidTile(gpu, firstBottom, 2);
        writeSolidTile(gpu, secondTop, 3);
        writeSolidTile(gpu, secondBottom, 1);

        EntitySpriteDefinition arrow = new EntitySpriteDefinition(
                0x0C,
                0x03,
                0x6BC6,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0x22, 0x00))));
        int[][] palettes = {{0, 0x112233, 0x223344, 0x334455}};
        RoomEntity entity =
                new RoomEntity(0, -1, 0x0C, 40, 32, EntityStatus.ACTIVE, arrow, 0, 0x60, 0x20, 4, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        // The tile offset moves $20/$22 to $40/$42.  X-flip swaps the pair
        // columns; Y-flip swaps each 8x8 half of the 8x16 sprites.
        assertEquals(palettes[0][2], pixelColor(buffer, 40, 12));
        assertEquals(palettes[0][1], pixelColor(buffer, 40, 20));
        assertEquals(palettes[0][1], pixelColor(buffer, 32, 12));
        assertEquals(palettes[0][3], pixelColor(buffer, 32, 20));
    }
}
