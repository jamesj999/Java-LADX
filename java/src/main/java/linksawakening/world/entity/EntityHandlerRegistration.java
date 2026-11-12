package linksawakening.world.entity;

import java.util.function.Predicate;

/** Registration surface for entity-family callbacks; hides registry storage. */
public interface EntityHandlerRegistration {
    default void registerTick(int type, EntityTickStage stage, Predicate<EntityFrameExecution> callback) {
        registerTick(type, stage, 0, callback);
    }

    void registerTick(int type, EntityTickStage stage, int visit, Predicate<EntityFrameExecution> callback);

    <T> T motion(Class<T> type, java.util.function.Supplier<? extends T> factory);

    void registerInitializer(
            int type, java.util.function.BiConsumer<RoomEntity, java.util.function.IntSupplier> initializer);

    void registerAdvance(int type, java.util.function.BiFunction<RoomEntity, EntityFrameContext, RoomEntity> advance);
}
