package linksawakening.audio.apu;
/** Receives unsigned register writes without boxing. */
@FunctionalInterface
public interface RegisterObserver {
    void accept(int register, int value);
}
