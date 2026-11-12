package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.RoomSession.ArrowShotResult;
import linksawakening.world.room.RoomSession.ShovelStartResult;

/** RoomPlayerItemActions operations for the live room session. */
final class RoomPlayerItemActions {
    private final Dependencies dependencies;
    private final RoomShovelActions roomShovelActions;

    RoomPlayerItemActions(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomShovelActions = new RoomShovelActions(new RoomShovelActions.Dependencies(
                dependencies.entityBridge(),
                dependencies.followers(),
                dependencies.items(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                dependencies.romTables(),
                dependencies.roomObjectMutations(),
                dependencies.roomPlayerItemActions(),
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw()));
    }

    boolean throwLiftedEntity(int linkDirection) {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return false;
        }
        boolean thrown = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .throwLiftedEntity(RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection));
        if (thrown) {
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .snapshot());
        }
        return thrown;
    }

    boolean fireHookshot(
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean linkAirborne,
            boolean linkPushing) {
        if (linkAirborne
                || linkPushing
                || dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return false;
        }
        int slot = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return true;
    }

    boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return false;
        }
        int slot = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
        if (slot < 0) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return true;
    }

    boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return false;
        }
        int slot = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return true;
    }

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return dependencies
                .roomPlayerItemActions()
                .get()
                .fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    boolean fireMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return false;
        }
        int slot = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
        if (slot < 0) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return true;
    }

    boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return false;
        }
        int slot = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return true;
    }

    ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        return roomShovelActions.startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne);
    }

    void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        roomShovelActions.advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
    }

    int shovelAnimationState(int javaDirection, int timer) {
        return roomShovelActions.shovelAnimationState(javaDirection, timer);
    }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) {
        return roomShovelActions.probeShovel(linkEntityX, linkEntityY, romDirection);
    }

    void writeShovelHole(ShovelProbe probe) {
        roomShovelActions.writeShovelHole(probe);
    }

    boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return false;
        }
        int slot = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return true;
    }

    boolean lastBombPlacementPlayedBump() {
        return dependencies.entityBridge().get().entityRuntime != null
                && dependencies.entityBridge().get().entityRuntime.inspection().lastBombPlacementPlayedBump();
    }

    boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return dependencies
                .roomPlayerItemActions()
                .get()
                .shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false)
                .spawned();
    }

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return dependencies
                .roomPlayerItemActions()
                .get()
                .shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    ArrowShotResult shootArrowResult(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return new ArrowShotResult(false, false);
        }
        int slot = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
        if (slot < 0) {
            return new ArrowShotResult(false, false);
        }
        boolean playWhoosh =
                dependencies.entityBridge().get().entityRuntime.inspection().lastArrowShotPlayedWhoosh();
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return new ArrowShotResult(true, playWhoosh);
    }

    int activeProjectileCount() {
        return dependencies.entityBridge().get().entityRuntime == null
                ? 0
                : dependencies.entityBridge().get().entityRuntime.inspection().activeProjectileCount();
    }

    boolean bombActive() {
        return dependencies.entityBridge().get().entityRuntime != null
                && dependencies.entityBridge().get().entityRuntime.inspection().bombActive();
    }

    boolean hookshotActive() {
        return dependencies.entityBridge().get().entityRuntime != null
                && dependencies.entityBridge().get().entityRuntime.inspection().hookshotActive();
    }

    boolean boomerangActive() {
        return dependencies.entityBridge().get().entityRuntime != null
                && dependencies.entityBridge().get().entityRuntime.inspection().boomerangActive();
    }

    record Dependencies(
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RomTables romTables,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomPlayerItemActions> roomPlayerItemActions,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw) {}
}
