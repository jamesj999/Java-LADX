package linksawakening.entity;

import java.util.List;

/** ROM-decoded OAM display list and handler metadata for one entity type. */
public final class EntitySpriteDefinition {

    public enum Shape {
        PAIR,
        SINGLE,
        RECTANGLE,
        DYNAMIC,
        UNSUPPORTED
    }

    public record OamAttribute(int tile, int attributes) {
        public OamAttribute {
            if ((tile & ~0xFF) != 0 || (attributes & ~0xFF) != 0) {
                throw new IllegalArgumentException("OAM bytes must be unsigned bytes");
            }
        }
    }

    public record Variant(OamAttribute first, OamAttribute second) {
        public Variant {
            if (first == null) {
                throw new IllegalArgumentException("A display-list variant needs a first OAM entry");
            }
        }
    }

    /** One [Y offset, X offset, tile, attributes] tuple from a rectangle list. */
    public record RectangleSprite(int yOffset, int xOffset, OamAttribute oam) {
        public RectangleSprite {
            if (yOffset < -128 || yOffset > 127 || xOffset < -128 || xOffset > 127) {
                throw new IllegalArgumentException("Rectangle offsets must be signed bytes");
            }
            if (oam == null) {
                throw new IllegalArgumentException("Rectangle OAM entry cannot be null");
            }
        }
    }

    /** One handler-generated OAM entry with offsets relative to hActiveEntityPosX/Y. */
    public record DynamicSprite(int yOffset, int xOffset, OamAttribute oam,
                                TileSource tileSource,
                                boolean appliesEntityFlipAttribute) {
        public enum TileSource {
            ENTITY_SHEETS,
            GPU
        }

        public DynamicSprite {
            if (yOffset < -128 || yOffset > 127 || xOffset < -128 || xOffset > 127) {
                throw new IllegalArgumentException("Dynamic offsets must be signed bytes");
            }
            if (oam == null || tileSource == null) {
                throw new IllegalArgumentException("Dynamic OAM metadata cannot be null");
            }
        }
    }

    private final int entityType;
    private final int bank;
    private final int address;
    private final Shape shape;
    private final int initialVariant;
    private final List<Variant> variants;
    private final List<List<RectangleSprite>> rectangleVariants;
    private final List<List<DynamicSprite>> dynamicVariants;

    public EntitySpriteDefinition(int entityType, int bank, int address, Shape shape,
                                  int initialVariant, List<Variant> variants) {
        this(entityType, bank, address, shape, initialVariant, variants, List.of(), List.of());
    }

    public EntitySpriteDefinition(int entityType, int bank, int address, Shape shape,
                                  int initialVariant, List<Variant> variants,
                                  List<List<RectangleSprite>> rectangleVariants) {
        this(entityType, bank, address, shape, initialVariant, variants, rectangleVariants,
            List.of());
    }

