package linksawakening.world.room;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * Finds composed owners for room fixtures without exposing a production wiring inventory.
 * Reflection is confined here: the facade intentionally exports roles, not its private dependency graph.
 */
final class RoomSessionInspection {
    private RoomSessionInspection() {}

    static RoomRuntime loadedRoom(RoomSession session) {
        return component(session, LoadedRoomHandle.class).currentOrNull();
    }

    static <T> T component(RoomSession session, Class<T> type) {
        var pending = new ArrayDeque<Object>();
        var visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        pending.add(session);
        while (!pending.isEmpty()) {
            Object value = pending.removeFirst();
            if (!visited.add(value)) continue;
            if (type.isInstance(value)) return type.cast(value);
            if (!value.getClass().getPackageName().equals(RoomSession.class.getPackageName())) continue;
            for (var field : value.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    Object child = field.get(value);
                    if (child != null) pending.add(child);
                } catch (IllegalAccessException failure) {
                    throw new LinkageError("Cannot inspect composed room owners", failure);
                }
            }
        }
        throw new AssertionError("Missing composed owner " + type.getName());
    }
}
