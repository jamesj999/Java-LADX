package linksawakening.world.room;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.world.object.OverworldBushInteraction;

/** ApplyLinkMotionState player-item dispatch; lift, chest and shovel routines retain their own action state. */
final class RoomPlayerItemActions {

    private final RoomShovelActions roomShovelActions;

    RoomPlayerItemActions(Dependencies dependencies) {

        roomShovelActions = new RoomShovelActions(new RoomShovelActions.Dependencies(
                dependencies.entityRuntimeState(),
                dependencies.followers(),
                dependencies.items(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                dependencies.romTables(),
                dependencies.roomObjectMutations(),
                this,
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw()));
    }

    boolean throwLiftedEntity(RoomRuntime loaded, linksawakening.state.Direction linkDirection) {

        boolean thrown = loaded.entities.throwLiftedEntity(
                RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection));
        if (thrown) {
            publishEntityView(loaded);
        }
        return thrown;
    }

    boolean fireHookshot(
            RoomRuntime loaded,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean linkAirborne,
            boolean linkPushing) {
        if (linkAirborne || linkPushing) {
            return false;
        }
        int slot = loaded.entities.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView(loaded);
        return true;
    }

    boolean fireBoomerang(
            RoomRuntime loaded,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            int pressedButtonsMask) {

        int slot =
                loaded.entities.spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
        if (slot < 0) {
            return false;
        }
        publishEntityView(loaded);
        return true;
    }

    boolean fireSwordBeam(RoomRuntime loaded, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {

        int slot = loaded.entities.spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView(loaded);
        return true;
    }

    boolean fireMagicRodFireball(
            RoomRuntime loaded, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return this.fireMagicRodFireball(loaded, linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    boolean fireMagicRodFireball(
            RoomRuntime loaded,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean pieceOfPower) {

        int slot = loaded.entities.spawnMagicRodFireball(
                linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
        if (slot < 0) {
            return false;
        }
        publishEntityView(loaded);
        return true;
    }

    boolean sprinkleMagicPowder(
            RoomRuntime loaded, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {

        int slot = loaded.entities.spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView(loaded);
        return true;
    }

    ShovelStartResult startShovel(
            RoomRuntime loaded, int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        return roomShovelActions.startShovel(loaded, linkEntityX, linkEntityY, romDirection, linkAirborne);
    }

    void advanceShovel(RoomRuntime loaded, int linkEntityX, int linkEntityY, int romDirection, int timer) {
        roomShovelActions.advanceShovel(loaded, linkEntityX, linkEntityY, romDirection, timer);
    }

    int shovelAnimationState(linksawakening.state.Direction javaDirection, int timer) {
        return roomShovelActions.shovelAnimationState(javaDirection, timer);
    }

    ShovelProbe probeShovel(RoomRuntime loaded, int linkEntityX, int linkEntityY, int romDirection) {
        return roomShovelActions.probeShovel(loaded, linkEntityX, linkEntityY, romDirection);
    }

    void writeShovelHole(RoomRuntime loaded, ShovelProbe probe) {
        roomShovelActions.writeShovelHole(loaded, probe);
    }

    boolean placeBomb(RoomRuntime loaded, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {

        int slot = loaded.entities.spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        publishEntityView(loaded);
        return true;
    }

    boolean lastBombPlacementPlayedBump(RoomRuntime loaded) {
        return loaded.entities.inspection().lastBombPlacementPlayedBump();
    }

    boolean shootArrow(RoomRuntime loaded, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return this.shootArrowResult(loaded, linkEntityX, linkEntityY, linkEntityZ, romDirection, false)
                .spawned();
    }

    ArrowShotResult shootArrowResult(
            RoomRuntime loaded, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return this.shootArrowResult(loaded, linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    ArrowShotResult shootArrowResult(
            RoomRuntime loaded,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean pieceOfPower) {

        int slot = loaded.entities.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
        if (slot < 0) {
            return new ArrowShotResult(false, false);
        }
        boolean playWhoosh = loaded.entities.inspection().lastArrowShotPlayedWhoosh();
        publishEntityView(loaded);
        return new ArrowShotResult(true, playWhoosh);
    }

    int activeProjectileCount(RoomRuntime loaded) {
        return loaded.entities.inspection().activeProjectileCount();
    }

    boolean bombActive(RoomRuntime loaded) {
        return loaded.entities.inspection().bombActive();
    }

    boolean hookshotActive(RoomRuntime loaded) {
        return loaded.entities.inspection().hookshotActive();
    }

    boolean boomerangActive(RoomRuntime loaded) {
        return loaded.entities.inspection().boomerangActive();
    }

    private void publishEntityView(RoomRuntime loaded) {
        loaded.room.replaceEntities(loaded.entities.inspection().view());
    }

    record Dependencies(
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomEntitySessionState entityRuntimeState,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RomTables romTables,
            RoomObjectMutations roomObjectMutations,
            RoomSessionViews roomSessionViews,
            RoomObjectMutations roomTileRedraw) {
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
