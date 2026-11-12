package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.Arrow;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.PlayerState;
import linksawakening.world.room.ArrowShotResult;

/** Registers the arrow and its room-facing adapter. */
final class ArrowWiring {
    private ArrowWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                PlayerState.INVENTORY_BOW,
                new Arrow(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new Arrow.ShootTarget() {
                            private boolean playWhooshForLastShot;

                            @Override
                            public boolean shootArrow() {
                                if (c.link() == null || c.roomSession() == null) {
                                    playWhooshForLastShot = false;
                                    return false;
                                }
                                ArrowShotResult result = c.roomSession()
                                        .playerItems()
                                        .shootArrowResult(
                                                c.link().romEntityX(),
                                                c.link().romEntityY(),
                                                c.link().romEntityZ(),
                                                c.link().applyRomItemDirectionFromInput(),
                                                c.playerState().activePowerUp()
                                                        == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
                                playWhooshForLastShot = result.playWhoosh();
                                if (result.spawned()) {
                                    c.link().startRomItemAttackStep();
                                }
                                return result.spawned();
                            }

                            @Override
                            public boolean playWhooshForLastShot() {
                                return playWhooshForLastShot;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession() == null
                                        ? 0
                                        : c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link()::canUseItems));
    }
}
