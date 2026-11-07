package linksawakening.world;

import java.util.EnumMap;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/** ROM type-byte dispatch; unregistered types remain explicitly unsupported. */
final class EntityHandlerRegistry implements linksawakening.world.entity.EntityHandlerRegistration {
  private static final class Binding implements EntityHandler {
    private final EntityHandler initializer;
    private BiFunction<RoomEntity, EntityFrameContext, RoomEntity> advance;
    private final EnumMap<EntityTickStage, Predicate<EntityFrameExecution>> stages =
        new EnumMap<>(EntityTickStage.class);

    private Binding(EntityHandler initializer) {
      this.initializer = initializer;
    }

    @Override
    public void initialize(RoomEntity entity, IntSupplier random) {
      if (initializer != null) initializer.initialize(entity, random);
    }

    @Override
    public RoomEntity advance(RoomEntity entity, EntityFrameContext context) {
      if (advance != null) return advance.apply(entity, context);
      return initializer == null ? entity : initializer.advance(entity, context);
    }

    @Override
    public boolean tick(EntityTickStage stage, EntityFrameExecution frame) {
      Predicate<EntityFrameExecution> handler = stages.get(stage);
      if (handler != null) return handler.test(frame);
      return initializer == null || initializer.tick(stage, frame);
    }
  }

  private final Binding[] handlers = new Binding[256];

  void register(int type, EntityHandler handler) {
    Objects.checkIndex(type, handlers.length);
    if (handlers[type] != null)
      throw new IllegalArgumentException("Duplicate entity type: " + type);
    handlers[type] = new Binding(Objects.requireNonNull(handler));
  }

  boolean supports(int type) {
    return type >= 0 && type < handlers.length && handlers[type] != null;
  }

  void initialize(RoomEntity entity, IntSupplier randomByteSupplier) {
    if (supports(entity.type())) handlers[entity.type()].initialize(entity, randomByteSupplier);
  }

  private Binding binding(int type) {
    Objects.checkIndex(type, handlers.length);
    if (handlers[type] == null) handlers[type] = new Binding(null);
    return handlers[type];
  }

  void registerAdvance(int type, BiFunction<RoomEntity, EntityFrameContext, RoomEntity> advance) {
    Objects.checkIndex(type, handlers.length);
    Objects.requireNonNull(advance);
    Binding binding = binding(type);
    if (binding.advance != null)
      throw new IllegalArgumentException("Duplicate advancing entity type: " + type);
    binding.advance = advance;
  }

  boolean hasAdvance(int type) {
    return supports(type) && handlers[type].advance != null;
  }

  RoomEntity advance(RoomEntity entity, EntityFrameContext context) {
    return supports(entity.type()) ? handlers[entity.type()].advance(entity, context) : entity;
  }

  public void registerTick(int type, EntityTickStage stage, Predicate<EntityFrameExecution> tick) {
    Objects.checkIndex(type, handlers.length);
    Objects.requireNonNull(tick);
    Objects.requireNonNull(stage);
    Binding binding = binding(type);
    if (binding.stages.putIfAbsent(stage, tick) != null) {
      throw new IllegalArgumentException("Duplicate tick stage for type: " + type);
    }
  }

  boolean tick(EntityTickStage stage, EntityFrameExecution frame) {
    int type = frame.entity.type();
    return !supports(type) || handlers[type].tick(stage, frame);
  }
}
