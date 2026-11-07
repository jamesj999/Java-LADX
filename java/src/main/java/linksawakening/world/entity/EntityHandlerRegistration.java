package linksawakening.world.entity;

import java.util.function.Predicate;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityFrameExecution;

/** Registration surface for entity-family callbacks; hides registry storage. */
public interface EntityHandlerRegistration {
    void registerTick(int type, EntityTickStage stage, Predicate<EntityFrameExecution> callback);
}
