package linksawakening.world;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One ordered store for entity effects, with typed views for legacy consumers. */
final class EntityOutbox {
    private record Entry(String channel, EntityEvent event) {}
    private final List<Entry> entries = new ArrayList<>();

    /** Separate channels preserve pre-tick staging even for the same payload type. */
    <T extends EntityEvent> List<T> channel(Class<T> type, String channel) {
        return new AbstractList<>() {
            private boolean matches(Entry entry) {
                return entry.channel().equals(channel) && type.isInstance(entry.event());
            }

            @Override public int size() {
                int count = 0;
                for (Entry entry : entries) if (matches(entry)) count++;
                return count;
            }

            @Override public T get(int index) {
                Objects.checkIndex(index, size());
                for (Entry entry : entries) {
                    if (matches(entry) && index-- == 0) return type.cast(entry.event());
                }
                throw new IndexOutOfBoundsException();
            }

            @Override public boolean add(T event) {
                entries.add(new Entry(channel, type.cast(Objects.requireNonNull(event))));
                return true;
            }

            @Override public void clear() {
                entries.removeIf(this::matches);
            }
        };
    }
}
