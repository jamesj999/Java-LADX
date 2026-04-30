package linksawakening.audio.sfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SoundEffectCatalog {
    private static final String[] JINGLE_NAMES = {
            "JINGLE_TREASURE_FOUND", "JINGLE_PUZZLE_SOLVED", "JINGLE_ENEMY_HIT",
            "JINGLE_CHARGING_SWORD", "JINGLE_POWDER", "JINGLE_GENIE_APPEAR",
            "JINGLE_SWORD_POKING", "JINGLE_FALL_DOWN", "JINGLE_BUMP",
            "JINGLE_MOVE_SELECTION", "JINGLE_STRONG_BUMP", "JINGLE_REVOLVING_DOOR",
            "JINGLE_FEATHER_JUMP", "JINGLE_WATER_SPLASH", "JINGLE_SWIM",
            "JINGLE_UNKNOWN_10", "JINGLE_OPEN_INVENTORY", "JINGLE_CLOSE_INVENTORY",
            "JINGLE_VALIDATE", "JINGLE_GOT_HEART", "JINGLE_DIALOG_BREAK",
            "JINGLE_SHIELD_TING", "JINGLE_GOT_POWER_UP", "JINGLE_ITEM_FALLING",
            "JINGLE_NEW_HEART", "JINGLE_FAIRY_HEAL", "JINGLE_DUNGEON_WARP_APPEAR",
            "JINGLE_DUNGEON_WARP", "JINGLE_WRONG_ANSWER", "JINGLE_FOREST_LOST",
            "JINGLE_GENIE_DISAPPEAR", "JINGLE_BOUNCE", "JINGLE_SEAGULL",
            "JINGLE_TARIN_BEE_BUZZ", "JINGLE_DUNGEON_OPENED", "JINGLE_JUMP",
            "JINGLE_OVERWORLD_WARP_HOLE", "JINGLE_DISAPPEAR", "JINGLE_WALRUS",
            "JINGLE_STALFOS_COLLAPSE", "JINGLE_SLIME_EEL_PULL", "JINGLE_DODONGO_EAT_BOMB",
            "JINGLE_INSTRUMENT_WARP", "JINGLE_MANBO_WARP", "JINGLE_GHOST_PRESENCE",
            "JINGLE_EAGLES_TOWER_ROTATE", "JINGLE_POOF", "JINGLE_EAGLE_SKID",
            "JINGLE_GRIM_CREEPER_BATS", "JINGLE_HOT_HEAD_SPLASH", "JINGLE_LINK_DAZED",
            "JINGLE_INSTRUMENTS_APPEAR", "JINGLE_SHADOW_MOVE", "JINGLE_SHADOW_AGAHNIM_DEFEAT",
            "JINGLE_SHADOW_ZOL_HURT", "JINGLE_SHADOW_MOLDORM_ROAM", "JINGLE_GANON_TRIDENT_APPEAR",
            "JINGLE_UNKNOWN_3A", "JINGLE_SWORD_BEAM", "JINGLE_PAIRODD_TELEPORT",
            "JINGLE_DETHI_HANDS", "JINGLE_URCHIN_PUSH", "JINGLE_FLYING_TILE",
            "JINGLE_FACADE_HOLE", "JINGLE_UNKNOWN_41"
    };

    private static final String[] WAVE_NAMES = {
            "WAVE_SFX_SEASHELL", "WAVE_SFX_LIFT_UP", "WAVE_SFX_LINK_HURT",
            "WAVE_SFX_LOW_HEARTS", "WAVE_SFX_RUPEE", "WAVE_SFX_HEART_PICKED_UP",
            "WAVE_SFX_BOSS_HURT", "WAVE_SFX_LINK_DIES", "WAVE_SFX_OCARINA_BALLAD",
            "WAVE_SFX_OCARINA_FROG", "WAVE_SFX_OCARINA_MAMBO", "WAVE_SFX_LINK_FALL",
            "WAVE_SFX_ANGLER_DASH", "WAVE_SFX_FLOOR_SWITCH", "WAVE_SFX_TEXT_PRINT",
            "WAVE_SFX_BOSS_DEATH_CRY", "WAVE_SFX_POWER_HIT", "WAVE_SFX_UNKNOWN_12",
            "WAVE_SFX_CUCCO_HURT", "WAVE_SFX_MONKEY", "WAVE_SFX_OCARINA_NOSONG",
            "WAVE_SFX_BOSS_GROWL", "WAVE_SFX_WIND_FISH_CRY", "WAVE_SFX_CHAIN_CHOMP",
            "WAVE_SFX_OWL_HOOT", "WAVE_SFX_ROLLING_SPIKE_BAR", "WAVE_SFX_COMPASS",
            "WAVE_SFX_ROVER_CRY", "WAVE_SFX_UNKNOWN_1D", "WAVE_SFX_L2_SWORD_APPEAR",
            "WAVE_SFX_WIND_FISH_MORPH", "WAVE_SFX_FLYING_WITH_SPOUT",
            "WAVE_SFX_SHADOW_DISPERSE", "WAVE_SFX_AGAHNIM_CHARGE",
            "WAVE_SFX_SHADOW_CHANGE_FORM"
    };

    private static final String[] NOISE_NAMES = {
            "NOISE_SFX_UNKNOWN_1", "NOISE_SFX_SWORD_SWING_A", "NOISE_SFX_SPIN_ATTACK",
            "NOISE_SFX_DOOR_UNLOCKED", "NOISE_SFX_CUT_GRASS", "NOISE_SFX_STAIRS",
            "NOISE_SFX_FOOTSTEP", "NOISE_SFX_BEAMOS_LASER", "NOISE_SFX_POT_SMASHED",
            "NOISE_SFX_WHOOSH", "NOISE_SFX_HOOKSHOT", "NOISE_SFX_EXPLOSION",
            "NOISE_SFX_MAGIC_ROD", "NOISE_SFX_SHOVEL_DIG", "NOISE_SFX_SEA_WAVES",
            "NOISE_SFX_DOOR_CLOSED", "NOISE_SFX_RUMBLE", "NOISE_SFX_BURSTING_FLAME",
            "NOISE_SFX_ENEMY_DESTROYED", "NOISE_SFX_SWORD_SWING_B", "NOISE_SFX_SWORD_SWING_C",
            "NOISE_SFX_DRAW_SHIELD", "NOISE_SFX_CLINK", "NOISE_SFX_SWORD_SWING_D",
            "NOISE_SFX_PING", "NOISE_SFX_BOSS_EXPLOSION", "NOISE_SFX_MOLDORM_ROAM",
            "NOISE_SFX_BUZZ_BLOB_ELECTROCUTE", "NOISE_SFX_OPEN_D4_D7", "NOISE_SFX_UNKNOWN_1E",
            "NOISE_SFX_BLACK_HOLE_ACTIVE", "NOISE_SFX_TRENDY_CRANE", "NOISE_SFX_SILENT",
            "NOISE_SFX_EVIL_EAGLE_FLY", "NOISE_SFX_LANMOLA_BURROW", "NOISE_SFX_WALRUS_SPLASH",
            "NOISE_SFX_D7_PILLAR_COLLAPSE", "NOISE_SFX_ELECTRIC_BEAM", "NOISE_SFX_WEAPON_SWING",
            "NOISE_SFX_GENIE_FIREBALL", "NOISE_SFX_BREAK", "NOISE_SFX_OPEN_KEY_CAVERN",
            "NOISE_SFX_RUMBLE2", "NOISE_SFX_INSTRUMENT_WARP", "NOISE_SFX_BOOMERANG",
            "NOISE_SFX_OPEN_FACE_SHRINE", "NOISE_SFX_CUEBALL_SPLASH", "NOISE_SFX_EAGLE_LANDING",
            "NOISE_SFX_EAGLE_LIFT_UP", "NOISE_SFX_EAGLE_FEATHERS", "NOISE_SFX_L2_SWORD_SPARKS",
            "NOISE_SFX_WATERSPOUT", "NOISE_SFX_ISLAND_DISAPPEAR", "NOISE_SFX_AGAHNIM_FAKE_BALL_EXPLODE",
            "NOISE_SFX_SHADOW_DISPERSE", "NOISE_SFX_AGAHNIM_BALL", "NOISE_SFX_AGAHNIM_FAKE_BALL",
            "NOISE_SFX_GANON_FLYING_TRIDENT", "NOISE_SFX_KIRBY_INHALE", "NOISE_SFX_UNKNOWN_3C",
            "NOISE_SFX_FINAL_BOSS_EXPLOSION", "NOISE_SFX_SLIME_EEL_FLOOR_BREAK",
            "NOISE_SFX_MINIBOSS_FLEE", "NOISE_SFX_PHOTO"
    };

    private final List<SoundEffect> effects;

    public SoundEffectCatalog(List<SoundEffect> effects) {
        this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }

    public static SoundEffectCatalog fromRom(byte[] romData) {
        Objects.requireNonNull(romData, "romData");
        RomSoundEffectTables tables = new RomSoundEffectTables(romData);
        List<SoundEffect> effects = new ArrayList<>(
                JINGLE_NAMES.length + WAVE_NAMES.length + NOISE_NAMES.length);
        addEffects(effects, SoundEffectNamespace.JINGLE, JINGLE_NAMES, tables);
        addEffects(effects, SoundEffectNamespace.WAVE, WAVE_NAMES, tables);
        addEffects(effects, SoundEffectNamespace.NOISE, NOISE_NAMES, tables);
        return new SoundEffectCatalog(effects);
    }

    public List<SoundEffect> effects() {
        return effects;
    }

    public Optional<SoundEffect> find(SoundEffectNamespace namespace, int id) {
        Objects.requireNonNull(namespace, "namespace");
        return effects.stream()
                .filter(effect -> effect.namespace() == namespace && effect.id() == (id & 0xFF))
                .findFirst();
    }

    private static void addEffects(
            List<SoundEffect> effects,
            SoundEffectNamespace namespace,
            String[] names,
            RomSoundEffectTables tables) {
        for (int i = 0; i < names.length; i++) {
            int id = i + 1;
            int beginPtr = tables.tableBase(namespace, true) + i * 2;
            int continuePtr = tables.tableBase(namespace, false) + i * 2;
            int beginHandler = tables.handlerAddress(namespace, id, true);
            int continueHandler = tables.handlerAddress(namespace, id, false);
            String source = "begin=" + romAddr(RomSoundEffectTables.SFX_BANK, beginPtr)
                    + "→" + romAddr(RomSoundEffectTables.SFX_BANK, beginHandler)
                    + System.lineSeparator()
                    + "continue=" + romAddr(RomSoundEffectTables.SFX_BANK, continuePtr)
                    + "→" + romAddr(RomSoundEffectTables.SFX_BANK, continueHandler);
            effects.add(new SoundEffect(namespace, id, names[i], source));
        }
    }

    private static String romAddr(int bank, int addr) {
        return String.format("%02X:%04X", bank & 0xFF, addr & 0xFFFF);
    }
}
