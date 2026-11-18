package linksawakening.world.room;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Session-owned lifecycle cell; operations borrow a generation once and preserve their no-room policy. */
final class LoadedRoomHandle {
    // No generation exists before the first successful load.
    private RoomRuntime current;

    RoomRuntime currentOrNull() {
        return current;
    }

    void install(RoomRuntime room) {
        current = Objects.requireNonNull(room, "room");
    }

    void clear() {
        current = null;
    }

    boolean hasRoom() {
        return current != null;
    }

    /** Run synchronously against one generation; empty drains never allocate. */
    <T> List<T> drain(Function<RoomRuntime, List<T>> operation) {
        RoomRuntime room = current;
        return room == null ? List.of() : operation.apply(room);
    }

    boolean test(Predicate<RoomRuntime> operation) {
        RoomRuntime room = current;
        return room != null && operation.test(room);
    }

    <C> boolean test(C context, BiPredicate<RoomRuntime, C> operation) {
        RoomRuntime room = current;
        return room != null && operation.test(room, context);
    }

    <T> T readOr(T absent, Function<RoomRuntime, T> operation) {
        RoomRuntime room = current;
        return room == null ? absent : operation.apply(room);
    }

    <C, T> T readOr(C context, T absent, BiFunction<RoomRuntime, C, T> operation) {
        RoomRuntime room = current;
        return room == null ? absent : operation.apply(room, context);
    }

    int intOr(int absent, ToIntFunction<RoomRuntime> operation) {
        RoomRuntime room = current;
        return room == null ? absent : operation.applyAsInt(room);
    }

    <C> void update(C context, BiConsumer<RoomRuntime, C> operation) {
        RoomRuntime room = current;
        if (room != null) operation.accept(room, context);
    }

    void updateInt(int value, ObjIntConsumer<RoomRuntime> operation) {
        RoomRuntime room = current;
        if (room != null) operation.accept(room, value);
    }

    @FunctionalInterface
    interface IntPairUpdate {
        void accept(RoomRuntime room, int first, int second);
    }

    void updateInts(int first, int second, IntPairUpdate operation) {
        RoomRuntime room = current;
        if (room != null) operation.accept(room, first, second);
    }

    @FunctionalInterface
    interface FlagIntUpdate {
        void accept(RoomRuntime room, boolean flag, int value);
    }

    void updateFlagInt(boolean flag, int value, FlagIntUpdate operation) {
        RoomRuntime room = current;
        if (room != null) operation.accept(room, flag, value);
    }

    @FunctionalInterface
    interface FlagPairUpdate {
        void accept(RoomRuntime room, boolean first, boolean second);
    }

    void updateFlags(boolean first, boolean second, FlagPairUpdate operation) {
        RoomRuntime room = current;
        if (room != null) operation.accept(room, first, second);
    }

    <T> T readOrGet(Supplier<T> absent, Function<RoomRuntime, T> operation) {
        RoomRuntime room = current;
        return room == null ? absent.get() : operation.apply(room);
    }

    @FunctionalInterface
    interface LoadedQuery<C, I, R> {
        R apply(RoomRuntime room, C context, I input);
    }

    <C, I, R> R readOr(C context, I input, R absent, LoadedQuery<C, I, R> operation) {
        RoomRuntime room = current;
        return room == null ? absent : operation.apply(room, context, input);
    }

    @FunctionalInterface
    interface SlotIntQuery {
        int apply(RoomRuntime room, int slot);
    }

    int intOr(int slot, int absent, SlotIntQuery operation) {
        RoomRuntime room = current;
        return room == null ? absent : operation.apply(room, slot);
    }

    @FunctionalInterface
    interface SlotPositionPredicate {
        boolean test(RoomRuntime room, int slot, int x, int y);
    }

    boolean testPosition(int slot, int x, int y, SlotPositionPredicate operation) {
        RoomRuntime room = current;
        return room != null && operation.test(room, slot, x, y);
    }

    @FunctionalInterface
    interface ContextIntQuery<C> {
        int apply(RoomRuntime room, C context);
    }

    <C> int intOr(C context, int absent, ContextIntQuery<C> operation) {
        RoomRuntime room = current;
        return room == null ? absent : operation.apply(room, context);
    }

    @FunctionalInterface
    interface FrameFlagPredicate<C, I> {
        boolean test(RoomRuntime room, C context, I input, boolean flag);
    }

    <C, I> boolean testFrame(C context, I input, boolean flag, FrameFlagPredicate<C, I> operation) {
        RoomRuntime room = current;
        return room != null && operation.test(room, context, input, flag);
    }

    @FunctionalInterface
    interface ContextIntPredicate<C> {
        boolean test(RoomRuntime room, C context, int value);
    }

    <C> boolean testInt(C context, int value, ContextIntPredicate<C> operation) {
        RoomRuntime room = current;
        return room != null && operation.test(room, context, value);
    }

    <C> void updateOr(C context, Consumer<C> absent, BiConsumer<RoomRuntime, C> operation) {
        RoomRuntime room = current;
        if (room == null) absent.accept(context);
        else operation.accept(room, context);
    }
}
