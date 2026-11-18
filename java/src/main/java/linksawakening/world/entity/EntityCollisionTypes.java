package linksawakening.world.entity;

import static linksawakening.world.entity.RoomEntityCombatRules.BIG_ENEMY_HITBOX_HEIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.BIG_ENEMY_HITBOX_WIDTH;
import static linksawakening.world.entity.RoomEntityCombatRules.BIG_NPC_HITBOX_HEIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.BIG_NPC_HITBOX_WIDTH;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ANIMAL_D0;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ANIMAL_D1;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ANIMAL_D2;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ANTI_FAIRY;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ARMOS_KNIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ARMOS_STATUE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BANANAS_SCHULE_SALE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BLOOPER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BOMBER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BOO_BUDDY;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BOUNCING_BOMBITE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BUNNY_D3;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BUSH_CRAWLER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_BUZZ_BLOB;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_COLOR_SHELL_BLUE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_COLOR_SHELL_GREEN;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_COLOR_SHELL_RED;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_CRAZY_TRACY;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_CROW;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_CRYSTAL_SWITCH;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_CUCCO;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_FISH;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GEL;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GHINI;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GIANT_GHINI;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GIANT_GOPONGA_FLOWER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GIBDO;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GOOMBA;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GOPONGA_FLOWER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GOPONGA_FLOWER_PROJECTILE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GRANDMA_ULRIRA;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_GRANDPA_ULRIRA;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_HARDHAT_BEETLE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_HIDING_GHINI;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_HIDING_ZOL;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_HINOX;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_HOLE_FILLER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_IRON_MASK;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_KEESE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_KID_70;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_KID_71;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_KID_72;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_KID_73;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_LEEVER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_LIKE_LIKE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MADAM_MEOWMEOW;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MAD_BOMBER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MARIN;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MARIN_SHORE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MARIN_TAL_TAL;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MASKED_MIMIC_GORIYA;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MERMAID;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MERMAID_STATUE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MIMIC;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MINI_MOLDORM;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MOBLIN;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MOBLIN_SWORD;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MOLDORM;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_MR_WRITE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_OCTOROK;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_OWL_EVENT;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_OWL_STATUE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PAIRODD;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PAPAHL;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PAPAHLS_WIFE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PEAHAT;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PHOTOGRAPHER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PINCER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PIRANHA_PLANT;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_POKEY;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_POLS_VOICE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_PUSHED_BLOCK;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ROLLING_BONES;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ROLLING_BONES_BAR;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ROOSTER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_SAND_CRAB;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_SHOP_OWNER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_SNAKE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_SPARK_CLOCKWISE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_SPARK_COUNTER_CLOCKWISE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_SPIKED_BEETLE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_SPIKE_TRAP;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_STALFOS_AGGRESSIVE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_STALFOS_EVASIVE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_STAR;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_TARIN;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_TARIN_BEEKEEPER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_TEKTITE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_THREE_OF_A_KIND;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_TIMER_BOMBITE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_TRENDY_GAME_OWNER;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_URCHIN;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_WATER_TEKTITE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_WINGED_OCTOROK;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_WITCH;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_WITCH_RAT;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_WIZROBE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ZOL;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ZOMBIE;
import static linksawakening.world.entity.RoomEntityCombatRules.ENTITY_ZORA;
import static linksawakening.world.entity.RoomEntityCombatRules.HITBOX_HEIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.HITBOX_WIDTH;
import static linksawakening.world.entity.RoomEntityCombatRules.NPC_HITBOX_HEIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.NPC_HITBOX_WIDTH;
import static linksawakening.world.entity.RoomEntityCombatRules.PILLAR_HITBOX_HEIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.PILLAR_HITBOX_WIDTH;
import static linksawakening.world.entity.RoomEntityCombatRules.SIDE_VIEW_PLATFORM_HITBOX_HEIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.SIDE_VIEW_PLATFORM_HITBOX_WIDTH;
import static linksawakening.world.entity.RoomEntityCombatRules.SMALL_ENEMY_HITBOX_HEIGHT;
import static linksawakening.world.entity.RoomEntityCombatRules.SMALL_ENEMY_HITBOX_WIDTH;

