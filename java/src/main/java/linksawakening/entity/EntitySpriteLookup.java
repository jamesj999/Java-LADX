package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;

/**
 * ROM-backed display-list decoder plus the small handler-to-list mapping
 * required by the first entity rendering increment.
 */

/** EntitySpriteLookup behavior on the shared EntitySpriteHandlerCatalog state. */
abstract class EntitySpriteLookup extends EntitySpriteDecodeState {
    protected EntitySpriteLookup(byte[] romData) {
        super(romData);
    }

    public EntitySpriteDefinition forEntityType(int entityType, EntityRoomLoader.RoomTable roomTable) {
        return forEntityType(entityType, roomTable, -1);
    }

    /** Selects the runtime-dependent display list used by entity $31. */
    public EntitySpriteDefinition forSwordShieldPickup(boolean beachSword) {
        return decodeSingle(ENTITY_SWORD_SHIELD_PICKUP, 0x03, beachSword ? 0x5B97 : 0x5B95, 1, 0);
    }

    /** Selects SirensInstrumentState1Handler's two projectile variants. */
    public EntitySpriteDefinition forKidPlaying() {
        return decodePair(0x71, 0x06, 0x606D, 4, 0);
    }

    public EntitySpriteDefinition forKidBall() {
        return decodeSingle(0x71, 0x06, 0x607D, 1, 0);
    }

    public EntitySpriteDefinition forInstrumentShard() {
        return decodePair(ENTITY_INSTRUMENT_OF_THE_SIRENS, 0x03, 0x5E8B, 2, 0);
    }

    /** Selects the perched pair or four-sprite flying layout used by the owl event. */
    public EntitySpriteDefinition forOwlEvent(boolean flying) {
        return flying ? decodeRectangle(0x41, 0x06, 0x6A3F, 1, 4, 0) : decodePair(0x41, 0x06, 0x6A37, 2, 0);
    }

