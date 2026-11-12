package linksawakening.world.entity;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.event.EntityEvent;

/** Ordered entity effects with stable typed channels; scans never allocate iterators or predicates. */
final class EntityOutbox {
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<EntityEvent> entries = new ArrayList<>();
    private final ArrayList<Channel<?>> channels = new ArrayList<>();

    <T extends EntityEvent> List<T> drain(Class<T> type, String name) {
        List<T> events = channel(type, name);
        int size = events.size();
        if (size == 0) return List.of();
        ArrayList<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(events.get(i));
        List<T> snapshot = List.copyOf(result);
        events.clear();
        return snapshot;
    }

    /** Class filtering allows different payload types on the same named channel. */
    @SuppressWarnings("unchecked")
    <T extends EntityEvent> List<T> channel(Class<T> type, String name) {
        for (int i = 0; i < channels.size(); i++) {
            Channel<?> channel = channels.get(i);
            if (channel.type == type && channel.name.equals(name)) return (List<T>) channel;
        }
        Channel<T> channel = new Channel<>(type, name);
        channels.add(channel);
        return channel;
    }

    private final class Channel<T extends EntityEvent> extends AbstractList<T> {
        private final Class<T> type;
        private final String name;

        private Channel(Class<T> type, String name) {
            this.type = type;
            this.name = name;
        }

        private boolean matches(int index) {
            return names.get(index).equals(name) && type.isInstance(entries.get(index));
        }

        @Override
        public int size() {
            int count = 0;
            for (int i = 0; i < entries.size(); i++) if (matches(i)) count++;
            return count;
        }

        @Override
        public T get(int index) {
            Objects.checkIndex(index, size());
            for (int i = 0; i < entries.size(); i++) {
                if (matches(i) && index-- == 0) return type.cast(entries.get(i));
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public boolean add(T event) {
            T checked = type.cast(Objects.requireNonNull(event));
            names.add(name);
            entries.add(checked);
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends T> values) {
            if (values instanceof List<? extends T> list) {
                int size = list.size();
                for (int i = 0; i < size; i++) add(list.get(i));
                return size != 0;
            }
            return super.addAll(values);
        }

        @Override
        public void clear() {
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (matches(i)) {
                    entries.remove(i);
                    names.remove(i);
                }
            }
        }
    }
}