    private EntitySpriteDefinition(int entityType, int bank, int address, Shape shape,
                                   int initialVariant, List<Variant> variants,
                                   List<List<RectangleSprite>> rectangleVariants,
                                   List<List<DynamicSprite>> dynamicVariants) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte");
        }
        if (shape == null || variants == null || rectangleVariants == null
            || dynamicVariants == null) {
            throw new IllegalArgumentException("Entity shape and variants cannot be null");
        }
        if (shape == Shape.UNSUPPORTED) {
            if (!variants.isEmpty() || !rectangleVariants.isEmpty() || !dynamicVariants.isEmpty()
                || initialVariant != -1) {
                throw new IllegalArgumentException("Unsupported definitions cannot have variants");
            }
        } else if (shape == Shape.RECTANGLE) {
            if (!variants.isEmpty() || rectangleVariants.isEmpty()
                || !dynamicVariants.isEmpty()
                || initialVariant < 0 || initialVariant >= rectangleVariants.size()) {
                throw new IllegalArgumentException("Rectangle definition has invalid variants");
            }
            for (List<RectangleSprite> variant : rectangleVariants) {
                if (variant == null || variant.isEmpty() || variant.stream().anyMatch(s -> s == null)) {
                    throw new IllegalArgumentException("Rectangle variants cannot be empty");
                }
            }
        } else if (shape == Shape.DYNAMIC) {
            if (!variants.isEmpty() || !rectangleVariants.isEmpty() || dynamicVariants.isEmpty()
                || initialVariant < 0 || initialVariant >= dynamicVariants.size()) {
                throw new IllegalArgumentException("Dynamic definition has invalid variants");
            }
            for (List<DynamicSprite> variant : dynamicVariants) {
                if (variant == null || variant.isEmpty() || variant.stream().anyMatch(s -> s == null)) {
                    throw new IllegalArgumentException("Dynamic variants cannot be empty");
                }
            }
        } else if (variants.isEmpty() || initialVariant < 0 || initialVariant >= variants.size()) {
            if (!rectangleVariants.isEmpty() || !dynamicVariants.isEmpty()) {
                throw new IllegalArgumentException("Pair and single definitions cannot have rectangles");
            }
            throw new IllegalArgumentException("Supported entity definition has invalid variants");
        } else if (!rectangleVariants.isEmpty() || !dynamicVariants.isEmpty()) {
            throw new IllegalArgumentException("Pair and single definitions cannot have extra OAM lists");
        }
        this.entityType = entityType;
        this.bank = bank;
        this.address = address;
        this.shape = shape;
        this.initialVariant = initialVariant;
        this.variants = List.copyOf(variants);
        this.rectangleVariants = copyRectangleVariants(rectangleVariants);
        this.dynamicVariants = copyDynamicVariants(dynamicVariants);
    }

    public static EntitySpriteDefinition dynamic(int entityType, int bank, int address,
                                                 int initialVariant,
                                                 List<List<DynamicSprite>> variants) {
        return new EntitySpriteDefinition(entityType, bank, address, Shape.DYNAMIC,
            initialVariant, List.of(), List.of(), variants);
    }

    public static EntitySpriteDefinition unsupported(int entityType) {
        return new EntitySpriteDefinition(entityType, -1, -1, Shape.UNSUPPORTED,
            -1, List.of());
    }

    public int entityType() {
        return entityType;
    }

    public int bank() {
        return bank;
    }

    public int address() {
        return address;
    }

    public Shape shape() {
        return shape;
    }

    public int initialVariant() {
        return initialVariant;
    }

    public int variantCount() {
        return switch (shape) {
            case RECTANGLE -> rectangleVariants.size();
            case DYNAMIC -> dynamicVariants.size();
            default -> variants.size();
        };
    }

    public boolean supported() {
        return shape != Shape.UNSUPPORTED;
    }

    public Variant variant(int index) {
        return variants.get(index);
    }

    public List<Variant> variants() {
        return variants;
    }

    public List<RectangleSprite> rectangleVariant(int index) {
        return rectangleVariants.get(index);
    }

    public List<List<RectangleSprite>> rectangleVariants() {
        return rectangleVariants;
    }

    public List<DynamicSprite> dynamicVariant(int index) {
        return dynamicVariants.get(index);
    }

    public List<List<DynamicSprite>> dynamicVariants() {
        return dynamicVariants;
    }

    private static List<List<RectangleSprite>> copyRectangleVariants(
                                                                List<List<RectangleSprite>> source) {
        List<List<RectangleSprite>> copy = new java.util.ArrayList<>(source.size());
        for (List<RectangleSprite> variant : source) {
            copy.add(List.copyOf(variant));
        }
        return List.copyOf(copy);
    }

    private static List<List<DynamicSprite>> copyDynamicVariants(
        List<List<DynamicSprite>> source) {
        java.util.ArrayList<List<DynamicSprite>> copy = new java.util.ArrayList<>(source.size());
        for (List<DynamicSprite> variant : source) {
            copy.add(List.copyOf(variant));
        }
        return List.copyOf(copy);
    }
}
