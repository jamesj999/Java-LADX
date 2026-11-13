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
 * Constructs equipment and its room-command adapters in the session registration order.
 * The adapters retain only EquipmentInputs, never the owning GameSession.
 */
final class EquipmentWiring {
    private EquipmentWiring() {}

    static void shield(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.SHIELD.romValue(),
                new Shield(inputs.gameplaySoundSink(), inputs.link().equipment()::canUseItems));
    }

    static void feather(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.ROCS_FEATHER.romValue(),
                new RocsFeather(inputs.link().airborne()::useRocsFeather));
    }

    static void bomb(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOMBS.romValue(),
                new Bomb(
                        inputs.playerState(),
                        inputs.gameplaySoundSink(),
                        new RoomBombPlacement(
                                inputs.link(), inputs.roomSession().playerItems()),
                        inputs.link().equipment()::canUseItems));
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

    static void sword(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.SWORD.romValue(),
                new Sword(
                        new Sword.Assets(
                                inputs.romTables().swordTables(),
                                inputs.graphics().swordSpriteSheet()),
                        inputs.gameplaySoundSink(),
                        inputs.random(),
                        inputs.graphics().swordPalette(),
                        new Sword.BeamTarget() {
                            @Override
                            public boolean fireSwordBeam() {
                                int romDirection = inputs.link().equipment().applyRomItemDirectionFromInput();
                                if (inputs.playerState().equipmentLevels().swordLevel() != 2
                                        || !inputs.playerState().vitals().fullHearts()
                                        || inputs.roomSession().playerItems().activeProjectileCount() != 0) {
                                    return false;
                                }
                                boolean spawned = inputs.roomSession()
                                        .playerItems()
                                        .fireSwordBeam(
                                                inputs.link().position().romEntityX(),
                                                inputs.link().position().romEntityY(),
                                                inputs.link().animation().romEntityZ(),
                                                romDirection);
                                if (spawned) {
                                    inputs.link().equipment().startRomItemAttackStep();
                                }
                                return spawned;
                            }
                        }));
    }

    static void magicRod(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_ROD.romValue(),
                new MagicRod(
                        inputs.romTables(),
                        inputs.graphics().linkSpriteSheet(),
                        inputs.graphics().linkTunicPalette(),
                        inputs.gameplaySoundSink(),
                        new MagicRod.LaunchTarget() {
                            @Override
                            public void startMagicRodAttackStep() {

                                inputs.link().equipment().startRomMagicRodAttackStep();
                            }

                            @Override
                            public boolean fireMagicRodFireball() {
                                int romDirection = inputs.link().equipment().applyRomItemDirectionFromInput();
                                boolean spawned = inputs.roomSession()
                                        .playerItems()
                                        .fireMagicRodFireball(
                                                inputs.link().position().romEntityX(),
                                                inputs.link().position().romEntityY(),
                                                inputs.link().animation().romEntityZ(),
                                                romDirection,
                                                inputs.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                return spawned;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return inputs.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        inputs.link().equipment()::canUseItems));
    }

    static void magicPowder(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_POWDER.romValue(),
                new MagicPowder(
                        inputs.playerState(),
                        inputs.gameplaySoundSink(),
                        new MagicPowder.SprinkleTarget() {
                            @Override
                            public boolean sprinkleMagicPowder() {
                                int romDirection = inputs.link().equipment().applyRomItemDirectionFromInput();
                                return inputs.roomSession()
                                        .playerItems()
                                        .sprinkleMagicPowder(
                                                inputs.link().position().romEntityX(),
                                                inputs.link().position().romEntityY(),
                                                inputs.link().animation().romEntityZ(),
                                                romDirection);
                            }

                            @Override
                            public void startMagicPowderAttackStep() {

                                inputs.link().equipment().startRomMagicPowderAttackStep();
                            }

                            @Override
                            public boolean magicPowderAttackStepActive() {
                                return inputs.link().equipment().romAttackStepAnimationCountdown() != 0;
                            }

                            @Override
                            public int linkPositionZ() {
                                return inputs.link().animation().romEntityZ();
                            }

                            @Override
                            public void presentToadstool() {
                                inputs.link().animation().presentToadstool();
                            }
                        },
                        inputs.link().equipment()::canUseItems));
    }

    static void arrow(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOW.romValue(),
                new Arrow(
                        inputs.playerState(),
                        inputs.gameplaySoundSink(),
                        new Arrow.ShootTarget() {
                            private boolean playWhooshForLastShot;

                            @Override
                            public boolean shootArrow() {
                                ArrowShotResult result = inputs.roomSession()
                                        .playerItems()
                                        .shootArrowResult(
                                                inputs.link().position().romEntityX(),
                                                inputs.link().position().romEntityY(),
                                                inputs.link().animation().romEntityZ(),
                                                inputs.link().equipment().applyRomItemDirectionFromInput(),
                                                inputs.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                playWhooshForLastShot = result.playWhoosh();
                                if (result.spawned()) {
                                    inputs.link().equipment().startRomItemAttackStep();
                                }
                                return result.spawned();
                            }

                            @Override
                            public boolean playWhooshForLastShot() {
                                return playWhooshForLastShot;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return inputs.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        inputs.link().equipment()::canUseItems));
    }

    static void ocarina(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.OCARINA.romValue(),
                new Ocarina(inputs.playerState(), inputs.gameplaySoundSink(), new Ocarina.PlaybackTarget() {
                    @Override
                    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
                        return inputs.roomSession().transitions().startOcarina(countdown, songFlags, selectedSong);
                    }

                    @Override
                    public boolean ocarinaPlaying() {
                        return inputs.roomSession().transitions().ocarinaPlaying();
                    }

                    @Override
                    public boolean canStartOcarina() {
                        return inputs.link().equipment().canUseItems()
                                && !inputs.link().airborne().isAirborne()
                                && !inputs.roomSession().playerItems().hookshotActive();
                    }

                    @Override
                    public int ocarinaAnimationState() {
                        return inputs.roomSession().transitions().ocarinaAnimationState();
                    }
                }));
    }

    static void shovel(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.SHOVEL.romValue(),
                new Shovel(
                        inputs.gameplaySoundSink(),
                        new Shovel.DigTarget() {
                            private int linkEntityX;
                            private int linkEntityY;
                            private int romDirection;

                            @Override
                            public Shovel.StartResult startShovel() {
                                romDirection = inputs.link().equipment().applyRomItemDirectionFromInput();
                                linkEntityX = inputs.link().position().romEntityX();
                                linkEntityY = inputs.link().position().romEntityY();
                                ShovelStartResult result = inputs.roomSession()
                                        .playerItems()
                                        .startShovel(
                                                linkEntityX,
                                                linkEntityY,
                                                romDirection,
                                                inputs.link().airborne().isAirborne());
                                return new Shovel.StartResult(result.started(), result.poking());
                            }

                            @Override
                            public void advanceShovel(int timer) {

                                inputs.roomSession()
                                        .playerItems()
                                        .advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
                            }

                            @Override
                            public int shovelAnimationState(int javaDirection, int timer) {
                                return inputs.roomSession()
                                        .playerItems()
                                        .shovelAnimationState(
                                                linksawakening.state.Direction.fromJava(javaDirection), timer);
                            }
                        },
                        inputs.link().equipment()::canUseItems));
    }

    static void hookshot(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(InventoryItem.HOOKSHOT.romValue(), new Hookshot(new Hookshot.LaunchTarget() {
            @Override
            public boolean fireHookshot() {
                return inputs.roomSession()
                        .playerItems()
                        .fireHookshot(
                                inputs.link().position().romEntityX(),
                                inputs.link().position().romEntityY(),
                                inputs.link().animation().romEntityZ(),
                                Direction.fromJava(inputs.link().animation().direction())
                                        .romIndex(),
                                inputs.link().airborne().isAirborne(),
                                inputs.link().equipment().isRomLinkPushing());
            }

            @Override
            public boolean hookshotActive() {
                return inputs.roomSession().playerItems().hookshotActive();
            }
        }));
    }

    static void boomerang(EquipmentInputs inputs, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOOMERANG.romValue(),
                new Boomerang(
                        new Boomerang.LaunchTarget() {
                            @Override
                            public boolean fireBoomerang() {
                                boolean spawned = inputs.roomSession()
                                        .playerItems()
                                        .fireBoomerang(
                                                inputs.link().position().romEntityX(),
                                                inputs.link().position().romEntityY(),
                                                inputs.link().animation().romEntityZ(),
                                                inputs.link().equipment().applyRomItemDirectionFromInput(),
                                                inputs.link().controls().romPressedButtonsMask());
                                if (spawned) {
                                    inputs.link().equipment().startRomItemAttackStep();
                                }
                                return spawned;
                            }

                            @Override
                            public boolean boomerangActive() {
                                return inputs.roomSession().playerItems().boomerangActive();
                            }
                        },
                        inputs.link().equipment()::canUseItems));
    }
}
