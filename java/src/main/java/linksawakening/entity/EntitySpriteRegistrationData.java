package linksawakening.entity;

import linksawakening.world.entity.EntityRoomLoader;

/** Immutable family registration recipes. Lookup preserves contextual priority and all map selector branches. */
final class EntitySpriteRegistrationData {
    private static final int[] MAPS = {-1, 1, 3, 6, 7, 10, 22, 31, 128, 129, 130, 131};
    private static final EntitySpriteRegistrationData INSTANCE = new EntitySpriteRegistrationData();
    private final EntitySpriteRecipe[][][] recipes;

    private EntitySpriteRegistrationData() {
        var rooms = EntityRoomLoader.RoomTable.values();
        recipes = new EntitySpriteRecipe[rooms.length][MAPS.length][256];
        for (var room : rooms)
            for (int map = 0; map < MAPS.length; map++)
                for (int entity = 0; entity < 256; entity++)
                    recipes[room.ordinal()][map][entity] = select(entity, room, MAPS[map]);
    }

    static EntitySpriteRegistrationData instance() {
        return INSTANCE;
    }

    EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if ((entityType & ~255) != 0)
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        if (roomTable == null) throw new IllegalArgumentException("Room entity table cannot be null");
        int map =
                switch (mapId) {
                    case 1 -> 1;
                    case 3 -> 2;
                    case 6 -> 3;
                    case 7 -> 4;
                    case 10 -> 5;
                    case 22 -> 6;
                    case 31 -> 7;
                    default -> mapId < 0 ? 0 : 8 + (mapId & 3);
                };
        return recipes[roomTable.ordinal()][map][entityType];
    }

    private static EntitySpriteRecipe select(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {

        EntitySpriteRecipe result;
        result = EntitySpriteContextualObjectRegistrations.lookupFloatingItem(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteRoomObjectRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpritePickupRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteBossRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpritePlayerProjectileRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteOverworldCreatureRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteCompanionRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpritePlantRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteAquaticRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteUndeadEnemyRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteVillageNpcRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteDungeonCreatureRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteSideViewRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteArmoredEnemyRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteHazardRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteSlimeEnemyRegistrations.lookup(entityType, roomTable, mapId);
        if (result != null) return result;

        return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.UNSUPPORTED, entityType);
    }
}
