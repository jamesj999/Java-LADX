package linksawakening.render;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;

/** Synthetic entity fixtures shared by rendering regression tests. */
@linksawakening.testing.RequiresRom
final class EntityRenderTestSupport {
    static EntitySpriteDefinition pairDefinition(
            EntitySpriteDefinition.OamAttribute first, EntitySpriteDefinition.OamAttribute second) {
        return new EntitySpriteDefinition(
                0x7A,
                0x06,
                0x5C89,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(first, second)));
    }

    static EntitySpriteDefinition rectangleDefinition(int address, int tile, int attributes) {
        List<EntitySpriteDefinition.RectangleSprite> rectangle = List.of(new EntitySpriteDefinition.RectangleSprite(
                0, 0, new EntitySpriteDefinition.OamAttribute(tile, attributes)));
        return new EntitySpriteDefinition(
                0x00,
                0x03,
                address,
                EntitySpriteDefinition.Shape.RECTANGLE,
                0,
                List.of(),
                List.of(rectangle, rectangle, rectangle, rectangle));
    }

    static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        return snapshot(null, entities);
    }

    static RoomEntitySnapshot snapshot(EntitySpriteSelection selection, RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>();
        slots.addAll(List.of(entities));
        while (slots.size() < 0x10) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots, selection);
    }

    static RoomEntitySnapshot snapshot(
            EntitySpriteSelection selection, EntitySpriteTileSnapshot tiles, RoomEntity... entities) {
        return snapshot(selection, entities).withSpriteTiles(tiles);
    }

    static byte[] loadRom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }

    static byte[] filledBuffer(int red, int green, int blue) {
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        for (int i = 0; i < buffer.length; i += 4) {
            buffer[i] = (byte) red;
            buffer[i + 1] = (byte) green;
            buffer[i + 2] = (byte) blue;
            buffer[i + 3] = (byte) 0xFF;
        }
        return buffer;
    }

    static void writeSolidTile(GPU gpu, int tileIndex, int colorIndex) {
        int low = (colorIndex & 0x01) != 0 ? 0xFF : 0x00;
        int high = (colorIndex & 0x02) != 0 ? 0xFF : 0x00;
        int base = tileIndex * GPU.TILE_DATA_SIZE;
        for (int y = 0; y < 8; y++) {
            gpu.writeVRAM(base + y * 2, (byte) low);
            gpu.writeVRAM(base + y * 2 + 1, (byte) high);
        }
    }

    static void writePatternTile(GPU gpu, int tileIndex, int[][] pixels) {
        int base = tileIndex * GPU.TILE_DATA_SIZE;
        for (int y = 0; y < 8; y++) {
            int low = 0;
            int high = 0;
            for (int x = 0; x < 8; x++) {
                int color = pixels[y][x];
                low |= (color & 0x01) << (7 - x);
                high |= ((color >>> 1) & 0x01) << (7 - x);
            }
            gpu.writeVRAM(base + y * 2, (byte) low);
            gpu.writeVRAM(base + y * 2 + 1, (byte) high);
        }
    }

    static int pixelColor(byte[] buffer, int x, int y) {
        int index = (y * Framebuffer.WIDTH + x) * 4;
        return (Byte.toUnsignedInt(buffer[index]) << 16)
                | (Byte.toUnsignedInt(buffer[index + 1]) << 8)
                | Byte.toUnsignedInt(buffer[index + 2]);
    }
}
