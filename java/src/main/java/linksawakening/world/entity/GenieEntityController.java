package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** ROM Genie encounter phases, sharing the owning room slot table. */
public final class GenieEntityController {
    final int[] genieActiveState = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] geniePrivateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] geniePrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] geniePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] geniePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] geniePrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] geniePrivateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] genieDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] genieDeathInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final int[] genieSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] genieSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] genieSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] genieSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] genieSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] genieSpeedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] genieImpactRegisteredThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    int genieControllerSlot = -1;
    int genieSharedBodySpeedX;
    int genieSharedBodySpeedY;

    final GenieEntityControllerServices runtime;

    GenieEntityController(GenieEntityControllerServices runtime) {
        this.runtime = runtime;
    }

    public void advanceGenieJar(RoomEntity jarEntity, int linkEntityY) {
        GenieJarBehavior.advanceGenieJar(this, jarEntity, linkEntityY);
    }

    RoomEntity advanceGenieJarAttract(RoomEntity jar) {
        int slot = jar.slot();
        if (geniePrivateState3[slot] == 0) {
            EnemyRecoilMotion.Vector towardCenter =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(jar.x(), jar.y(), jar.z(), 0x50, 0x48, 0x08);
            genieSpeedX[slot] = towardCenter.x() & 0xFF;
            genieSpeedY[slot] = towardCenter.y() & 0xFF;
            if ((((jar.x() - 0x50 + 0x02) & 0xFF) < 0x04) && (((jar.y() - 0x48 + 0x02) & 0xFF) < 0x04)) {
                geniePrivateState3[slot] = (geniePrivateState3[slot] + 1) & 0xFF;
            }
        }
        int x = addFallingSpeedToPosition(jar.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(jar.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot);
        return withPositionAndVariant(jar, x, y, jar.spriteVariant());
    }

    public void advanceGenieAppearance(RoomEntity appearance) {
        GenieBodyBehavior.advanceGenieAppearance(this, appearance);
    }

    public void advanceGenieBody(
            RoomEntity body, int frameCounter, int linkEntityX, int linkEntityY, IntSupplier randomByteSupplier) {
        GenieBodyBehavior.advanceGenieBody(this, body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier);
    }

    int spawnGenieAimedFireball(RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) {
        return GenieBodyBehavior.spawnGenieAimedFireball(this, body, bodySlot, linkEntityX, linkEntityY);
    }

    public void advanceGenieDestruction(RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) {
        GenieProjectileBehavior.advanceGenieDestruction(this, genie, frameCounter, randomByteSupplier);
    }

    public void advanceGenieSideProjectile(RoomEntity projectile, int frameCounter) {
        GenieProjectileBehavior.advanceGenieSideProjectile(this, projectile, frameCounter);
    }

    public void advanceGenieFireball(RoomEntity fireball, int frameCounter) {
        int slot = fireball.slot();
        int x = addFallingSpeedToPosition(fireball.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(fireball.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot);
        int z = addFallingSpeedToPosition(fireball.z(), genieSpeedZ[slot], genieSpeedZAccumulator, slot);
        genieSpeedZ[slot] = (genieSpeedZ[slot] - 2) & 0xFF;
        if ((z & 0x80) != 0) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.slots()[slot] = withZ(withPositionAndVariant(fireball, x, y, (frameCounter >>> 3) & 1), z);
    }

    int spawnGenieBody(GenieMotion.BodySpawnRequest request) {
        return spawnGenieEntity(
                request.x(),
                request.y(),
                request.privateState1(),
                request.transitionCountdown(),
                request.health(),
                GenieMotion.INITIAL_PHYSICS_FLAGS,
                GenieMotion.INITIAL_HITBOX_FLAGS);
    }

    int spawnGenieEntity(
            int x, int y, int privateState1, int transitionCountdown, int health, int physicsFlags, int hitboxFlags) {
        return GenieProjectileBehavior.spawnGenieEntity(
                this, x, y, privateState1, transitionCountdown, health, physicsFlags, hitboxFlags);
    }

    RoomEntity registerGenieJarImpact(RoomEntity jar) {
        return GenieJarBehavior.registerGenieJarImpact(this, jar);
    }

    public int geniePrivateCountdown1(int slot) {
        return geniePrivateCountdown1[slot];
    }

    public void decrementGeniePrivateCountdown1(int slot) {
        geniePrivateCountdown1[slot]--;
    }

    public int geniePrivateCountdown2(int slot) {
        return geniePrivateCountdown2[slot];
    }

    public void decrementGeniePrivateCountdown2(int slot) {
        geniePrivateCountdown2[slot]--;
    }

    public int geniePrivateState1(int slot) {
        return geniePrivateState1[slot];
    }

    public int geniePrivateState4(int slot) {
        return geniePrivateState4[slot];
    }

    public void setGenieControllerSlot(int value) {
        genieControllerSlot = value;
    }

    public void setGenieSharedBodySpeedX(int value) {
        genieSharedBodySpeedX = value;
    }

    public int genieSpeedX(int slot) {
        return genieSpeedX[slot];
    }

    public void setGenieSharedBodySpeedY(int value) {
        genieSharedBodySpeedY = value;
    }

    public int genieSpeedY(int slot) {
        return genieSpeedY[slot];
    }

    public int genieActiveState(int slot) {
        return genieActiveState[slot];
    }

    void setGenieActiveState(int slot, int value) {
        genieActiveState[slot] = value;
    }

    void setGeniePrivateState1(int slot, int value) {
        geniePrivateState1[slot] = value;
    }

    void setGeniePrivateState2(int slot, int value) {
        geniePrivateState2[slot] = value;
    }

    int geniePrivateState3(int slot) {
        return geniePrivateState3[slot];
    }

    void setGeniePrivateState3(int slot, int value) {
        geniePrivateState3[slot] = value;
    }

    void setGeniePrivateState4(int slot, int value) {
        geniePrivateState4[slot] = value;
    }

    void setGeniePrivateCountdown1(int slot, int value) {
        geniePrivateCountdown1[slot] = value;
    }

    void setGeniePrivateCountdown2(int slot, int value) {
        geniePrivateCountdown2[slot] = value;
    }

    void setGenieDirection(int slot, int value) {
        genieDirection[slot] = value;
    }

    void setGenieDeathInitialized(int slot, boolean value) {
        genieDeathInitialized[slot] = value;
    }

    void setGenieSpeedX(int slot, int value) {
        genieSpeedX[slot] = value;
    }

    void setGenieSpeedY(int slot, int value) {
        genieSpeedY[slot] = value;
    }

    int genieSpeedZ(int slot) {
        return genieSpeedZ[slot];
    }

    void setGenieSpeedZ(int slot, int value) {
        genieSpeedZ[slot] = value;
    }

    void setGenieSpeedXAccumulator(int slot, int value) {
        genieSpeedXAccumulator[slot] = value;
    }

    void setGenieSpeedYAccumulator(int slot, int value) {
        genieSpeedYAccumulator[slot] = value;
    }

    void setGenieSpeedZAccumulator(int slot, int value) {
        genieSpeedZAccumulator[slot] = value;
    }

    int genieControllerSlot() {
        return genieControllerSlot;
    }

    void beginFrame() {
        java.util.Arrays.fill(genieImpactRegisteredThisFrame, false);
    }
}