    public EntitySpriteDefinition forEntityType(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        EntitySpriteDefinition result;
        result = EntitySpriteFloatingItemRegistrations.lookupFloatingItem(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteSheetsRegistrations.lookupSheets(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteMusicalNoteRegistrations.lookupMusicalNote(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteWitchRatRegistrations.lookupWitchRat(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteKid71Registrations.lookupKid71(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteMoblinRegistrations.lookupMoblin(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteMoldormRegistrations.lookupMoldorm(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteBlooperRegistrations.lookupBlooper(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpritePolsVoiceRegistrations.lookupPolsVoice(this, entityType, roomTable, mapId);
        if (result != null) return result;
        result = EntitySpriteSwordShieldPickupRegistrations.lookupSwordShieldPickup(this, entityType, roomTable, mapId);
        if (result != null) return result;

        return EntitySpriteDefinition.unsupported(entityType);
    }

    /**
     * Returns the chest display list selected by the source's two GBC room
     * exceptions. The alternate list contains two single-sprite entries;
     * every other room uses the normal 33-item list.
     */
    public EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant) {
        if ((mapId & ~0xFF) != 0 || (roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Chest map and room ids must be unsigned bytes");
        }
        if (itemVariant < 0 || itemVariant > 0x20) {
            throw new IllegalArgumentException("Chest item variant must be in range 0..0x20: " + itemVariant);
        }
        if ((mapId == 0x05 && roomId == 0xCE && itemVariant == 0x00)
                || (mapId == 0x06 && roomId == 0x1A && itemVariant == 0x01)) {
            return decodeSingle(ENTITY_CHEST_WITH_ITEM, 0x07, 0x7B53, 2, 0);
        }
        return decodeSingle(ENTITY_CHEST_WITH_ITEM, 0x07, 0x7B57, 0x21, 0);
    }

    /** Selects Pokey's bank-$15 body list for its current detached-segment count. */
    public EntitySpriteDefinition forPokeyState(int inertia) {
        if (inertia < 0) {
            throw new IllegalArgumentException("Pokey inertia cannot be negative: " + inertia);
        }
        if (inertia == 0) {
            return decodeRectangle(ENTITY_POKEY, 0x15, 0x4B17, 4, 6, 0);
        }
        if (inertia == 1) {
            return decodeRectangle(ENTITY_POKEY, 0x15, 0x4B77, 4, 4, 0);
        }
        return decodeRectangle(ENTITY_POKEY, 0x15, 0x4BB7, 4, 2, 0);
    }

    /** Selects Urchin's normal or credits bank-$15 display-list pair. */
    public EntitySpriteDefinition forUrchinState(boolean creditsGameplay) {
        return decodePair(ENTITY_URCHIN, 0x15, creditsGameplay ? 0x7393 : 0x7383, 4, 0);
    }

    /** Decodes Unknown044SpriteVariants at bank-$15:$4CD5 for detached segments. */
    public EntitySpriteDefinition forPokeySegment() {
        return decodePair(ENTITY_POKEY, 0x15, 0x4CD5, 1, 0);
    }

    /** Selects the two bank-$15 display-list pairs used by Stalfos Evasive. */
    public EntitySpriteDefinition forStalfosEvasiveState(int privateState1) {
        if (privateState1 < 0) {
            throw new IllegalArgumentException("Stalfos Evasive private state cannot be negative");
        }
        return privateState1 == 0
                ? decodePair(ENTITY_STALFOS_EVASIVE, 0x15, 0x4E7D, 3, 0)
                : decodePair(ENTITY_STALFOS_EVASIVE, 0x15, 0x4E8E, 2, 0);
    }

    /**
     * Selects Bush Crawler's bank-$07 display list for the lifted replacement
     * state. State $02 is the adjacent Bushcrawler2 list; state $01 is the
     * ordinary outdoor/indoor pair selected by the room table.
     */
    public EntitySpriteDefinition forBushCrawlerState(int privateState1, EntityRoomLoader.RoomTable roomTable) {
        if (privateState1 < 0 || privateState1 > 0x02) {
            throw new IllegalArgumentException("Bush Crawler private state out of range: " + privateState1);
        }
        if (privateState1 == 0x02) {
            return decodePair(ENTITY_BUSH_CRAWLER, 0x07, 0x400A, 2, 0);
        }
        return forEntityType(ENTITY_BUSH_CRAWLER, roomTable);
    }

    /**
     * Builds the two-pass display list used while a normal Bush Crawler is
     * crawling. The handler first renders its room-specific pair, then the
     * four-tile Bushcrawler2 pair at the same active position.
     */
    public EntitySpriteDefinition forBushCrawlerCrawlState(EntityRoomLoader.RoomTable roomTable, int privateState4) {
        if (privateState4 < 0 || privateState4 > 1) {
            throw new IllegalArgumentException("Bush Crawler private state 4 must be 0 or 1: " + privateState4);
        }
        EntitySpriteDefinition normal = forEntityType(ENTITY_BUSH_CRAWLER, roomTable);
        EntitySpriteDefinition crawling = decodePair(ENTITY_BUSH_CRAWLER, 0x07, 0x400A, 2, 0);
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(2);
        for (int animation = 0; animation < 2; animation++) {
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(4);
            appendDynamicPair(
                    sprites, normal.variant(privateState4), EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
            appendDynamicPair(
                    sprites,
                    crawling.variant(animation),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
            variants.add(List.copyOf(sprites));
        }
        return EntitySpriteDefinition.dynamic(ENTITY_BUSH_CRAWLER, 0x07, normal.address(), 0, variants);
    }

    /**
     * Selects the bank-$03 Iron Mask display list. Private state 2 is zero
     * while the mask is intact and nonzero after the mask has been removed.
     */
    public EntitySpriteDefinition forIronMaskState(int privateState2) {
        if ((privateState2 & ~0xFF) != 0) {
            throw new IllegalArgumentException("Iron Mask private state must be an unsigned byte: " + privateState2);
        }
        return privateState2 == 0
                ? decodePair(ENTITY_IRON_MASK, 0x03, 0x4FCB, 8, 0)
                : decodePair(ENTITY_IRON_MASK, 0x03, 0x4FEB, 2, 0);
    }
}
