package linksawakening.world.entity;

import java.util.function.Predicate;

/** Registration surface for entity-family callbacks; hides registry storage. */
public interface EntityHandlerRegistration {
    default void registerTick(int type, EntityTickStage stage, Predicate<EntityFrameExecution> callback) {
        registerTick(type, stage, 0, callback);
    }

    void registerTick(int type, EntityTickStage stage, int visit, Predicate<EntityFrameExecution> callback);
}
