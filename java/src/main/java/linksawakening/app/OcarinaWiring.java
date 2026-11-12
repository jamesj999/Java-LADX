package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Ocarina;
import linksawakening.state.PlayerState;

/** Registers the ocarina and its room-facing adapter. */
final class OcarinaWiring {
    private OcarinaWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                PlayerState.INVENTORY_OCARINA,
                new Ocarina(c.playerState(), c.gameplaySoundSink(), new Ocarina.PlaybackTarget() {
                    @Override
                    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
                        return c.roomSession() != null
                                && c.roomSession().transitions().startOcarina(countdown, songFlags, selectedSong);
                    }

                    @Override
                    public boolean ocarinaPlaying() {
                        return c.roomSession() != null
                                && c.roomSession().transitions().ocarinaPlaying();
                    }

                    @Override
                    public boolean canStartOcarina() {
                        return c.link() != null
                                && c.roomSession() != null
                                && c.link().canUseItems()
                                && !c.link().isAirborne()
                                && !c.roomSession().playerItems().hookshotActive();
                    }

                    @Override
                    public int ocarinaAnimationState() {
                        return c.roomSession() == null
                                ? -1
                                : c.roomSession().transitions().ocarinaAnimationState();
                    }
                }));
    }
}
