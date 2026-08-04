package linksawakening.entity;

import java.util.List;

/** ROM-decoded OAM display list and handler metadata for one entity type. */
public final class EntitySpriteDefinition {

    public enum Shape {
        PAIR,
        SINGLE,
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

    private final int entityType;
    private final int bank;
    private final int address;
    private final Shape shape;
    private final int initialVariant;
    private final List<Variant> variants;

    public EntitySpriteDefinition(int entityType, int bank, int address, Shape shape,
                                  int initialVariant, List<Variant> variants) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte");
        }
        if (shape == null || variants == null) {
            throw new IllegalArgumentException("Entity shape and variants cannot be null");
        }
        if (shape == Shape.UNSUPPORTED) {
            if (!variants.isEmpty() || initialVariant != -1) {
                throw new IllegalArgumentException("Unsupported definitions cannot have variants");
            }
        } else if (variants.isEmpty() || initialVariant < 0 || initialVariant >= variants.size()) {
            throw new IllegalArgumentException("Supported entity definition has invalid variants");
        }
        this.entityType = entityType;
        this.bank = bank;
        this.address = address;
        this.shape = shape;
        this.initialVariant = initialVariant;
        this.variants = List.copyOf(variants);
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
        return variants.size();
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
}
