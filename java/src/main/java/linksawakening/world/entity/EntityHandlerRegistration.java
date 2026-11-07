package linksawakening.world.entity;

import java.util.function.Predicate;
import linksawakening.world.EntityTickStage;
import linksawakening.world.EntityFrameExecution;

/** Registration surface for entity-family callbacks; hides registry storage. */
public interface EntityHandlerRegistration {
    void registerTick(int type, EntityTickStage stage, Predicate<EntityFrameExecution> callback);
}
