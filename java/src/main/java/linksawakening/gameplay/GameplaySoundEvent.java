package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffectNamespace;
import static linksawakening.audio.sfx.SoundEffectNamespace.JINGLE;
import static linksawakening.audio.sfx.SoundEffectNamespace.NOISE;
import static linksawakening.audio.sfx.SoundEffectNamespace.WAVE;

public enum GameplaySoundEvent {
    ROC_FEATHER_JUMP(JINGLE, 0x0D),
    ENEMY_BOMB_THROW(JINGLE, 0x08),
    LEDGE_FALL(JINGLE, 0x08),
    PIT_FALL(WAVE, 0x0C),
    OVERWORLD_WARP_HOLE(JINGLE, 0x25),
    DUNGEON_WARP(JINGLE, 0x1C),
    DUNGEON_WARP_APPEAR(JINGLE, 0x1B),
    STRONG_BUMP(JINGLE, 0x0B),
    BOUNCE(JINGLE, 0x20),
    ROLLING_SPIKE_BAR(WAVE, 0x1A),
    FOOTSTEP(NOISE, 0x07),
    WATER_SPLASH(JINGLE, 0x0E),
    ITEM_FALLING(JINGLE, 0x18),
    SWORD_SWING_A(NOISE, 0x02),
    SWORD_SWING_B(NOISE, 0x14),
    SWORD_SWING_C(NOISE, 0x15),
    SWORD_SWING_D(NOISE, 0x18),
    SWORD_FULLY_CHARGED(JINGLE, 0x04),
    SPIN_ATTACK(NOISE, 0x03),
    CUT_GRASS(NOISE, 0x05),
    INVENTORY_OPEN(JINGLE, 0x11),
    INVENTORY_CLOSE(JINGLE, 0x12),
    MENU_MOVE(JINGLE, 0x0A),
    MENU_VALIDATE(JINGLE, 0x13),
    SHIELD_DRAW(NOISE, 0x16),
    SHIELD_TING(JINGLE, 0x16),
    PUZZLE_SOLVED(JINGLE, 0x02),
    LINK_HURT(WAVE, 0x03),
    LINK_DIES(WAVE, 0x08),
    LOW_HEARTS(WAVE, 0x04),
    RUPEE_COUNT(WAVE, 0x05),
    HEART_REFILL(WAVE, 0x06),
    ENEMY_BUMP(JINGLE, 0x09),
    TREASURE_FOUND(JINGLE, 0x01),
    ENEMY_HIT(JINGLE, 0x03),
    SWORD_POKE(JINGLE, 0x07),
    ENEMY_BURNING(NOISE, 0x12),
    ENEMY_DESTROYED(NOISE, 0x13),
    BOSS_EXPLOSION(NOISE, 0x1A),
    MOLDORM_ROAM(NOISE, 0x1B),
    BEAMOS_LASER(NOISE, 0x08),
    SPIKE_TRAP_WHOOSH(NOISE, 0x0A),
    ARROW_SHOT(NOISE, 0x0A),
    HOOKSHOT(NOISE, 0x0B),
    BOOMERANG(NOISE, 0x2D),
    MAGIC_ROD(NOISE, 0x0D),
    MAGIC_POWDER(JINGLE, 0x05),
    MAGIC_POWDER_POOF(JINGLE, 0x2F),
    SHOVEL_DIG(NOISE, 0x0E),
    SECRET_SEASHELL(WAVE, 0x01),
    SWORD_BEAM(JINGLE, 0x3B),
    BOMB_EXPLOSION(NOISE, 0x0C),
    WRONG_ANSWER(JINGLE, 0x1D),
    DOOR_UNLOCKED(NOISE, 0x04),
    DOOR_CLOSED(NOISE, 0x10),
    STAIRS(NOISE, 0x06),
    OPEN_KEY_CAVERN(NOISE, 0x2A),
    DUNGEON_OPENED(JINGLE, 0x23),
    SWITCH_BLOCK_TOGGLE(WAVE, 0x0E),
    FLOOR_SWITCH(WAVE, 0x0E),
    OCARINA_BALLAD(WAVE, 0x09),
    OCARINA_MAMBO(WAVE, 0x0B),
    OCARINA_FROG(WAVE, 0x0A),
    OCARINA_NO_SONG(WAVE, 0x15),
    MANBO_WARP(JINGLE, 0x2C);

    private final SoundEffectNamespace namespace;
    private final int id;

    GameplaySoundEvent(SoundEffectNamespace namespace, int id) {
        this.namespace = namespace;
        this.id = id;
    }

    SoundEffectNamespace namespace() { return namespace; }
    int id() { return id; }

    // Named-only consumers receive the entity name for shared player/entity cues.
    static GameplaySoundEvent forEntityWrite(SoundEffectNamespace namespace, int id) {
        for (GameplaySoundEvent event : values()) {
            if (event.namespace == namespace && event.id == id) return event;
        }
        return null;
    }
}
