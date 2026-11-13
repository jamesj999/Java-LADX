package linksawakening.world.room;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.world.object.OverworldBushInteraction;

/** ApplyLinkMotionState player-item dispatch; lift, chest and shovel routines retain their own action state. */
final class RoomPlayerItemActions {
    private final Dependencies dependencies;
    private final RoomShovelActions roomShovelActions;

    RoomPlayerItemActions(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomShovelActions = new RoomShovelActions(new RoomShovelActions.Dependencies(
                dependencies.entityRuntimeState(),
                dependencies.followers(),
                dependencies.items(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                dependencies.romTables(),
                dependencies.roomObjectMutations(),
                () -> this,
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw()));
    }

    boolean throwLiftedEntity(linksawakening.state.Direction linkDirection) {
        if (dependencies.entityRuntimeState().runtime == null) {
            return false;
        }
        boolean thrown = dependencies
                .entityRuntimeState()
                .runtime
                .throwLiftedEntity(RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection));
        if (thrown) {
            publishEntityView();
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
                || dependencies.entityRuntimeState().runtime == null) {
            return false;
        }
        int slot = dependencies
                .entityRuntimeState()
                .runtime
                .spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView();
        return true;
    }

    boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        if (dependencies.location().activeRoom == null || dependencies.entityRuntimeState().runtime == null) {
            return false;
        }
        int slot = dependencies
                .entityRuntimeState()
                .runtime
                .spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
        if (slot < 0) {
            return false;
        }
        publishEntityView();
        return true;
    }

    boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (dependencies.location().activeRoom == null || dependencies.entityRuntimeState().runtime == null) {
            return false;
        }
        int slot = dependencies
                .entityRuntimeState()
                .runtime
                .spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView();
        return true;
    }

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return this.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    boolean fireMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (dependencies.location().activeRoom == null || dependencies.entityRuntimeState().runtime == null) {
            return false;
        }
        int slot = dependencies
                .entityRuntimeState()
                .runtime
                .spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
        if (slot < 0) {
            return false;
        }
        publishEntityView();
        return true;
    }

    boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (dependencies.location().activeRoom == null || dependencies.entityRuntimeState().runtime == null) {
            return false;
        }
        int slot = dependencies
                .entityRuntimeState()
                .runtime
                .spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView();
        return true;
    }

    ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        return roomShovelActions.startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne);
    }

    void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        roomShovelActions.advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
    }

    int shovelAnimationState(linksawakening.state.Direction javaDirection, int timer) {
        return roomShovelActions.shovelAnimationState(javaDirection, timer);
    }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) {
        return roomShovelActions.probeShovel(linkEntityX, linkEntityY, romDirection);
    }

    void writeShovelHole(ShovelProbe probe) {
        roomShovelActions.writeShovelHole(probe);
    }

    boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (dependencies.location().activeRoom == null || dependencies.entityRuntimeState().runtime == null) {
            return false;
        }
        int slot = dependencies
                .entityRuntimeState()
                .runtime
                .spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView();
        return true;
    }

    boolean lastBombPlacementPlayedBump() {
        return dependencies.entityRuntimeState().runtime != null
                && dependencies.entityRuntimeState().runtime.inspection().lastBombPlacementPlayedBump();
    }

    boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return this.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false)
                .spawned();
    }

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return this.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    ArrowShotResult shootArrowResult(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        if (dependencies.location().activeRoom == null || dependencies.entityRuntimeState().runtime == null) {
            return new ArrowShotResult(false, false);
        }
        int slot = dependencies
                .entityRuntimeState()
                .runtime
                .spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
        if (slot < 0) {
            return new ArrowShotResult(false, false);
        }
        boolean playWhoosh =
                dependencies.entityRuntimeState().runtime.inspection().lastArrowShotPlayedWhoosh();
        publishEntityView();
        return new ArrowShotResult(true, playWhoosh);
    }

    int activeProjectileCount() {
        return dependencies.entityRuntimeState().runtime == null
                ? 0
                : dependencies.entityRuntimeState().runtime.inspection().activeProjectileCount();
    }

    boolean bombActive() {
        return dependencies.entityRuntimeState().runtime != null
                && dependencies.entityRuntimeState().runtime.inspection().bombActive();
    }

    boolean hookshotActive() {
        return dependencies.entityRuntimeState().runtime != null
                && dependencies.entityRuntimeState().runtime.inspection().hookshotActive();
    }

    boolean boomerangActive() {
        return dependencies.entityRuntimeState().runtime != null
                && dependencies.entityRuntimeState().runtime.inspection().boomerangActive();
    }

    private void publishEntityView() {
        dependencies
                .location()
                .activeRoom
                .replaceEntities(
                        dependencies.entityRuntimeState().runtime.inspection().view());
    }

    record Dependencies(
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomEntityFrameState entityRuntimeState,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RomTables romTables,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw) {
        Dependencies {
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(overworldBushInteraction, "overworldBushInteraction");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
        }
    }
}
