package linksawakening.world.room;

import linksawakening.world.transition.Warp;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.rom.RomTables;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

/** RoomPlayerItemActions operations for the live room session. */
final class RoomPlayerItemActions {
    private final RoomSession session;
    RoomPlayerItemActions(RoomSession session) { this.session = session; }

    boolean throwLiftedEntity(int linkDirection) {
        if (session.context.entityBridge.entityRuntime == null) {
            return false;
        }
        boolean thrown = session.context.entityBridge.entityRuntime.throwLiftedEntity(
            session.romDirectionForProjectileCollision(linkDirection));
        if (thrown) {
            session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        }
        return thrown;
    }

    boolean fireHookshot(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean linkAirborne, boolean linkPushing) {
        if (linkAirborne || linkPushing || session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.context.entityBridge.entityRuntime.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.context.entityBridge.entityRuntime.spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ,
            romDirection, pressedButtonsMask);
        if (slot < 0) {
            return false;
        }
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.context.entityBridge.entityRuntime.spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return session.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.context.entityBridge.entityRuntime.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ,
            romDirection, pieceOfPower);
        if (slot < 0) {
            return false;
        }
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.context.entityBridge.entityRuntime.spawnMagicPowderSprinkle(
            linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        return true;
    }

    ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM shovel direction out of range: "
                + romDirection);
        }
        if (session.context.activeRoom == null || linkAirborne || session.context.shovelUseState != 0) {
            return new ShovelStartResult(false, true, -1);
        }
        ShovelProbe probe = session.probeShovel(linkEntityX, linkEntityY, romDirection);
        session.context.shovelUseState = session.SHOVEL_STATE_ACTIVE;
        return new ShovelStartResult(true, !probe.valid(), probe.location());
    }

    void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        if (session.context.activeRoom == null || session.context.shovelUseState == 0) {
            return;
        }
        int normalizedTimer = timer & 0xFF;
        if (normalizedTimer == session.SHOVEL_DIG_TIMER) {
            ShovelProbe probe = session.probeShovel(linkEntityX, linkEntityY, romDirection);
            if (!probe.valid()) {
                return;
            }
            session.context.shovelUseState = session.SHOVEL_STATE_DUG;
            session.writeShovelHole(probe);
            session.context.pendingShovelDrop = new PendingShovelDrop(
                probe.objectLeft(), probe.objectTop(), linkEntityX, linkEntityY,
                session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                    || session.context.activeRoom.roomId() != 0x0E);
        } else if (normalizedTimer == session.SHOVEL_FINISH_TIMER) {
            if (session.context.shovelUseState == session.SHOVEL_STATE_DUG && session.context.followingNpcState.marinFollowing()) {
                session.context.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                    session.SHOVEL_DIALOG_TABLE, session.SHOVEL_DIALOG_ID));
            }
            session.context.shovelUseState = 0;
        }
    }

    int shovelAnimationState(int javaDirection, int timer) {
        return session.context.romTables.shovelAnimationState(timer,
            session.romDirectionForProjectileCollision(javaDirection));
    }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) {
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
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
        int physicsTable = session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? RomTables.PHYSICS_TABLE_OVERWORLD
            : session.context.activeRoom.mapId() == 0xFF
                ? RomTables.PHYSICS_TABLE_INDOORS2
                : RomTables.PHYSICS_TABLE_INDOORS1;
        boolean valid = session.context.romTables.objectPhysicsFlag(physicsTable, objectId) == 0;
        if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
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
        int[] objects = session.context.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = session.OBJECT_SHOVEL_HOLE;
        if (session.context.activeRoom.renderValues() != null && areaIndex < session.context.activeRoom.renderValues().length) {
            session.context.activeRoom.renderValues()[areaIndex] = session.OBJECT_SHOVEL_HOLE;
        }
        if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            // On GBC the mutable WRAM2 overlay is the render source for both
            // tile ids and attributes; rebuilding from the immutable ROM
            // overlay here would immediately undo the hole.
            session.context.overworldBushInteraction.refreshRoomObjectCell(
                session.context.activeRoom.roomId(), probe.location(), objects, session.context.activeRoom.renderValues(),
                session.context.activeRoom.gbcOverlay(), session.context.activeRoom.tileIds(), session.context.activeRoom.tileAttrs());
            session.refreshOverworldCollisionAfterObjectMutation();
        } else {
            session.refreshActiveRoomTilemap();
            session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        }
    }

    boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = session.context.entityBridge.entityRuntime.spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean lastBombPlacementPlayedBump() {
        return session.context.entityBridge.entityRuntime != null && session.context.entityBridge.entityRuntime.lastBombPlacementPlayedBump();
    }

    boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return session.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false)
            .spawned();
    }

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return session.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return new ArrowShotResult(false, false);
        }
        int slot = session.context.entityBridge.entityRuntime.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection,
            pieceOfPower);
        if (slot < 0) {
            return new ArrowShotResult(false, false);
        }
        boolean playWhoosh = session.context.entityBridge.entityRuntime.lastArrowShotPlayedWhoosh();
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        return new ArrowShotResult(true, playWhoosh);
    }

    int activeProjectileCount() {
        return session.context.entityBridge.entityRuntime == null ? 0 : session.context.entityBridge.entityRuntime.activeProjectileCount();
    }

    boolean bombActive() {
        return session.context.entityBridge.entityRuntime != null && session.context.entityBridge.entityRuntime.bombActive();
    }

    boolean hookshotActive() {
        return session.context.entityBridge.entityRuntime != null && session.context.entityBridge.entityRuntime.hookshotActive();
    }

    boolean boomerangActive() {
        return session.context.entityBridge.entityRuntime != null && session.context.entityBridge.entityRuntime.boomerangActive();
    }
}
