package linksawakening.state;

/** Named player setup for tests; defaults are the production development loadout. */
public final class PlayerStateTestBuilder {
    private final PlayerState player = new PlayerState();

    public PlayerStateTestBuilder newGame(int arrows, int bombs, int powder) {
        player.playerInitialization().initializeNewGame(arrows, bombs, powder);
        return this;
    }

    public PlayerStateTestBuilder hearts(int hearts) {
        player.vitals().setMaxHearts(hearts);
        return this;
    }

    public PlayerStateTestBuilder health(int health) {
        player.vitals().setHealth(health);
        return this;
    }

    public PlayerStateTestBuilder items(InventoryItem a, InventoryItem b) {
        player.inventory().setItemA(a.romValue());
        player.inventory().setItemB(b.romValue());
        return this;
    }

    public PlayerState build() {
        return player;
    }
}