/** Focused behavior using the state owned by RoomEntityCombatRules. */
final class EntityCollisionTypes {
    static boolean supportsEnemyCollision(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE,
                    ENTITY_MOBLIN,
                    ENTITY_IRON_MASK,
                    ENTITY_MOBLIN_SWORD,
                    ENTITY_TEKTITE,
                    ENTITY_LEEVER,
                    ENTITY_ANTI_FAIRY,
                    ENTITY_SPARK_COUNTER_CLOCKWISE,
                    ENTITY_SPARK_CLOCKWISE,
                    ENTITY_STALFOS_AGGRESSIVE,
                    ENTITY_STALFOS_EVASIVE,
                    ENTITY_ZOL,
                    ENTITY_GEL,
                    ENTITY_HIDING_ZOL,
                    ENTITY_STAR,
                    ENTITY_BLOOPER,
                    ENTITY_WINGED_OCTOROK,
                    ENTITY_PINCER,
                    ENTITY_BUSH_CRAWLER,
                    ENTITY_GIBDO,
                    ENTITY_POLS_VOICE,
                    ENTITY_LIKE_LIKE,
                    ENTITY_PEAHAT,
                    ENTITY_GOOMBA,
                    ENTITY_SNAKE,
                    ENTITY_WIZROBE,
                    ENTITY_ARMOS_STATUE,
                    ENTITY_ARMOS_KNIGHT,
                    ENTITY_HINOX,
                    ENTITY_ROLLING_BONES,
                    ENTITY_ROLLING_BONES_BAR,
                    ENTITY_HIDING_GHINI,
                    ENTITY_GIANT_GHINI,
                    ENTITY_GHINI,
                    ENTITY_HARDHAT_BEETLE,
                    ENTITY_SPIKE_TRAP,
                    ENTITY_WATER_TEKTITE,
                    ENTITY_FISH,
                    ENTITY_CROW,
                    ENTITY_CUCCO,
                    ENTITY_GOPONGA_FLOWER,
                    ENTITY_GIANT_GOPONGA_FLOWER,
                    ENTITY_GOPONGA_FLOWER_PROJECTILE,
                    ENTITY_POKEY,
                    ENTITY_PIRANHA_PLANT,
                    ENTITY_ZORA,
                    ENTITY_ZOMBIE,
                    ENTITY_BUZZ_BLOB,
                    ENTITY_SAND_CRAB,
                    ENTITY_URCHIN,
                    ENTITY_BOO_BUDDY,
                    ENTITY_SPIKED_BEETLE,
                    ENTITY_PAIRODD,
                    ENTITY_COLOR_SHELL_RED,
                    ENTITY_COLOR_SHELL_GREEN,
                    ENTITY_COLOR_SHELL_BLUE,
                    ENTITY_CRYSTAL_SWITCH,
                    ENTITY_OCTOROK,
                    ENTITY_BOUNCING_BOMBITE,
                    ENTITY_TIMER_BOMBITE,
                    ENTITY_MAD_BOMBER,
                    ENTITY_BOMBER,
                    ENTITY_MIMIC,
                    ENTITY_MINI_MOLDORM,
                    ENTITY_MOLDORM,
                    0x5B,
                    ENTITY_MASKED_MIMIC_GORIYA,
                    ENTITY_THREE_OF_A_KIND,
                    ENTITY_WITCH_RAT,
                    0xE4 -> true;
            default -> false;
        };
    }

    static int hitboxWidth(int type) {
        if (RoomEntityCombatRules.usesBigNpcHitbox(type)) {
            return BIG_NPC_HITBOX_WIDTH;
        }
        if (RoomEntityCombatRules.usesNpcHitbox(type)) {
            return NPC_HITBOX_WIDTH;
        }
        if (RoomEntityCombatRules.usesPillarHitbox(type)) {
            return PILLAR_HITBOX_WIDTH;
        }
        if (RoomEntityCombatRules.usesSideViewPlatformHitbox(type)) {
            return SIDE_VIEW_PLATFORM_HITBOX_WIDTH;
        }
        return switch (type & 0xFF) {
            case ENTITY_GEL -> SMALL_ENEMY_HITBOX_WIDTH;
            case ENTITY_PUSHED_BLOCK,
                    ENTITY_GIANT_GHINI,
                    ENTITY_GIANT_GOPONGA_FLOWER,
                    ENTITY_SPIKE_TRAP,
                    ENTITY_ARMOS_KNIGHT,
                    ENTITY_URCHIN,
                    ENTITY_MOLDORM,
                    ENTITY_HINOX -> BIG_ENEMY_HITBOX_WIDTH;
            default -> HITBOX_WIDTH;
        };
    }

    static int hitboxHeight(int type) {
        if (type == ENTITY_GRANDPA_ULRIRA) return 0x0A;
        if ((type & 0xFF) == ENTITY_WITCH) {
            return 0x0E;
        }
        if (RoomEntityCombatRules.usesBigNpcHitbox(type)) {
            return BIG_NPC_HITBOX_HEIGHT;
        }
        if (RoomEntityCombatRules.usesNpcHitbox(type)) {
            return NPC_HITBOX_HEIGHT;
        }
        if (RoomEntityCombatRules.usesPillarHitbox(type)) {
            return PILLAR_HITBOX_HEIGHT;
        }
        if (RoomEntityCombatRules.usesSideViewPlatformHitbox(type)) {
            return SIDE_VIEW_PLATFORM_HITBOX_HEIGHT;
        }
        return switch (type & 0xFF) {
            case ENTITY_GEL -> SMALL_ENEMY_HITBOX_HEIGHT;
            case ENTITY_PUSHED_BLOCK,
                    ENTITY_GIANT_GHINI,
                    ENTITY_GIANT_GOPONGA_FLOWER,
                    ENTITY_SPIKE_TRAP,
                    ENTITY_ARMOS_KNIGHT,
                    ENTITY_URCHIN,
                    ENTITY_MOLDORM,
                    ENTITY_HINOX -> BIG_ENEMY_HITBOX_HEIGHT;
            default -> HITBOX_HEIGHT;
        };
    }

    static boolean usesNpcHitbox(int type) {
        return switch (type & 0xFF) {
            case ENTITY_MARIN,
                    ENTITY_TARIN,
                    ENTITY_WITCH,
                    ENTITY_OWL_EVENT,
                    ENTITY_OWL_STATUE,
                    ENTITY_SHOP_OWNER,
                    ENTITY_TRENDY_GAME_OWNER,
                    ENTITY_KID_70,
                    ENTITY_KID_71,
                    ENTITY_KID_72,
                    ENTITY_KID_73,
                    ENTITY_PAPAHLS_WIFE,
                    ENTITY_GRANDMA_ULRIRA,
                    ENTITY_GRANDPA_ULRIRA,
                    ENTITY_MADAM_MEOWMEOW,
                    ENTITY_CRAZY_TRACY,
                    ENTITY_MR_WRITE,
                    ENTITY_HOLE_FILLER,
                    ENTITY_PAPAHL,
                    ENTITY_TARIN_BEEKEEPER,
                    ENTITY_MERMAID,
                    ENTITY_MARIN_SHORE,
                    ENTITY_MARIN_TAL_TAL,
                    ENTITY_MERMAID_STATUE,
                    ENTITY_BANANAS_SCHULE_SALE,
                    ENTITY_ANIMAL_D0,
                    ENTITY_ANIMAL_D1,
                    ENTITY_ANIMAL_D2,
                    ENTITY_BUNNY_D3,
                    ENTITY_PHOTOGRAPHER,
                    ENTITY_ROOSTER -> true;
            default -> false;
        };
    }
}
