package linksawakening.world.entity;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.MoldormSegmentPosition;

/** Bounded room-owned cache of immutable ROM display lists; no per-query keys or closures. */
public final class EntitySpriteViews {
    private static final EntityRoomLoader.RoomTable[] TABLES = EntityRoomLoader.RoomTable.values();
    private static final EntityStatus[] STATUSES = EntityStatus.values();
    private static final int CAPACITY = 128;
    private final Source source;
    private final int[] kinds = new int[CAPACITY],
            first = new int[CAPACITY],
            second = new int[CAPACITY],
            third = new int[CAPACITY];
    private final EntitySpriteDefinition[] definitions = new EntitySpriteDefinition[CAPACITY];
    private int size, next;

    static EntitySpriteViews unsupported() {
        return new EntitySpriteViews(UnsupportedSource.INSTANCE);
    }

    private EntitySpriteViews(Source source) {
        this.source = java.util.Objects.requireNonNull(source);
    }

    EntitySpriteViews(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteHandlerCatalog
                    catalog) {
        this.source = catalog == null ? UnsupportedSource.INSTANCE : new RomSource(catalog);
    }

    public EntitySpriteDefinition forEntityType(int type, EntityRoomLoader.RoomTable table, int map) {
        return get(0, type, table.ordinal(), map);
    }

    public EntitySpriteDefinition forColorShellState(int type, int state, EntityStatus status) {
        return get(1, type, state, status.ordinal());
    }

    public EntitySpriteDefinition forOwlEvent(boolean flying) {
        return get(2, flying ? 1 : 0, 0, 0);
    }

    public EntitySpriteDefinition forBushCrawlerState(int state, EntityRoomLoader.RoomTable table) {
        return get(3, state, table.ordinal(), 0);
    }

    public EntitySpriteDefinition forBushCrawlerCrawlState(EntityRoomLoader.RoomTable table, int state) {
        return get(4, table.ordinal(), state, 0);
    }

    public EntitySpriteDefinition forStalfosEvasiveState(int state) {
        return get(5, state, 0, 0);
    }

    public EntitySpriteDefinition forPokeyState(int state) {
        return get(6, state, 0, 0);
    }

    public EntitySpriteDefinition forIronMaskState(int state) {
        return get(7, state, 0, 0);
    }

    public EntitySpriteDefinition forUrchinState(boolean credits) {
        return get(8, credits ? 1 : 0, 0, 0);
    }

    public EntitySpriteDefinition forSwordShieldPickup(boolean sword) {
        return get(9, sword ? 1 : 0, 0, 0);
    }

    public EntitySpriteDefinition forFollowerEntityType(int type) {
        return get(10, type, 0, 0);
    }

    public EntitySpriteDefinition forMoblinKingState(int body, int weapon, boolean airborne) {
        return get(11, body, weapon, airborne ? 1 : 0);
    }

    public EntitySpriteDefinition forMagicPowderTorchState(boolean lit) {
        return get(12, lit ? 1 : 0, 0, 0);
    }

    public EntitySpriteDefinition forKidPlaying() {
        return get(13, 0, 0, 0);
    }

    public EntitySpriteDefinition forKidBall() {
        return get(14, 0, 0, 0);
    }

    public EntitySpriteDefinition forInstrumentShard() {
        return get(15, 0, 0, 0);
    }

    public EntitySpriteDefinition forFallingEntity() {
        return get(16, 0, 0, 0);
    }

    public EntitySpriteDefinition forBurningEntity() {
        return get(17, 0, 0, 0);
    }

    public EntitySpriteDefinition forZolSlimeEye() {
        return get(18, 0, 0, 0);
    }

    public EntitySpriteDefinition forPokeySegment() {
        return get(19, 0, 0, 0);
    }

    public EntitySpriteDefinition forMagicRodFireState() {
        return get(20, 0, 0, 0);
    }

    public EntitySpriteDefinition forBombArrow() {
        return get(21, 0, 0, 0);
    }

    public EntitySpriteDefinition forBombRightBeforeExploding() {
        return get(22, 0, 0, 0);
    }

    public EntitySpriteDefinition forBombExplosion() {
        return get(23, 0, 0, 0);
    }

    public EntitySpriteDefinition forEntityType(int type, EntityRoomLoader.RoomTable table) {
        return forEntityType(type, table, -1);
    }

