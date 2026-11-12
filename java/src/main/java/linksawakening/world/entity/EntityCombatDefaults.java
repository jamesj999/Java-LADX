package linksawakening.world.entity;

import static linksawakening.world.entity.RoomEntityCombatRules.*;

/** Focused behavior using the state owned by RoomEntityCombatRules. */
final class EntityCombatDefaults {
    static int contactDamage(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE,
                    ENTITY_OCTOROK,
                    ENTITY_PEAHAT,
                    ENTITY_ZOL,
                    ENTITY_GEL,
                    ENTITY_WATER_TEKTITE,
                    ENTITY_PAIRODD,
                    ENTITY_HIDING_ZOL,
                    ENTITY_STAR,
                    ENTITY_BLOOPER,
                    ENTITY_WINGED_OCTOROK,
                    ENTITY_GOOMBA,
                    ENTITY_SNAKE,
                    ENTITY_BUSH_CRAWLER,
                    ENTITY_POKEY,
                    ENTITY_PIRANHA_PLANT,
                    ENTITY_ZORA,
                    ENTITY_ZOMBIE -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_BUZZ_BLOB -> 0x08;
            case ENTITY_SAND_CRAB -> 0x04;
            case ENTITY_URCHIN -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_FISH -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_CROW -> CROW_CONTACT_DAMAGE;
                // Cucco's static health-group table points at the ordinary enemy
                // damage row; physics_flags.asm marks it harmless, and the
                // runtime suppresses this raw value at the entity boundary.
            case ENTITY_CUCCO -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_GOPONGA_FLOWER, ENTITY_GIANT_GOPONGA_FLOWER -> 0x08;
            case ENTITY_BOO_BUDDY -> BOO_BUDDY_CONTACT_DAMAGE;
            case ENTITY_ARMOS_STATUE -> 0x10;
            case ENTITY_ARMOS_KNIGHT -> 0x0C;
            case ENTITY_HINOX -> 0x08;
            case ENTITY_ROLLING_BONES -> 0x08;
            case ENTITY_ROLLING_BONES_BAR -> 0x04;
            case ENTITY_MOBLIN,
                    ENTITY_IRON_MASK,
                    ENTITY_MOBLIN_SWORD,
                    ENTITY_TEKTITE,
                    ENTITY_LEEVER,
                    ENTITY_STALFOS_AGGRESSIVE,
                    ENTITY_STALFOS_EVASIVE,
                    ENTITY_MINI_MOLDORM -> MOBLIN_CONTACT_DAMAGE;
            case ENTITY_MIMIC -> 0x18;
            case ENTITY_MASKED_MIMIC_GORIYA -> 0x04;
            case ENTITY_THREE_OF_A_KIND -> 0x04;
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE -> RoomEntityCombatRules
                    .OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_MAD_BOMBER -> MAD_BOMBER_CONTACT_DAMAGE;
            case ENTITY_BOMBER -> BOMBER_CONTACT_DAMAGE;
            case ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE -> BOMBITE_CONTACT_DAMAGE;
            case ENTITY_GIBDO -> GIBDO_CONTACT_DAMAGE;
            case ENTITY_POLS_VOICE -> 0x08;
            case ENTITY_LIKE_LIKE -> 0;
            case ENTITY_ANTI_FAIRY -> ANTI_FAIRY_CONTACT_DAMAGE;
            case ENTITY_HIDING_GHINI, ENTITY_GIANT_GHINI, ENTITY_GHINI -> GHINI_CONTACT_DAMAGE;
            case ENTITY_HARDHAT_BEETLE -> HARDHAT_CONTACT_DAMAGE;
            case ENTITY_SPIKED_BEETLE -> 0x04;
            case ENTITY_WIZROBE -> WIZROBE_CONTACT_DAMAGE;
            case ENTITY_SPIKE_TRAP -> SPIKE_TRAP_CONTACT_DAMAGE;
            case ENTITY_CRYSTAL_SWITCH -> CRYSTAL_SWITCH_CONTACT_DAMAGE;
            case ENTITY_PINCER -> PINCER_CONTACT_DAMAGE;
            case ENTITY_WITCH_RAT -> 0x04;
            case ENTITY_MOLDORM -> 0x08;
            case 0xE4 -> 0x08;
            default -> 0;
        };
    }

    static int initialHealth(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE,
                    ENTITY_OCTOROK,
                    ENTITY_PEAHAT,
                    ENTITY_WATER_TEKTITE,
                    ENTITY_STAR,
                    ENTITY_BLOOPER,
                    ENTITY_WINGED_OCTOROK,
                    ENTITY_GOOMBA,
                    ENTITY_SNAKE,
                    ENTITY_POKEY,
                    ENTITY_PIRANHA_PLANT,
                    ENTITY_ZORA,
                    ENTITY_ZOMBIE -> OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_BUZZ_BLOB -> 0x04;
            case ENTITY_SAND_CRAB -> 0x02;
            case ENTITY_FISH -> OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_CROW -> CROW_INITIAL_HEALTH;
            case ENTITY_CUCCO -> 0x01;
            case ENTITY_GOPONGA_FLOWER, ENTITY_GIANT_GOPONGA_FLOWER -> 0x04;
            case ENTITY_BOO_BUDDY -> BOO_BUDDY_INITIAL_HEALTH;
            case ENTITY_ARMOS_STATUE -> 0x04;
            case ENTITY_ARMOS_KNIGHT -> 0x0C;
            case ENTITY_HINOX -> 0x08;
            case ENTITY_ROLLING_BONES -> 0x08;
            case ENTITY_ROLLING_BONES_BAR -> 0x01;
            case ENTITY_PAIRODD -> PAIRODD_INITIAL_HEALTH;
            case ENTITY_MOBLIN,
                    ENTITY_IRON_MASK,
                    ENTITY_MOBLIN_SWORD,
                    ENTITY_TEKTITE,
                    ENTITY_LEEVER,
                    ENTITY_STALFOS_AGGRESSIVE,
                    ENTITY_STALFOS_EVASIVE,
                    ENTITY_MINI_MOLDORM -> MOBLIN_INITIAL_HEALTH;
            case ENTITY_MIMIC -> 0x04;
            case ENTITY_MASKED_MIMIC_GORIYA -> 0x02;
            case ENTITY_THREE_OF_A_KIND -> 0x01;
            case ENTITY_PINCER -> 0x02;
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE -> RoomEntityCombatRules
                    .OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_ZOL -> ZOL_INITIAL_HEALTH;
            case ENTITY_HIDING_ZOL -> HIDING_ZOL_INITIAL_HEALTH;
            case ENTITY_GEL -> GEL_INITIAL_HEALTH;
            case ENTITY_GIBDO -> GIBDO_INITIAL_HEALTH;
            case ENTITY_POLS_VOICE -> 0x04;
            case ENTITY_LIKE_LIKE -> LIKE_LIKE_INITIAL_HEALTH;
            case ENTITY_MAD_BOMBER -> MAD_BOMBER_INITIAL_HEALTH;
            case ENTITY_BOMBER -> BOMBER_INITIAL_HEALTH;
            case ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE -> BOMBITE_INITIAL_HEALTH;
            case ENTITY_ANTI_FAIRY -> ANTI_FAIRY_INITIAL_HEALTH;
            case ENTITY_HIDING_GHINI, ENTITY_GIANT_GHINI, ENTITY_GHINI -> GHINI_INITIAL_HEALTH;
            case ENTITY_HARDHAT_BEETLE -> HARDHAT_INITIAL_HEALTH;
            case ENTITY_SPIKED_BEETLE -> 0x02;
            case ENTITY_WIZROBE -> WIZROBE_INITIAL_HEALTH;
            case ENTITY_SPIKE_TRAP -> SPIKE_TRAP_INITIAL_HEALTH;
            case ENTITY_CRYSTAL_SWITCH -> CRYSTAL_SWITCH_INITIAL_HEALTH;
            case ENTITY_WITCH_RAT -> 0x01;
            case ENTITY_MOLDORM -> 0x04;
            case 0xE4 -> 0x08;
            default -> 0;
        };
    }
}
