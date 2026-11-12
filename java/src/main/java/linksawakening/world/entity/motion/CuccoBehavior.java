package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.CuccoMotion.*;

import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityCombatRules;

/** Focused behavior using the state owned by CuccoMotion. */
final class CuccoBehavior {
    static CuccoMotion.Update advance(CuccoMotion owner, CuccoFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int transitionCountdown = inputs.transitionCountdown();
        int flashCountdown = inputs.flashCountdown();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkZ = inputs.linkZ();
        boolean linkInteractive = inputs.linkInteractive();
        boolean powerBraceletHeld = inputs.powerBraceletHeld();
        boolean linkAttackStepActive = inputs.linkAttackStepActive();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        boolean indoorRoom = inputs.indoorRoom();
        boolean marinFollowing = inputs.marinFollowing();
        int transitionSequenceCounter = inputs.transitionSequenceCounter();
        return owner.advance(new CuccoInteractionFrame(
                entity,
                frameCounter,
                transitionCountdown,
                flashCountdown,
                linkEntityX,
                linkEntityY,
                linkZ,
                linkInteractive,
                powerBraceletHeld,
                linkAttackStepActive,
                randomByteSupplier,
                backgroundCollision,
                indoorRoom,
                marinFollowing,
                transitionSequenceCounter,
                true));
    }

    static CuccoMotion.Update advance(CuccoMotion owner, CuccoInteractionFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int transitionCountdown = inputs.transitionCountdown();
        int flashCountdown = inputs.flashCountdown();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkZ = inputs.linkZ();
        boolean linkInteractive = inputs.linkInteractive();
        boolean powerBraceletHeld = inputs.powerBraceletHeld();
        boolean linkAttackStepActive = inputs.linkAttackStepActive();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        boolean indoorRoom = inputs.indoorRoom();
        boolean marinFollowing = inputs.marinFollowing();
        int transitionSequenceCounter = inputs.transitionSequenceCounter();
        boolean angrySpawnSlotAvailable = inputs.angrySpawnSlotAvailable();
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Cucco motion entity");
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Cucco random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        int frame = frameCounter & 0xFF;
        int countdown = transitionCountdown & 0xFF;
        int nextFlashCountdown = flashCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int z = entity.z() & 0xFF;
        int variant = owner.spriteVariant[slot];
        int renderedVariant = variant + (owner.direction[slot] == 0 ? 2 : 0);
        boolean unloaded = false;
        boolean boomerangSound = false;
        int waveSoundId = -1;
        int dialogTableId = -1;
        int dialogLowId = -1;
        CuccoMotion.SpawnRequest spawnRequest = null;

        // $05:452A-453B mirrors only hActiveEntitySpriteVariant for the
        // current draw. SetEntitySpriteVariant later writes the next frame's
        // unmirrored WRAM variant; keep the two values separate.

        boolean landed = false;
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

        RoomEntity collisionEntity = EntitySnapshots.withMotion(
                entity,
                x,
                y,
                variant,
                entity.entityFlipAttribute(),
                z,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
        boolean linkCollision = linkInteractive
                && (linkZ & 0xFF) == 0
                && owner.state[slot] != 3
                && RoomEntityCombatRules.overlapsLink(collisionEntity, linkEntityX, linkEntityY);
        if (linkCollision && !linkAttackStepActive && powerBraceletHeld) {
            RoomEntity updated = CuccoMotion.withPosition(entity, x, y, z, renderedVariant);
            return new CuccoMotion.Update(
                    updated, countdown, nextFlashCountdown, true, false, false, -1, dialogTableId, dialogLowId, null);
        }

        switch (owner.state[slot]) {
            case 0 -> {
                variant = 0;
                if (countdown == 0) {
                    int random = randomByteSupplier.getAsInt() & 0x07;
                    owner.speedX[slot] = SPEED_BY_RANDOM[random];
                    owner.direction[slot] = random & 0x04;
                    owner.speedY[slot] = SPEED_BY_RANDOM[randomByteSupplier.getAsInt() & 0x07];
                    countdown = 0x30 + (randomByteSupplier.getAsInt() & 0x1F);
                    owner.state[slot] = 1;
                }
            }
            case 1 -> {
                CuccoMotion.Position moved = owner.move(entity, x, y, backgroundCollision);
                x = moved.x();
                y = moved.y();
                if (landed) {
                    if (countdown == 0) {
                        countdown = 0x30;
                        owner.state[slot] = 0;
                        variant = CuccoMotion.withUnsignedByte(variant);
                        break;
                    }
                    owner.speedZ[slot] = 0x05;
                    z = (z + 1) & 0xFF;
                }
                variant = (frame >>> 3) & 0x01;
            }
            case 2 -> {
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
                if (owner.privateState1[slot] == 0x23
                        && !indoorRoom
                        && (frame & 0x0F) == 0
                        && angrySpawnSlotAvailable) {
                    int spawnIndex = randomByteSupplier.getAsInt() & 0x0F;
                    int spawnX = SPAWN_X[spawnIndex];
                    int spawnY = SPAWN_Y[spawnIndex];
                    CuccoMotion.Vector spawnVector =
                            CuccoMotion.vectorTowardsLink(spawnX, spawnY, 0x10, linkEntityX, linkEntityY, 0x18);
                    spawnRequest = new CuccoMotion.SpawnRequest(spawnX, spawnY, 0x10, spawnVector.x(), spawnVector.y());
                }
            }
            case 3 -> {
                CuccoMotion.Position moved = owner.move(entity, x, y, null);
                x = moved.x();
                y = moved.y();
                if (x >= 0xA9 || ((y - z) & 0xFF) >= 0x91) {
                    unloaded = true;
                }
                variant = (frame >>> 1) & 0x01;
                owner.direction[slot] = CuccoMotion.signedByte(owner.speedX[slot]) < 0 ? 1 : 0;
                boomerangSound = true;
            }
            default -> throw new IllegalStateException("Invalid Cucco state: " + owner.state[slot]);
        }

        owner.spriteVariant[slot] = variant & 1;
        RoomEntity updated = CuccoMotion.withPosition(entity, x, y, z, renderedVariant);
        if (spawnRequest != null) {
            waveSoundId = CUCCO_HURT_WAVE_SFX;
        }
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
}
