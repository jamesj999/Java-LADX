package linksawakening.app.wiring;

import java.util.Objects;
import linksawakening.app.wiring.PlayerWiring.PlayerGraphics;
import linksawakening.entity.Link;
import linksawakening.equipment.Arrow;
import linksawakening.equipment.Bomb;
import linksawakening.equipment.Boomerang;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.MagicPowder;
import linksawakening.equipment.MagicRod;
import linksawakening.equipment.Ocarina;
import linksawakening.equipment.RocsFeather;
import linksawakening.equipment.Shield;
import linksawakening.equipment.Shovel;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.room.ArrowShotResult;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.ShovelStartResult;

/**
 * Registers equipment in ROM order. Constructed only during session wiring. Stateless factory used during
 * session initialization.
 */
final class EquipmentWiring {
    private EquipmentWiring() {}

    static void shield(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.SHIELD.romValue(),
                new Shield(c.gameplaySoundSink(), c.link().equipment()::canUseItems));
    }

    static void feather(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.ROCS_FEATHER.romValue(), new RocsFeather(c.link().airborne()::useRocsFeather));
    }

    static void bomb(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOMBS.romValue(),
                new Bomb(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new RoomBombPlacement(c.link(), c.roomSession().playerItems()),
                        c.link().equipment()::canUseItems));
    }

    /** Dependencies shared by equipment target adapters; no session reference is retained. */
    record EquipmentInputs(
            PlayerState playerState,
            Link link,
            RoomSession roomSession,
            RomTables romTables,
            PlayerGraphics graphics,
            GameplaySoundSink gameplaySoundSink,
            RomRandomByteSource random) {
        EquipmentInputs {
            Objects.requireNonNull(playerState);
            Objects.requireNonNull(link);
            Objects.requireNonNull(roomSession);
            Objects.requireNonNull(romTables);
            Objects.requireNonNull(graphics);
            Objects.requireNonNull(gameplaySoundSink);
            Objects.requireNonNull(random);
        }
    }

    static void sword(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.SWORD.romValue(),
                new Sword(
                        c.romTables(),
                        c.graphics().swordSpriteSheet(),
                        c.gameplaySoundSink(),
                        c.random(),
                        c.graphics().swordPalette(),
                        new Sword.BeamTarget() {
                            @Override
                            public boolean fireSwordBeam() {
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                if (c.playerState().equipmentLevels().swordLevel() != 2
                                        || !c.playerState().vitals().fullHearts()
                                        || c.roomSession().playerItems().activeProjectileCount() != 0) {
                                    return false;
                                }
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireSwordBeam(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection);
                                if (spawned) {
                                    c.link().equipment().startRomItemAttackStep();
                                }
                                return spawned;
                            }
                        }));
    }

    static void magicRod(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_ROD.romValue(),
                new MagicRod(
                        c.romTables(),
                        c.graphics().linkSpriteSheet(),
                        c.graphics().linkTunicPalette(),
                        c.gameplaySoundSink(),
                        new MagicRod.LaunchTarget() {
                            @Override
                            public void startMagicRodAttackStep() {

                                c.link().equipment().startRomMagicRodAttackStep();
                            }

                            @Override
                            public boolean fireMagicRodFireball() {
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireMagicRodFireball(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection,
                                                c.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                return spawned;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }

    static void magicPowder(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_POWDER.romValue(),
                new MagicPowder(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new MagicPowder.SprinkleTarget() {
                            @Override
                            public boolean sprinkleMagicPowder() {
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                return c.roomSession()
                                        .playerItems()
                                        .sprinkleMagicPowder(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection);
                            }

                            @Override
                            public void startMagicPowderAttackStep() {

                                c.link().equipment().startRomMagicPowderAttackStep();
                            }

                            @Override
                            public boolean magicPowderAttackStepActive() {
                                return c.link().equipment().romAttackStepAnimationCountdown() != 0;
                            }

                            @Override
                            public int linkPositionZ() {
                                return c.link().animation().romEntityZ();
                            }

                            @Override
                            public void presentToadstool() {
                                c.link().animation().presentToadstool();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }

    static void arrow(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOW.romValue(),
                new Arrow(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new Arrow.ShootTarget() {
                            private boolean playWhooshForLastShot;

                            @Override
                            public boolean shootArrow() {
                                ArrowShotResult result = c.roomSession()
                                        .playerItems()
                                        .shootArrowResult(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                c.link().equipment().applyRomItemDirectionFromInput(),
                                                c.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                playWhooshForLastShot = result.playWhoosh();
                                if (result.spawned()) {
                                    c.link().equipment().startRomItemAttackStep();
                                }
                                return result.spawned();
                            }

                            @Override
                            public boolean playWhooshForLastShot() {
                                return playWhooshForLastShot;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }

    static void ocarina(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.OCARINA.romValue(),
                new Ocarina(c.playerState(), c.gameplaySoundSink(), new Ocarina.PlaybackTarget() {
                    @Override
                    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
                        return c.roomSession().transitions().startOcarina(countdown, songFlags, selectedSong);
                    }

                    @Override
                    public boolean ocarinaPlaying() {
                        return c.roomSession().transitions().ocarinaPlaying();
                    }

                    @Override
                    public boolean canStartOcarina() {
                        return c.link().equipment().canUseItems()
                                && !c.link().airborne().isAirborne()
                                && !c.roomSession().playerItems().hookshotActive();
                    }

                    @Override
                    public int ocarinaAnimationState() {
                        return c.roomSession().transitions().ocarinaAnimationState();
                    }
                }));
    }

    static void shovel(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.SHOVEL.romValue(),
                new Shovel(
                        c.gameplaySoundSink(),
                        new Shovel.DigTarget() {
                            private int linkEntityX;
                            private int linkEntityY;
                            private int romDirection;

                            @Override
                            public Shovel.StartResult startShovel() {
                                romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                linkEntityX = c.link().position().romEntityX();
                                linkEntityY = c.link().position().romEntityY();
                                ShovelStartResult result = c.roomSession()
                                        .playerItems()
                                        .startShovel(
                                                linkEntityX,
                                                linkEntityY,
                                                romDirection,
                                                c.link().airborne().isAirborne());
                                return new Shovel.StartResult(result.started(), result.poking());
                            }

                            @Override
                            public void advanceShovel(int timer) {

                                c.roomSession()
                                        .playerItems()
                                        .advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
                            }

                            @Override
                            public int shovelAnimationState(int javaDirection, int timer) {
                                return c.roomSession()
                                        .playerItems()
                                        .shovelAnimationState(
                                                linksawakening.state.Direction.fromJava(javaDirection), timer);
                            }
                        },
                        c.link().equipment()::canUseItems));
    }

    static void hookshot(EquipmentInputs c, ItemRegistry registry) {
        registry.register(InventoryItem.HOOKSHOT.romValue(), new Hookshot(new Hookshot.LaunchTarget() {
            @Override
            public boolean fireHookshot() {
                return c.roomSession()
                        .playerItems()
                        .fireHookshot(
                                c.link().position().romEntityX(),
                                c.link().position().romEntityY(),
                                c.link().animation().romEntityZ(),
                                Direction.fromJava(c.link().animation().direction())
                                        .romIndex(),
                                c.link().airborne().isAirborne(),
                                c.link().equipment().isRomLinkPushing());
            }

            @Override
            public boolean hookshotActive() {
                return c.roomSession().playerItems().hookshotActive();
            }
        }));
    }

    static void boomerang(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOOMERANG.romValue(),
                new Boomerang(
                        new Boomerang.LaunchTarget() {
                            @Override
                            public boolean fireBoomerang() {
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireBoomerang(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                c.link().equipment().applyRomItemDirectionFromInput(),
                                                c.link().controls().romPressedButtonsMask());
                                if (spawned) {
                                    c.link().equipment().startRomItemAttackStep();
                                }
                                return spawned;
                            }

                            @Override
                            public boolean boomerangActive() {
                                return c.roomSession().playerItems().boomerangActive();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }
}
