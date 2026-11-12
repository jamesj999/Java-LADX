package linksawakening.world.entity;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;

/** Bounded room-owned cache of immutable ROM display lists; no per-query keys or closures. */
public final class EntitySpriteViews {
    private static final EntityRoomLoader.RoomTable[] TABLES = EntityRoomLoader.RoomTable.values();
    private static final EntityStatus[] STATUSES = EntityStatus.values();
    private static final int CAPACITY = 128;
    private final EntitySpriteHandlerCatalog catalog;
    private final int[] kinds = new int[CAPACITY],
            first = new int[CAPACITY],
            second = new int[CAPACITY],
            third = new int[CAPACITY];
    private final EntitySpriteDefinition[] definitions = new EntitySpriteDefinition[CAPACITY];
    private int size, next;

    EntitySpriteViews(EntitySpriteHandlerCatalog catalog) {
        this.catalog = catalog;
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
        EntitySpriteDefinition value = decode(kind, a, b, c);
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

    private EntitySpriteDefinition decode(int kind, int a, int b, int c) {
        return switch (kind) {
            case 0 -> catalog == null ? EntitySpriteDefinition.unsupported(a) : catalog.forEntityType(a, TABLES[b], c);
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
}
