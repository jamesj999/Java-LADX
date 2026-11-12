package linksawakening.gameplay;

/** Side effects following accepted enemy damage, in the order requested by the consumer. */
public interface DamageConsequences {
    void contactHurt();

    void powerUpEnded();

    DamageConsequences NONE = new DamageConsequences() {
        public void contactHurt() {}

        public void powerUpEnded() {}
    };
}
