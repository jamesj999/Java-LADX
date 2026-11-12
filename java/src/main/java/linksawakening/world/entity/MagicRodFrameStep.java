package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.motion.*;

/** Per-invocation scratch for MagicRodBehavior.advanceMagicRodFireballEntity; values are assigned in source order. */
final class MagicRodFrameStep {
    private linksawakening.world.entity.EntityProjectileController owner;
    private int index;
    private linksawakening.world.entity.RoomEntity entity;
    private int frame;
    private boolean handlerInteractive;
    private boolean timersInteractive;
    private linksawakening.world.entity.EntityProjectileControllerServices runtime;
    private int slot;
    private boolean unloadOnHit;
    private int variant;
    private linksawakening.world.entity.RoomEntity moved;
    private linksawakening.world.entity.RoomEntityObjectQuery intersectionQuery;
    private linksawakening.world.entity.RoomEntityObjectSample object;
    private linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision objectCollision;
    private linksawakening.world.entity.RoomEntity positioned;

    MagicRodFrameStep(
            linksawakening.world.entity.EntityProjectileController owner,
            int index,
            linksawakening.world.entity.RoomEntity entity,
            int frame,
            boolean handlerInteractive,
            boolean timersInteractive) {
        this.owner = owner;
        this.index = index;
        this.entity = entity;
        this.frame = frame;
        this.handlerInteractive = handlerInteractive;
        this.timersInteractive = timersInteractive;
    }

    void run() {
        if (!advanceProjectile()) return;
        if (!resolveObjectCollision()) return;
    }

    private boolean advanceProjectile() {

        runtime = owner.runtime;
        slot = entity.slot();
        // The source calls func_003_75A2 before either the wall-fire branch or
        // ReturnIfNonInteractive. A fireball can therefore still deliver the
        // common target collision while Link is being transitioned.
        unloadOnHit = owner.collideMagicRodFireballWithEntities(entity, frame);
        if (unloadOnHit) {
            // $7782-$7795 unloads a normal projectile hit. Blocking targets
            // and existing wall flames retain a nonzero collision byte.
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return false;
        }
        variant = runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .advanceSpriteVariant(slot, frame);
        if (runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .privateCountdown1(slot)
                != 0) {
            if (runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .tickFireTransition(slot, timersInteractive)) {
                runtime.clearEntity(slot);
                return false;
            }
            EntitySpriteDefinition fireDefinition = runtime.spriteHandlers() == null
                    ? entity.spriteDefinition()
                    : runtime.spriteHandlers().forMagicRodFireState();
            runtime.slots()[index] = withDefinition(entity, fireDefinition, variant);
            return false;
        }

        // ReturnIfNonInteractive_03 is after the normal sprite write and
        // before movement/object lookup in the ROM helper.
        if (!handlerInteractive) {
            runtime.slots()[index] = withVariant(entity, variant);
            return false;
        }

        moved = runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .advancePosition(entity);
        intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        return true;
    }

    private boolean resolveObjectCollision() {

        objectCollision = object == null
                ? ProjectileObjectCollision.NONE
                : owner.projectileObjectCollision(moved, object, frame, slot);
        if (objectCollision == ProjectileObjectCollision.UNLOAD) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return false;
        }
        // $7DF3-$7E00 restores BOTH coordinates from the active pre-movement
        // position before the fireball handler can clear a burnable collision.
        positioned = objectCollision == ProjectileObjectCollision.BLOCKED ? entity : moved;
        if (owner.isMagicRodBurnableObject(object)) {
            runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .setCollision(slot, false);
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            runtime.events()
                    .magicRodObjectRequests
                    .add(new MagicRodObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
            runtime.slots()[index] = withVariant(positioned, variant);
            return false;
        }

        if (runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .collided(slot)
                || objectCollision == ProjectileObjectCollision.BLOCKED) {
            // ArrowRenderAndMove writes privateCountdown1 after the normal
            // fireball display has already been rendered. Keep the normal
            // definition for this frame; the next handler tick switches to
            // FireSpriteVariants and decrements the $30 countdown.
            runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .beginFireTransition(slot);
        }
        runtime.slots()[index] = withVariant(positioned, variant);
        return true;
    }
}
