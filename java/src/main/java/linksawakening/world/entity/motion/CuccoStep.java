package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.CuccoMotion.*;

import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityCombatRules;

/** Per-invocation scratch for CuccoBehavior.advance; values are assigned in source order. */
final class CuccoStep {
    private linksawakening.world.entity.motion.CuccoMotion owner;
    private linksawakening.world.entity.motion.CuccoInteractionFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int transitionCountdown;
    private int flashCountdown;
    private int linkEntityX;
    private int linkEntityY;
    private int linkZ;
    private boolean linkInteractive;
    private boolean powerBraceletHeld;
    private boolean linkAttackStepActive;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private boolean indoorRoom;
    private boolean marinFollowing;
    private int transitionSequenceCounter;
    private boolean angrySpawnSlotAvailable;
    private int slot;
    private int frame;
    private int countdown;
    private int nextFlashCountdown;
    private int x;
    private int y;
    private int z;
    private int variant;
    private int renderedVariant;
    private boolean unloaded;
    private boolean boomerangSound;
    private int waveSoundId;
    private int dialogTableId;
    private int dialogLowId;
    private linksawakening.world.entity.motion.CuccoMotion.SpawnRequest spawnRequest;
    private boolean landed;
    private linksawakening.world.entity.RoomEntity collisionEntity;
    private boolean linkCollision;
    private linksawakening.world.entity.RoomEntity updated;

    CuccoStep(
            linksawakening.world.entity.motion.CuccoMotion owner,
            linksawakening.world.entity.motion.CuccoInteractionFrame inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    linksawakening.world.entity.motion.CuccoMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!advanceHeightAndAnimation()) return completedResult;
        if (!advanceBehavior()) return completedResult;

