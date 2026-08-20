package linksawakening.world;

/** Source-shaped initial jar state for bank-$04's Genie handler. */
final class GenieMotion {
    static final int JAR_HEALTH_THRESHOLD = 0x03;
    static final int INITIAL_HEALTH = 0x06;
    static final int INITIAL_PHYSICS_FLAGS = 0x91;
    static final int JAR_HEALTH = 0x20;
    static final int JAR_PHYSICS_FLAGS = 0x81;
    static final int JAR_HITBOX_FLAGS = 0x80;
    static final int BODY_PRIVATE_STATE = 0x02;
    static final int BODY_TRANSITION_COUNTDOWN = 0x27;
    static final int BODY_HEALTH = 0x08;
    static final int NOISE_SFX_BREAK = 0x29;

    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] health = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] hitboxFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] transitioned = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record JarState(int privateState1, int health, int physicsFlags, int hitboxFlags) {
    }

    record BodySpawnRequest(int x, int y, int privateState1,
                            int transitionCountdown, int health) {
        BodySpawnRequest {
            x &= 0xFF;
            y &= 0xFF;
            privateState1 &= 0xFF;
            transitionCountdown &= 0xFF;
            health &= 0xFF;
        }
    }

    record RockSpawnRequest(int type, int x, int y, int spriteVariant,
                            int privateCountdown1, int physicsFlags) {
        RockSpawnRequest {
            type &= 0xFF;
            x &= 0xFF;
            y &= 0xFF;
            spriteVariant &= 0xFF;
            privateCountdown1 &= 0xFF;
            physicsFlags &= 0xFF;
        }
    }

    record Update(JarState jar, BodySpawnRequest bodySpawn, RockSpawnRequest rockSpawn,
                  boolean rockSpawnSucceeded, boolean jarSmashed,
                  boolean sourceUnloaded, int noiseSfx) {
    }

    /** Initializes the room-loaded Genie entity as a private-state-$00 jar. */
    JarState initialize(int slot) {
        validateSlot(slot);
        privateState1[slot] = 0;
        health[slot] = INITIAL_HEALTH;
        physicsFlags[slot] = INITIAL_PHYSICS_FLAGS;
        hitboxFlags[slot] = JAR_HITBOX_FLAGS;
        transitioned[slot] = false;
        initialized[slot] = true;
        return jarState(slot);
    }

    /** Advances only GenieState0Handler's normal-build jar branch. */
    Update advanceState0(int slot, int jarX, int jarY, int jarZ, int privateState4,
                         boolean bodySpawnSucceeded, boolean rockSpawnSucceeded) {
        validateSlot(slot);
        validateByte(jarX, "Genie jar X");
        validateByte(jarY, "Genie jar Y");
        validateByte(jarZ, "Genie jar Z");
        validateByte(privateState4, "Genie jar private state 4");
        if (!initialized[slot]) {
            initialize(slot);
        }

        if (transitioned[slot]) {
            return new Update(jarState(slot), null, null, false, false, false, -1);
        }

        if (privateState4 < JAR_HEALTH_THRESHOLD) {
            health[slot] = JAR_HEALTH;
            physicsFlags[slot] = JAR_PHYSICS_FLAGS;
            hitboxFlags[slot] = JAR_HITBOX_FLAGS;
            return new Update(jarState(slot), null, null, false, false, false, -1);
        }

        // SpawnNewEntity reports carry on exhaustion. Keep the source jar
        // intact when no body slot exists instead of writing through an
        // invalid DE index.
        if (!bodySpawnSucceeded) {
            return new Update(jarState(slot), null, null, false, false, false, -1);
        }

        BodySpawnRequest body = new BodySpawnRequest(
            jarX, jarY - 0x18, BODY_PRIVATE_STATE,
            BODY_TRANSITION_COUNTDOWN, BODY_HEALTH);
        RockSpawnRequest rock = new RockSpawnRequest(
            0x05, jarX, jarY - jarZ, 0x00, 0x0F, 0xC4);
        if (!rockSpawnSucceeded) {
            // SmashRock returns on its own slot failure, but GenieState0's
            // following noise write is unconditional in the source.
            return new Update(jarState(slot), body, rock, false, false, false,
                NOISE_SFX_BREAK);
        }

        transitioned[slot] = true;
        return new Update(jarState(slot), body, rock, true, true, true, NOISE_SFX_BREAK);
    }

    int privateState1(int slot) {
        validateSlot(slot);
        return privateState1[slot];
    }

    private JarState jarState(int slot) {
        return new JarState(privateState1[slot], health[slot],
            physicsFlags[slot], hitboxFlags[slot]);
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Genie slot out of range: " + slot);
        }
    }

    private static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }
}
