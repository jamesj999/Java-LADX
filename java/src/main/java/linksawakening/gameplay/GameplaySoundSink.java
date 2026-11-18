package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffectNamespace;

public interface GameplaySoundSink {
    void play(GameplaySoundEvent event);

    default void playRomSound(SoundEffectNamespace namespace, int id) {
        GameplaySoundEvent event = GameplaySoundEvent.forEntityWrite(namespace, id);
        if (event != null) play(event);
    }

    static GameplaySoundSink none() {
        return event -> {
        };
    }
}