        return new CuccoMotion.Update(
                updated,
                countdown,
                nextFlashCountdown,
                false,
                unloaded,
                boomerangSound,
                waveSoundId,
                dialogTableId,
                dialogLowId,
                spawnRequest);
    }

    private linksawakening.world.entity.motion.CuccoMotion.Update completedResult;

    private boolean advanceState0() {
        variant = 0;
        if (countdown == 0) {
            int random = randomByteSupplier.getAsInt() & 0x07;
            owner.speedX[slot] = SPEED_BY_RANDOM[random];
            owner.direction[slot] = random & 0x04;
            owner.speedY[slot] = SPEED_BY_RANDOM[randomByteSupplier.getAsInt() & 0x07];
            countdown = 0x30 + (randomByteSupplier.getAsInt() & 0x1F);
            owner.state[slot] = 1;
        }

        return true;
    }

    private boolean advanceState1() {
        CuccoMotion.Position moved = owner.move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();
        if (landed) {
            if (countdown == 0) {
                countdown = 0x30;
                owner.state[slot] = 0;
                variant = CuccoMotion.withUnsignedByte(variant);
                return true;
            }
            owner.speedZ[slot] = 0x05;
            z = (z + 1) & 0xFF;
        }
        variant = (frame >>> 3) & 0x01;

        return true;
    }

    private boolean advanceState2() {
        if ((((frame ^ slot) & 0x1F) == 0) && z == 0) {
            CuccoMotion.Vector vector = CuccoMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x0C);
            owner.speedY[slot] = CuccoMotion.negate(vector.y());
            owner.speedX[slot] = CuccoMotion.negate(vector.x());
        }
        CuccoMotion.Position moved = owner.move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();
        variant = (frame >>> 1) & 0x01;
        owner.direction[slot] = CuccoMotion.signedByte((linkEntityX - x) & 0xFF) < 0 ? 0 : 1;
        if (owner.privateState1[slot] == 0x23 && !indoorRoom && (frame & 0x0F) == 0 && angrySpawnSlotAvailable) {
            int spawnIndex = randomByteSupplier.getAsInt() & 0x0F;
            int spawnX = SPAWN_X[spawnIndex];
            int spawnY = SPAWN_Y[spawnIndex];
            CuccoMotion.Vector spawnVector =
                    CuccoMotion.vectorTowardsLink(spawnX, spawnY, 0x10, linkEntityX, linkEntityY, 0x18);
            spawnRequest = new CuccoMotion.SpawnRequest(spawnX, spawnY, 0x10, spawnVector.x(), spawnVector.y());
        }

        return true;
    }

    private boolean advanceState3() {
        CuccoMotion.Position moved = owner.move(entity, x, y, null);
        x = moved.x();
        y = moved.y();
        if (x >= 0xA9 || ((y - z) & 0xFF) >= 0x91) {
            unloaded = true;
        }
        variant = (frame >>> 1) & 0x01;
        owner.direction[slot] = CuccoMotion.signedByte(owner.speedX[slot]) < 0 ? 1 : 0;
        boomerangSound = true;

        return true;
    }

    private boolean prepareFrame() {

        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        transitionCountdown = inputs.transitionCountdown();
        flashCountdown = inputs.flashCountdown();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkZ = inputs.linkZ();
        linkInteractive = inputs.linkInteractive();
        powerBraceletHeld = inputs.powerBraceletHeld();
        linkAttackStepActive = inputs.linkAttackStepActive();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        indoorRoom = inputs.indoorRoom();
        marinFollowing = inputs.marinFollowing();
        transitionSequenceCounter = inputs.transitionSequenceCounter();
        angrySpawnSlotAvailable = inputs.angrySpawnSlotAvailable();
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Cucco motion entity");
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Cucco random-byte supplier cannot be null");
        }

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        frame = frameCounter & 0xFF;
        countdown = transitionCountdown & 0xFF;
        nextFlashCountdown = flashCountdown & 0xFF;
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;
        z = entity.z() & 0xFF;
        variant = owner.spriteVariant[slot];
        renderedVariant = variant + (owner.direction[slot] == 0 ? 2 : 0);
        unloaded = false;
        boomerangSound = false;
        waveSoundId = -1;
        dialogTableId = -1;
        dialogLowId = -1;
        spawnRequest = null;

        // $05:452A-453B mirrors only hActiveEntitySpriteVariant for the
        // current draw. SetEntitySpriteVariant later writes the next frame's
        // unmirrored WRAM variant; keep the two values separate.

        landed = false;
        return true;
    }

    private boolean advanceHeightAndAnimation() {

        if (owner.state[slot] != 3) {
            z = CuccoMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
            landed = (z & 0x80) != 0;
            if (landed) {
                z = 0;
                owner.speedZ[slot] = 0;
            }
        }

        if (nextFlashCountdown != 0) {
            if (nextFlashCountdown == 0x08 && marinFollowing) {
                nextFlashCountdown--;
                if ((transitionSequenceCounter & 0xFF) == 0x04) {
                    if ((randomByteSupplier.getAsInt() & 0x3F) == 0) {
                        dialogTableId = 0x02;
                        dialogLowId = 0x76;
                    } else {
                        dialogTableId = 0x00;
                        dialogLowId = 0x8F;
                    }
                }
            }
            if (owner.privateState1[slot] != 0x23) {
                owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0xFF;
            }
            owner.state[slot] = 2;
        }

        collisionEntity = EntitySnapshots.withMotion(
                entity,
                x,
                y,
                variant,
                entity.entityFlipAttribute(),
                z,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
        linkCollision = linkInteractive
                && (linkZ & 0xFF) == 0
                && owner.state[slot] != 3
                && RoomEntityCombatRules.overlapsLink(collisionEntity, linkEntityX, linkEntityY);
        if (linkCollision && !linkAttackStepActive && powerBraceletHeld) {
            RoomEntity updated = CuccoMotion.withPosition(entity, x, y, z, renderedVariant);
            {
                completedResult = new CuccoMotion.Update(
                        updated,
                        countdown,
                        nextFlashCountdown,
                        true,
                        false,
                        false,
                        -1,
                        dialogTableId,
                        dialogLowId,
                        null);
                return false;
            }
        }
        return true;
    }

    private boolean advanceBehavior() {

        switch (owner.state[slot]) {
            case 0 -> {
                if (!advanceState0()) return false;
            }
            case 1 -> {
                if (!advanceState1()) return false;
            }
            case 2 -> {
                if (!advanceState2()) return false;
            }
            case 3 -> {
                if (!advanceState3()) return false;
            }
            default -> throw new IllegalStateException("Invalid Cucco state: " + owner.state[slot]);
        }

        owner.spriteVariant[slot] = variant & 1;
        updated = CuccoMotion.withPosition(entity, x, y, z, renderedVariant);
        if (spawnRequest != null) {
            waveSoundId = CUCCO_HURT_WAVE_SFX;
        }
        return true;
    }
}