    private EntitySpriteDefinition get(int kind, int a, int b, int c) {
        for (int i = 0; i < size; i++) {
            if (kinds[i] == kind && first[i] == a && second[i] == b && third[i] == c) return definitions[i];
        }
        EntitySpriteDefinition value = source.decode(kind, a, b, c);
        int index = next;
        next = (next + 1) % CAPACITY;
        size = Math.min(size + 1, CAPACITY);
        kinds[index] = kind;
        first[index] = a;
        second[index] = b;
        third[index] = c;
        definitions[index] = value;
        return value;
    }

    /** False only for the explicit ROM-free source, whose display lists are unsupported. */
    public boolean supportsPresentation() {
        return source.supportsPresentation();
    }

    /** A ROM-free source preserves the entity and its variant at optional presentation sites. */
    public RoomEntity present(RoomEntity entity, EntitySpriteDefinition definition, int variant) {
        return source.present(entity, definition, variant);
    }

    public EntitySpriteDefinition forMiniMoldormState(
            int headVariant, int entityX, int entityY, int segment1X, int segment1Y, int segment2X, int segment2Y) {
        return source.forMiniMoldormState(headVariant, entityX, entityY, segment1X, segment1Y, segment2X, segment2Y);
    }

    public EntitySpriteDefinition forMoldormState(
            int headVariant,
            int entityX,
            int entityY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter) {
        return source.forMoldormState(headVariant, entityX, entityY, segments, privateState3, frameCounter);
    }

    public EntitySpriteDefinition forMoldormPresentation(
            int headVariant,
            int headX,
            int headY,
            int renderBaseX,
            int renderBaseY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter) {
        return source.forMoldormPresentation(
                headVariant, headX, headY, renderBaseX, renderBaseY, segments, privateState3, frameCounter);
    }

    public EntitySpriteDefinition forSlimeEyeState(boolean half, int variant, int z) {
        return source.forSlimeEyeState(half, variant, z);
    }

    public EntitySpriteDefinition decodeSingle(
            int entityType, int bank, int address, int variantCount, int initialVariant) {
        return source.decodeSingle(entityType, bank, address, variantCount, initialVariant);
    }

