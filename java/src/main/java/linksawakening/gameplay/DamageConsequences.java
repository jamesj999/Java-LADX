package linksawakening.gameplay;

/** Caller-supplied damage side effects, retained by combat consumers and invoked in source order. */
public interface DamageConsequences {
    void contactHurt();

    void powerUpEnded();

    DamageConsequences NONE = new DamageConsequences() {
        public void contactHurt() {}

        public void powerUpEnded() {}
    };
}
