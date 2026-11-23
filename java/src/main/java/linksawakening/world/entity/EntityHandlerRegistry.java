package linksawakening.world.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** ROM type-byte dispatch; unregistered types remain explicitly unsupported. */
public final class EntityHandlerRegistry {
    private static final EntityHandler NO_INITIALIZER = (entity, random) -> {};
    // Retain one callable instance: its identity or allocation-free reuse is part of this retained path.
    @SuppressWarnings("UnnecessaryLambda")
    private static final BiFunction<RoomEntity, EntityFrameContext, RoomEntity> NO_ADVANCE = (entity, frame) -> entity;
    // Retain one callable instance: its identity or allocation-free reuse is part of this retained path.
    @SuppressWarnings("UnnecessaryLambda")
    private static final Predicate<EntityFrameExecution> USE_INITIALIZER = frame -> true;

    private static final class Binding implements EntityHandler {
        private EntityHandler initializer = NO_INITIALIZER;
        private boolean initializerRegistered;
        private BiFunction<RoomEntity, EntityFrameContext, RoomEntity> advance = NO_ADVANCE;
        private boolean advanceRegistered;
        private final EnumMap<EntityTickStage, List<Predicate<EntityFrameExecution>>> stages =
                new EnumMap<>(EntityTickStage.class);

        private Binding() {}

        private Binding(EntityHandler initializer) {
            this.initializer = Objects.requireNonNull(initializer, "initializer");
            initializerRegistered = true;
        }

        @Override
        public void initialize(RoomEntity entity, IntSupplier random) {
            initializer.initialize(entity, random);
        }

        @Override
        public RoomEntity advance(RoomEntity entity, EntityFrameContext context) {
            if (advanceRegistered) return advance.apply(entity, context);
            return initializer.advance(entity, context);
        }

        boolean tickVisit(EntityTickStage stage, int visit, EntityFrameExecution frame) {
            List<Predicate<EntityFrameExecution>> callbacks = stages.getOrDefault(stage, List.of());
            Predicate<EntityFrameExecution> handler = callbacks.isEmpty() ? USE_INITIALIZER : callbacks.get(visit);
            return handler == USE_INITIALIZER ? initializer.tick(stage, frame) : handler.test(frame);
        }
    }

    private final EntityMotionRegistry motions;

    EntityHandlerRegistry() {
        this(new EntityMotionRegistry());
    }

    EntityHandlerRegistry(EntityMotionRegistry motions) {
        this.motions = Objects.requireNonNull(motions, "motions");
    }

    public <T> T motion(Class<T> type, Supplier<? extends T> factory) {
        return motions.get(type, factory);
    }

    public void registerInitializer(int type, BiConsumer<RoomEntity, IntSupplier> initializer) {
        Objects.requireNonNull(initializer);
        Binding binding = binding(type);
        if (binding.initializerRegistered)
            throw new IllegalArgumentException("Duplicate initializer for entity type: " + type);
        binding.initializer = initializer::accept;
        binding.initializerRegistered = true;
    }

    // Unregistered type entries are absent; supports() distinguishes them from registered no-op hooks.
    private final Binding[] handlers = new Binding[256];
    private final boolean[] registered = new boolean[256];

    {
        Arrays.setAll(handlers, ignored -> new Binding());
    }

    void register(int type, EntityHandler handler) {
        Objects.checkIndex(type, handlers.length);
        if (registered[type]) throw new IllegalArgumentException("Duplicate entity type: " + type);
        handlers[type] = new Binding(Objects.requireNonNull(handler));
        registered[type] = true;
    }

    boolean supports(int type) {
        return type >= 0 && type < handlers.length && registered[type];
    }

    void initialize(RoomEntity entity, IntSupplier randomByteSupplier) {
        if (supports(entity.type())) handlers[entity.type()].initialize(entity, randomByteSupplier);
    }

    private Binding binding(int type) {
        Objects.checkIndex(type, handlers.length);
        registered[type] = true;
        return handlers[type];
    }

    public void registerAdvance(int type, BiFunction<RoomEntity, EntityFrameContext, RoomEntity> advance) {
        Objects.checkIndex(type, handlers.length);
        Objects.requireNonNull(advance);
        Binding binding = binding(type);
        if (binding.advanceRegistered) throw new IllegalArgumentException("Duplicate advancing entity type: " + type);
        binding.advance = advance;
        binding.advanceRegistered = true;
    }

    boolean hasAdvance(int type) {
        return supports(type) && handlers[type].advanceRegistered;
    }

    RoomEntity advance(RoomEntity entity, EntityFrameContext context) {
        return supports(entity.type()) ? handlers[entity.type()].advance(entity, context) : entity;
    }

    public void registerTick(int type, EntityTickStage stage, int visit, Predicate<EntityFrameExecution> tick) {
        Objects.checkIndex(type, handlers.length);
        Objects.requireNonNull(tick);
        Objects.requireNonNull(stage);
        Binding binding = binding(type);
        Objects.checkIndex(visit, stage.visits());
        List<Predicate<EntityFrameExecution>> callbacks = binding.stages.computeIfAbsent(
                stage, ignored -> new ArrayList<>(Collections.nCopies(stage.visits(), USE_INITIALIZER)));
        if (callbacks.get(visit) != USE_INITIALIZER) {
            throw new IllegalArgumentException("Duplicate tick stage for type: " + type);
        }
        callbacks.set(visit, tick);
    }

    boolean tick(EntityTickStage stage, EntityFrameExecution frame) {
        for (int visit = 0; visit < stage.visits(); visit++) {
            // Re-read the entity type after each visit, as the original phase calls did.
            int type = frame.slotFrame().entity().type();
            if (supports(type) && !handlers[type].tickVisit(stage, visit, frame)) return false;
        }
        return true;
    }

    public void registerTick(int type, EntityTickStage stage, Predicate<EntityFrameExecution> callback) {
        registerTick(type, stage, 0, callback);
    }
}
