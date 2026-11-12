package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** Focused behavior using the state owned by EntityLiftThrowController. */
final class LiftableObjectBehavior {
    static void smashStatue(EntityLiftThrowController owner, RoomEntity entity) {
        var runtime = owner.runtime;
        // LiftableStatue_SpawnRubble ($19:4165-$41FE).
        int[] xs = {0, 8, 0, 8, 0, 8};
        int[] ys = {-8, -8, 0, 0, 8, 8};
        int[] vx = {0xFC, 5, 0xFA, 6, 0xFB, 4};
        int[] vy = {0xFC, 0xF8, 0xFE, 0xFF, 3, 2};
        int[] vz = {0x18, 0x14, 0x13, 0x16, 0x12, 0x14};
        if (runtime.spriteHandlers() != null) {
            var definition = runtime.spriteHandlers().decodeSingle(0x9D, 0x19, 0x4020, 1, 0);
            for (int piece = 0; piece < 6; piece++) {
                int slot = runtime.findFreeEntitySlot();
                if (slot < 0) break;
                runtime.slots()[slot] = new RoomEntity(
                        slot,
                        -1,
                        0x9D,
                        (entity.x() + xs[piece]) & 255,
                        (entity.y() + ys[piece]) & 255,
                        EntityStatus.ACTIVE,
                        definition,
                        0,
                        0,
                        0,
                        entity.z(),
                        -1,
                        false);
                runtime.initializeHealthGroup(slot);
                runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0xC1);
                runtime.combatTables().writeEnemyHitboxFlags(slot, 0);
                runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(slot, true);
                runtime.motions()
                        .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                        .startEnemyBomb(slot, vx[piece], vy[piece], vz[piece]);
            }
        }
        int visualY = (entity.y() - entity.z()) & 255;
        runtime.events()
                .transientVfxRequests
                .add(new linksawakening.world.entity.event.TransientVfxRequest(
                        linksawakening.vfx.TransientVfxType.POOF, entity.x(), visualY));
        runtime.events()
                .transientVfxRequests
                .add(new linksawakening.world.entity.event.TransientVfxRequest(
                        linksawakening.vfx.TransientVfxType.POOF, entity.x(), (visualY - 16) & 255));
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(entity.slot(), 0x9D, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x29));
    }

    static RoomEntity advancePlacedBombEntity(
            EntityLiftThrowController owner,
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            int frame) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (!runtime.bombInteractions().readPlacedBombMotionInitialized(slot)) {
            int direction = runtime.bombInteractions().readBombDirection(slot);
            if (direction < ThrownEntityMotion.ROM_DIRECTION_RIGHT
                    || direction > ThrownEntityMotion.ROM_DIRECTION_DOWN) {
                // A room-loaded type-$02 entity has no SpawnPlayerProjectile direction.
                // Preserve its static source state instead of inventing a table index.
                return entity;
            }
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .startPlacedBomb(slot, runtime.bombInteractions().readBombDirection(slot));
            runtime.bombInteractions().writePlacedBombMotionInitialized(slot, true);
        }
        return runtime.motions()
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(
                        entity,
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        runtime.physicsTables().readEntityGroundStatus(slot))
                .entity();
    }

    static void advanceLiftableRockSmash(
            EntityLiftThrowController owner, int index, RoomEntity entity, int frameCounter) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        int countdown = owner.readLiftableRockSmashCountdown(slot) & 0xFF;
        if (countdown <= 1) {
            // LiftableRockEntityHandler unloads on the countdown-$01 frame,
            // before the display-list helper gets a chance to render it.
            runtime.clearEntity(slot);
            return;
        }

        int sourceVariant = owner.readLiftableRockSmashSourceVariant(slot);
        boolean swampLeaves = !runtime.indoorRoom()
                && runtime.presentationState().readSpriteSelection() != null
                && runtime.presentationState().readSpriteSelection().roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && runtime.presentationState().readSpriteSelection().roomId() == 0x32;
        int variant = EntityMotionMath.liftableRockSmashVariant(sourceVariant, swampLeaves, countdown);
        if (sourceVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS && ((frameCounter ^ slot) & 0x01) != 0) {
            // The $FF tall-grass path in func_019_7C50 only emits OAM on
            // alternating frames. -1 is the same hidden-display sentinel
            // consumed by RenderActiveEntitySpritesRect.
            variant = -1;
        }
        runtime.slots()[index] = withVariant(entity, variant);
    }
}
