package linksawakening.world.entity;

import java.util.function.Predicate;

/** Registration surface for entity-family callbacks; hides registry storage. */
public interface EntityHandlerRegistration {
    void registerTick(int type, EntityTickStage stage, Predicate<EntityFrameExecution> callback);
}
