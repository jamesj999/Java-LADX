package linksawakening.app.gameplay;

import linksawakening.gameplay.GameplaySoundEvent;

/** Side-effect ports for the ROM music/dialog/warp sequence. */
interface EntityDialogRouting {
    EntityDialogRouting DIRECT = new EntityDialogRouting() {
        public void music(GameplaySimulation c, int track) {
            c.musicDirector().playDirectMusic(track);
        }

        public void dialogs(GameplaySimulation c) {
            c.dialogInteractions().openEntityDialogRequests();
        }

        public void manbo(GameplaySimulation c) {
            if (c.roomTransitionCoordinator().handlePendingManboTransition(c.link()))
                c.gameplaySoundSink().play(GameplaySoundEvent.MANBO_WARP);
        }

        public void instrument(GameplaySimulation c) {
            c.roomTransitionCoordinator().handlePendingInstrumentTransition(c.link());
        }
    };

    void music(GameplaySimulation c, int track);

    void dialogs(GameplaySimulation c);

    void manbo(GameplaySimulation c);

    void instrument(GameplaySimulation c);
}
