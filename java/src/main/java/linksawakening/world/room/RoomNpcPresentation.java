package linksawakening.world.room;

import java.util.HashMap;
import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntitySnapshot;

final class RoomNpcPresentation {
    private final Dependencies dependencies;

    RoomNpcPresentation(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityRuntimeState().runtime == null
                || !dependencies.entityRuntimeState().runtime.applyMarinWakeUpPresentation(x, y, spriteVariant)) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(
                        dependencies.entityRuntimeState().runtime.inspection().view());
        return true;
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        if (dependencies.location().activeRoom != null
                && dependencies.location().activeRoom.mapId() == 0x10
                && dependencies.location().activeRoom.roomId() == 0xA3
                && dependencies.followers().tarinFlag != 1
                && dependencies.followers().tarinFlag != 2
                && dependencies.entityRuntimeState().runtime != null
                && (frameCounter & 0x1F) == 0) {
            dependencies.entityRuntimeState().runtime.updateHouseTarinPresentation(linkX, linkY);
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityRuntimeState()
                            .runtime
                            .inspection()
                            .view());
        }
    }

    void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.entityView() == null
                || dependencies.location().activeRoom.entityView().spriteSelection() == null) {
            return;
        }
        Map<Integer, EntitySpriteDefinition> merged = new HashMap<>(dependencies
                .location()
                .activeRoom
                .entityView()
                .spriteSelection()
                .spriteOverrides());
        if (overrides != null) {
            merged.putAll(overrides);
        }
        RoomEntitySnapshot updated = dependencies
                .location()
                .activeRoom
                .entityView()
                .withSpriteSelection(dependencies
                        .location()
                        .activeRoom
                        .entityView()
                        .spriteSelection()
                        .withSpriteOverrides(merged));
        dependencies.location().activeRoom.replaceEntities(updated);
        if (dependencies.entityRuntimeState().runtime != null) {
            dependencies.entityRuntimeState().runtime.configuration().setSpriteSelection(updated.spriteSelection());
        }
    }

    record Dependencies(
            RoomEntityFrameState entityRuntimeState, RoomFollowerState followers, RoomLocationState location) {
        Dependencies {
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(location, "location");
        }
    }
}
