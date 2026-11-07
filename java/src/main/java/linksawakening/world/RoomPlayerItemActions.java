package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** RoomPlayerItemActions operations for the live room session. */
final class RoomPlayerItemActions {
    private final RoomSession session;
    RoomPlayerItemActions(RoomSession session) { this.session = session; }

    boolean throwLiftedEntity(int linkDirection) {
        if (session.entityBridge.entityRuntime == null) {
            return false;
        }
        boolean thrown = session.entityBridge.entityRuntime.throwLiftedEntity(
            session.romDirectionForProjectileCollision(linkDirection));
        if (thrown) {
            session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        }
        return thrown;
    }

    boolean fireHookshot(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean linkAirborne, boolean linkPushing) {
        if (linkAirborne || linkPushing || session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.entityBridge.entityRuntime.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.entityBridge.entityRuntime.spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ,
            romDirection, pressedButtonsMask);
        if (slot < 0) {
            return false;
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.entityBridge.entityRuntime.spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return session.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.entityBridge.entityRuntime.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ,
            romDirection, pieceOfPower);
        if (slot < 0) {
            return false;
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.entityBridge.entityRuntime.spawnMagicPowderSprinkle(
            linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM shovel direction out of range: "
                + romDirection);
        }
        if (session.activeRoom == null || linkAirborne || session.shovelUseState != 0) {
            return new ShovelStartResult(false, true, -1);
        }
        ShovelProbe probe = session.probeShovel(linkEntityX, linkEntityY, romDirection);
        session.shovelUseState = session.SHOVEL_STATE_ACTIVE;
        return new ShovelStartResult(true, !probe.valid(), probe.location());
    }

    void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        if (session.activeRoom == null || session.shovelUseState == 0) {
            return;
        }
        int normalizedTimer = timer & 0xFF;
        if (normalizedTimer == session.SHOVEL_DIG_TIMER) {
            ShovelProbe probe = session.probeShovel(linkEntityX, linkEntityY, romDirection);
            if (!probe.valid()) {
                return;
            }
            session.shovelUseState = session.SHOVEL_STATE_DUG;
            session.writeShovelHole(probe);
            session.pendingShovelDrop = new PendingShovelDrop(
                probe.objectLeft(), probe.objectTop(), linkEntityX, linkEntityY,
                session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                    || session.activeRoom.roomId() != 0x0E);
        } else if (normalizedTimer == session.SHOVEL_FINISH_TIMER) {
            if (session.shovelUseState == session.SHOVEL_STATE_DUG && session.followingNpcState.marinFollowing()) {
                session.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                    session.SHOVEL_DIALOG_TABLE, session.SHOVEL_DIALOG_ID));
            }
            session.shovelUseState = 0;
        }
    }

    int shovelAnimationState(int javaDirection, int timer) {
        return session.romTables.shovelAnimationState(timer,
            session.romDirectionForProjectileCollision(javaDirection));
    }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) {
        if (session.activeRoom == null || session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
            return ShovelProbe.invalid();
        }

        // func_002_4D20 subtracts Link's sprite-center/bottom origin before
        // masking to the room-object grid. These are hLinkPositionX/Y, not
        // Java's top-left pixel coordinates.
        int objectLeft = (linkEntityX + session.SHOVEL_TARGET_X[romDirection] - 0x08) & 0xF0;
        int objectTop = (linkEntityY + session.SHOVEL_TARGET_Y[romDirection] - 0x10) & 0xF0;
        int cellX = objectLeft >>> 4;
        int cellY = objectTop >>> 4;
        if (cellX < 0 || cellX >= RoomConstants.OBJECTS_PER_ROW
            || cellY < 0 || cellY >= RoomConstants.OBJECTS_PER_COLUMN) {
            return ShovelProbe.invalid();
        }

        int location = objectTop | cellX;
        int objectId = session.objectAtRoomLocation(location);
        int physicsTable = session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? RomTables.PHYSICS_TABLE_OVERWORLD
            : session.activeRoom.mapId() == 0xFF
                ? RomTables.PHYSICS_TABLE_INDOORS2
                : RomTables.PHYSICS_TABLE_INDOORS1;
        boolean valid = session.romTables.objectPhysicsFlag(physicsTable, objectId) == 0;
        if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            valid &= objectId != session.OBJECT_SHOVEL_BLOCKER_A
                && objectId != session.OBJECT_SHOVEL_BLOCKER_B
                && objectId != session.OBJECT_SHOVEL_BLOCKER_C;
        } else {
            valid &= objectId == session.OBJECT_SHOVEL_INDOOR_DIGGABLE;
        }
        return new ShovelProbe(valid, location, objectLeft, objectTop);
    }

    void writeShovelHole(ShovelProbe probe) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + probe.location();
        int[] objects = session.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = session.OBJECT_SHOVEL_HOLE;
        if (session.activeRoom.renderValues() != null && areaIndex < session.activeRoom.renderValues().length) {
            session.activeRoom.renderValues()[areaIndex] = session.OBJECT_SHOVEL_HOLE;
        }
        if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            // On GBC the mutable WRAM2 overlay is the render source for both
            // tile ids and attributes; rebuilding from the immutable ROM
            // overlay here would immediately undo the hole.
            session.overworldBushInteraction.refreshRoomObjectCell(
                session.activeRoom.roomId(), probe.location(), objects, session.activeRoom.renderValues(),
                session.activeRoom.gbcOverlay(), session.activeRoom.tileIds(), session.activeRoom.tileAttrs());
            session.refreshOverworldCollisionAfterObjectMutation();
        } else {
            session.refreshActiveRoomTilemap();
            session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        }
    }

    boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.entityBridge.entityRuntime.spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean lastBombPlacementPlayedBump() {
        return session.entityBridge.entityRuntime != null && session.entityBridge.entityRuntime.lastBombPlacementPlayedBump();
    }

    boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return session.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false)
            .spawned();
    }

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return session.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return new ArrowShotResult(false, false);
        }
        int slot = session.entityBridge.entityRuntime.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection,
            pieceOfPower);
        if (slot < 0) {
            return new ArrowShotResult(false, false);
        }
        boolean playWhoosh = session.entityBridge.entityRuntime.lastArrowShotPlayedWhoosh();
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return new ArrowShotResult(true, playWhoosh);
    }

    int activeProjectileCount() {
        return session.entityBridge.entityRuntime == null ? 0 : session.entityBridge.entityRuntime.activeProjectileCount();
    }

    boolean bombActive() {
        return session.entityBridge.entityRuntime != null && session.entityBridge.entityRuntime.bombActive();
    }

    boolean hookshotActive() {
        return session.entityBridge.entityRuntime != null && session.entityBridge.entityRuntime.hookshotActive();
    }

    boolean boomerangActive() {
        return session.entityBridge.entityRuntime != null && session.entityBridge.entityRuntime.boomerangActive();
    }
}
