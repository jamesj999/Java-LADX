package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.rom.RomBank;

@linksawakening.testing.RequiresRom
abstract class EntitySpriteHandlerCatalogFixture {

    protected static void assertDefinition(
            EntitySpriteDefinition definition,
            int bank,
            int address,
            EntitySpriteDefinition.Shape shape,
            int variants,
            int initialVariant) {
        assertEquals(bank, definition.bank());
        assertEquals(address, definition.address());
        assertEquals(shape, definition.shape());
        assertEquals(variants, definition.variantCount());
        assertEquals(initialVariant, definition.initialVariant());
    }

    protected static void assertPairBytes(EntitySpriteDefinition definition, int[][] expected) {
        assertEquals(expected.length, definition.variantCount());
        for (int variant = 0; variant < expected.length; variant++) {
            EntitySpriteDefinition.Variant actual = definition.variant(variant);
            assertEquals(expected[variant][0], actual.first().tile());
            assertEquals(expected[variant][1], actual.first().attributes());
            assertEquals(expected[variant][2], actual.second().tile());
            assertEquals(expected[variant][3], actual.second().attributes());
        }
    }

    protected static void assertPairPrefixBytes(EntitySpriteDefinition definition, int[][] expected) {
        for (int variant = 0; variant < expected.length; variant++) {
            EntitySpriteDefinition.Variant actual = definition.variant(variant);
            assertEquals(expected[variant][0], actual.first().tile());
            assertEquals(expected[variant][1], actual.first().attributes());
            assertEquals(expected[variant][2], actual.second().tile());
            assertEquals(expected[variant][3], actual.second().attributes());
        }
    }

    protected static void assertRectangleBytes(EntitySpriteDefinition definition, int[][][] expected) {
        assertEquals(expected.length, definition.variantCount());
        for (int variant = 0; variant < expected.length; variant++) {
            assertEquals(
                    expected[variant].length,
                    definition.rectangleVariant(variant).size());
            for (int sprite = 0; sprite < expected[variant].length; sprite++) {
                EntitySpriteDefinition.RectangleSprite actual =
                        definition.rectangleVariant(variant).get(sprite);
                assertEquals(expected[variant][sprite][0], actual.yOffset());
                assertEquals(expected[variant][sprite][1], actual.xOffset());
                assertEquals(expected[variant][sprite][2], actual.oam().tile());
                assertEquals(expected[variant][sprite][3], actual.oam().attributes());
            }
        }
    }

    protected static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x1A, 0x4000)];
    }

    protected static void write(byte[] rom, int bank, int address, int... values) {
        int offset = RomBank.romOffset(bank, address);
        for (int value : values) {
            rom[offset++] = (byte) value;
        }
    }

    protected static byte[] loadRom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
