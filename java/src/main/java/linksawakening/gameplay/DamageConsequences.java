package linksawakening.gameplay;

/** Caller-supplied damage side effects, retained by combat consumers and invoked in source order. */
public interface DamageConsequences {

    DamageConsequences NONE = new DamageConsequences() {
        @Override
        public void contactHurt() {}

        @Override
        public void powerUpEnded() {}
    };

    void contactHurt();

    void powerUpEnded();
}
