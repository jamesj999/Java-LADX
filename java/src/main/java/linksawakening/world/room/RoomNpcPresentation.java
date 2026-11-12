package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

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
                || dependencies.entityBridge().get().entityRuntime == null
                || !dependencies.entityBridge().get().entityRuntime.applyMarinWakeUpPresentation(x, y, spriteVariant)) {
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
                        .view());
        return true;
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        if (dependencies.location().activeRoom != null
                && dependencies.location().activeRoom.mapId() == 0x10
                && dependencies.location().activeRoom.roomId() == 0xA3
                && dependencies.followers().tarinFlag != 1
                && dependencies.followers().tarinFlag != 2
                && dependencies.entityBridge().get().entityRuntime != null
                && (frameCounter & 0x1F) == 0) {
            dependencies.entityBridge().get().entityRuntime.updateHouseTarinPresentation(linkX, linkY);
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
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
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setSpriteSelection(updated.spriteSelection());
        }
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomFollowerState followers,
            RoomLocationState location) {}
}
