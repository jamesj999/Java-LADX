package linksawakening.world.room;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntitySnapshot;

final class RoomNpcPresentation {
    private final Dependencies dependencies;

    RoomNpcPresentation(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    boolean applyMarinWakeUpPresentation(RoomRuntime loaded, int x, int y, int spriteVariant) {
        if (!loaded.entities.commands().applyMarinWakeUpPresentation(x, y, spriteVariant)) {
            return false;
        }
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        return true;
    }

    void updateHouseTarinPresentation(RoomRuntime loaded, int frameCounter, int linkX, int linkY) {
        if (loaded.room.mapId() == 0x10
                && loaded.room.roomId() == 0xA3
                && dependencies.followers().tarinFlag != 1
                && dependencies.followers().tarinFlag != 2
                && (frameCounter & 0x1F) == 0) {
            loaded.entities.commands().updateHouseTarinPresentation(linkX, linkY);
            loaded.room.replaceEntities(loaded.entities.inspection().view());
        }
    }

    void setFollowerSpriteOverrides(RoomRuntime loaded, Map<Integer, EntitySpriteDefinition> overrides) {
        if (loaded.room.entityView().spriteSelection() == null) {
            return;
        }
        Map<Integer, EntitySpriteDefinition> merged =
                new HashMap<>(loaded.room.entityView().spriteSelection().spriteOverrides());
        if (overrides != null) {
            merged.putAll(overrides);
        }
        RoomEntitySnapshot updated = loaded.room
                .entityView()
                .withSpriteSelection(loaded.room.entityView().spriteSelection().withSpriteOverrides(merged));
        loaded.room.replaceEntities(updated);

        loaded.entities.configuration().setSpriteSelection(updated.spriteSelection());
    }

    record Dependencies(RoomFollowerState followers) {
        Dependencies {
            Objects.requireNonNull(followers, "followers");
        }
    }
}
