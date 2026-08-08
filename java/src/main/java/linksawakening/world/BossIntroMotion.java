package linksawakening.world;

/** Bank-$00 BossIntro's delayed one-shot music and dialog request. */
final class BossIntroMotion {
    private static final int MUSIC_BOSS = 0x19;
    private static final int MUSIC_MINIBOSS = 0x50;
    private static final int ENTITY_DESERT_LANMOLA = 0x87;
    private static final int ENTITY_GRIM_CREEPER = 0xBC;
    private static final int MAP_FACE_SHRINE = 0x05;
    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int[] BOSS_INTRO_DIALOGS = {
        0xB0, 0xB4, 0xB1, 0xB2, 0xB3, 0xB6, 0xBA, 0xBC, 0xB8
    };

    record Update(int musicTrack, int dialogTableId, int dialogLowId) {
        static Update none() {
            return new Update(-1, -1, -1);
        }
    }

    private int delay = 0x20;
    private boolean didIntro;

    Update advance(int options1, int entityType, int mapId, int transitionSequenceCounter) {
        if (delay > 0) {
            delay--;
            return Update.none();
        }
        if (didIntro) {
            return Update.none();
        }
        didIntro = true;

        int normalizedOptions = options1 & 0xFF;
        int normalizedType = entityType & 0xFF;
        int normalizedMap = mapId & 0xFF;
        int musicTrack = (normalizedOptions & 0x04) != 0
            ? MUSIC_MINIBOSS : MUSIC_BOSS;
        if ((transitionSequenceCounter & 0xFF) != 0x04) {
            return new Update(musicTrack, -1, -1);
        }

        int dialogLowId = -1;
        if (normalizedType == ENTITY_DESERT_LANMOLA) {
            dialogLowId = 0xDA;
        } else if (normalizedType == ENTITY_GRIM_CREEPER) {
            dialogLowId = 0x26;
        } else if ((normalizedOptions & 0x04) == 0
            && normalizedMap != MAP_COLOR_DUNGEON
            && normalizedMap != MAP_FACE_SHRINE
            && normalizedMap < BOSS_INTRO_DIALOGS.length) {
            dialogLowId = BOSS_INTRO_DIALOGS[normalizedMap];
        }
        return new Update(musicTrack, dialogLowId < 0 ? -1 : 0, dialogLowId);
    }

    void reset() {
        delay = 0x20;
        didIntro = false;
    }

    int delay() {
        return delay;
    }

    boolean didIntro() {
        return didIntro;
    }
}
