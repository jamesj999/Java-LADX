package linksawakening.state;

import static linksawakening.state.PlayerState.*;

/** Shared state and dependency ownership for PlayerState. */
abstract class PlayerStateComponents implements LinkPlayerState {

    protected final Inventory inventory = new Inventory();
    protected final Progression progression = new Progression();
    protected final EquipmentLevels equipmentLevels = new EquipmentLevels();
    protected final PlayerSounds playerSounds = new PlayerSounds();
    protected final Ammunition ammunition = new Ammunition(inventory);
    protected final Vitals vitals = new Vitals(equipmentLevels, playerSounds, progression);
    protected final Wallet wallet = new Wallet(playerSounds);
    protected final PlayerResources playerResources = new PlayerResources(playerSounds, vitals, wallet);
    protected final PlayerRewards playerRewards =
            new PlayerRewards(ammunition, equipmentLevels, inventory, progression, vitals, wallet);
    protected final ChestRewards chestRewards =
            new ChestRewards(ammunition, equipmentLevels, inventory, progression, wallet);
    protected final PlayerInitialization playerInitialization =
            new PlayerInitialization(ammunition, equipmentLevels, inventory, playerSounds, progression, vitals, wallet);
    protected final PlayerSaveLoader playerSaveLoader =
            new PlayerSaveLoader(ammunition, equipmentLevels, inventory, playerSounds, progression, vitals, wallet);
}