    public EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant) {
        return source.forChestState(mapId, roomId, itemVariant);
    }

    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY, int spriteVariant) {
        return source.forRollingBonesBar(privateState1, entityY, spriteVariant);
    }

    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY) {
        return source.forRollingBonesBar(privateState1, entityY);
    }

    private static final EntitySpriteDefinition[] UNSUPPORTED = unsupportedDefinitions();

    private static EntitySpriteDefinition[] unsupportedDefinitions() {
        EntitySpriteDefinition[] definitions = new EntitySpriteDefinition[256];
        java.util.Arrays.setAll(definitions, EntitySpriteDefinition::unsupported);
        return definitions;
    }

    private enum UnsupportedSource implements Source {
        INSTANCE;

        public boolean supportsPresentation() {
            return false;
        }

        public RoomEntity present(RoomEntity entity, EntitySpriteDefinition definition, int variant) {
            return entity;
        }

        public EntitySpriteDefinition decode(int kind, int a, int b, int c) {
            int type =
                    switch (kind) {
                        case 0, 1, 10 -> a;
                        case 2 -> 0x41;
                        case 3, 4 -> 0xBB;
                        case 5 -> 0x1E;
                        case 6, 19 -> 0xE3;
                        case 7 -> 0x24;
                        case 8 -> 0xC5;
                        case 9 -> 0x31;
                        case 11 -> 0x92;
                        case 12 -> 0x08;
                        case 13 -> 0x70;
                        case 14 -> 0x6B;
                        case 15 -> 0x39;
                        case 16, 17 -> 0;
                        case 18 -> 0x1B;
                        case 20 -> 4;
                        case 21 -> 0;
                        case 22, 23 -> 2;
                        default -> throw new AssertionError(kind);
                    };
            return UNSUPPORTED[type & 255];
        }
    }

    private interface Source {
        EntitySpriteDefinition decode(int kind, int a, int b, int c);

        default boolean supportsPresentation() {
            return true;
        }

        default RoomEntity present(RoomEntity entity, EntitySpriteDefinition definition, int variant) {
            return EntitySnapshots.withDefinition(entity, definition, variant);
        }

        default EntitySpriteDefinition forMiniMoldormState(
                int headVariant, int entityX, int entityY, int segment1X, int segment1Y, int segment2X, int segment2Y) {
            return UNSUPPORTED[41 & 255];
        }

        default EntitySpriteDefinition forMoldormState(
                int headVariant,
                int entityX,
                int entityY,
                List<MoldormSegmentPosition> segments,
                int privateState3,
                int frameCounter) {
            return UNSUPPORTED[89 & 255];
        }

        default EntitySpriteDefinition forMoldormPresentation(
                int headVariant,
                int headX,
                int headY,
                int renderBaseX,
                int renderBaseY,
                List<MoldormSegmentPosition> segments,
                int privateState3,
                int frameCounter) {
            return UNSUPPORTED[89 & 255];
        }

        default EntitySpriteDefinition forSlimeEyeState(boolean half, int variant, int z) {
            return UNSUPPORTED[91 & 255];
        }

        default EntitySpriteDefinition decodeSingle(
                int entityType, int bank, int address, int variantCount, int initialVariant) {
            return UNSUPPORTED[entityType & 255];
        }

        default EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant) {
            return UNSUPPORTED[7 & 255];
        }

        default EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY, int spriteVariant) {
            return UNSUPPORTED[130 & 255];
        }

        default EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY) {
            return UNSUPPORTED[130 & 255];
        }
    }

    private record RomSource(EntitySpriteHandlerCatalog catalog) implements Source {
        private RomSource {
            java.util.Objects.requireNonNull(catalog, "catalog");
        }

        public EntitySpriteDefinition decode(int kind, int a, int b, int c) {
            return switch (kind) {
                case 0 -> catalog.forEntityType(a, TABLES[b], c);
                case 1 -> catalog.forColorShellState(a, b, STATUSES[c]);
                case 2 -> catalog.forOwlEvent(a != 0);
                case 3 -> catalog.forBushCrawlerState(a, TABLES[b]);
                case 4 -> catalog.forBushCrawlerCrawlState(TABLES[a], b);
                case 5 -> catalog.forStalfosEvasiveState(a);
                case 6 -> catalog.forPokeyState(a);
                case 7 -> catalog.forIronMaskState(a);
                case 8 -> catalog.forUrchinState(a != 0);
                case 9 -> catalog.forSwordShieldPickup(a != 0);
                case 10 -> catalog.forFollowerEntityType(a);
                case 11 -> catalog.forMoblinKingState(a, b, c != 0);
                case 12 -> catalog.forMagicPowderTorchState(a != 0);
                case 13 -> catalog.forKidPlaying();
                case 14 -> catalog.forKidBall();
                case 15 -> catalog.forInstrumentShard();
                case 16 -> catalog.forFallingEntity();
                case 17 -> catalog.forBurningEntity();
                case 18 -> catalog.forZolSlimeEye();
                case 19 -> catalog.forPokeySegment();
                case 20 -> catalog.forMagicRodFireState();
                case 21 -> catalog.forBombArrow();
                case 22 -> catalog.forBombRightBeforeExploding();
                case 23 -> catalog.forBombExplosion();
                default -> throw new AssertionError(kind);
            };
        }

        public EntitySpriteDefinition forMiniMoldormState(
                int headVariant, int entityX, int entityY, int segment1X, int segment1Y, int segment2X, int segment2Y) {
            return catalog.forMiniMoldormState(
                    headVariant, entityX, entityY, segment1X, segment1Y, segment2X, segment2Y);
        }

        public EntitySpriteDefinition forMoldormState(
                int headVariant,
                int entityX,
                int entityY,
                List<MoldormSegmentPosition> segments,
                int privateState3,
                int frameCounter) {
            return catalog.forMoldormState(headVariant, entityX, entityY, segments, privateState3, frameCounter);
        }

        public EntitySpriteDefinition forMoldormPresentation(
                int headVariant,
                int headX,
                int headY,
                int renderBaseX,
                int renderBaseY,
                List<MoldormSegmentPosition> segments,
                int privateState3,
                int frameCounter) {
            return catalog.forMoldormPresentation(
                    headVariant, headX, headY, renderBaseX, renderBaseY, segments, privateState3, frameCounter);
        }

        public EntitySpriteDefinition forSlimeEyeState(boolean half, int variant, int z) {
            return catalog.forSlimeEyeState(half, variant, z);
        }

        public EntitySpriteDefinition decodeSingle(
                int entityType, int bank, int address, int variantCount, int initialVariant) {
            return catalog.decodeSingle(entityType, bank, address, variantCount, initialVariant);
        }

        public EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant) {
            return catalog.forChestState(mapId, roomId, itemVariant);
        }

        public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY, int spriteVariant) {
            return catalog.forRollingBonesBar(privateState1, entityY, spriteVariant);
        }

        public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY) {
            return catalog.forRollingBonesBar(privateState1, entityY);
        }
    }
}
