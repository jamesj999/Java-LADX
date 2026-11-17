package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.gameplay.entity.EntityPhase;
import linksawakening.app.gameplay.interaction.DialogBlockers;
import linksawakening.app.gameplay.recovery.PassOutController;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;

/** Sequences the ROM gameplay phases between transition advancement and input completion. */
public final class GameplaySimulation implements GameplayFrame.Simulation, SimulationStages {
    private final InventoryController inventoryController;
    private final InventoryMenu inventoryMenu;
    private final Link link;
    private final PassOutController passOutController;
    private final DialogController dialogController;
    private final DialogBlockers dialogBlockers;
    private final DialogStoryPhase dialogStoryPhase;
    private final LinkEquipmentPhase linkEquipmentPhase;
    private final CombatPhase combatPhase;
    private final EntityPhase entityPhase;
    private final AnimatedTilesPhase animatedTilesPhase;

    public GameplaySimulation(Inputs inputs) {
        this.inventoryController = Objects.requireNonNull(inputs.inventoryController);
        this.inventoryMenu = Objects.requireNonNull(inputs.inventoryMenu);
        this.link = Objects.requireNonNull(inputs.link);
        this.passOutController = Objects.requireNonNull(inputs.passOutController);
        this.dialogController = Objects.requireNonNull(inputs.dialogController);
        this.dialogBlockers = Objects.requireNonNull(inputs.dialogBlockers);
        this.dialogStoryPhase = Objects.requireNonNull(inputs.dialogStoryPhase);
        this.linkEquipmentPhase = Objects.requireNonNull(inputs.linkEquipmentPhase);
        this.combatPhase = Objects.requireNonNull(inputs.combatPhase);
        this.entityPhase = Objects.requireNonNull(inputs.entityPhase);
        this.animatedTilesPhase = Objects.requireNonNull(inputs.animatedTilesPhase);
    }

    public PassOutController passOutController() {
        return passOutController;
    }

    @Override
    public boolean dialogStory() {
        return dialogStoryPhase.tick();
    }

    @Override
    public boolean dialogBlocks() {
        return dialogBlockers.current().pausesGameplay();
    }

    @Override
    public void linkEquipment(boolean blocked, boolean inventory) {
        linkEquipmentPhase.tick(blocked, inventory);
    }

    @Override
    public boolean passingOut() {
        return link.status().isPassingOut() || passOutController.handledFrame();
    }

    @Override
    public Sword.CollisionBox combat(boolean blocked) {
        return combatPhase.tick(blocked);
    }

    @Override
    public void entities(Sword.CollisionBox sword) {
        entityPhase.tick(sword);
    }

    @Override
    public void completeInput() {
        completeGameplayFrame(dialogController);
    }

    @Override
    public void animatedTiles(boolean blocked) {
        animatedTilesPhase.tick(blocked);
    }
    /** The simulation's strict per-frame order, including opening-story gates. */
    @Override
    public void tick() {
        // GAMEPLAY_INVENTORY has its own handler ($00:0EA1/$20:5904):
        // world resources, effects, Link and entities do not advance behind it.
        boolean inventoryBlockedAtStart = inventoryController.shouldBlockOverworldInput();
        if (inventoryBlockedAtStart) {
            boolean worldHandler = inventoryMenu.transition().worldHandlerActive();
            boolean interactive = inventoryMenu.isFullyOpen();
            inventoryController.tick();
            if (interactive && inventoryMenu.isFullyOpen()) inventoryController.dispatchMenuInput();
            if (!worldHandler) return;
        }
        run(inventoryBlockedAtStart);
    }

    public static boolean shouldTickRoomEntities(
            boolean scrollActive,
            boolean transitionInputBlocked,
            boolean inventoryBlocksInput,
            boolean inventoryTransitioning) {
        return !scrollActive && !transitionInputBlocked && !inventoryBlocksInput;
    }

    public static void completeGameplayFrame(DialogController dialog) {
        if (dialog != null) {
            dialog.tickGameplayFrameCooldown();
        }
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private InventoryController inventoryController;
        private InventoryMenu inventoryMenu;
        private Link link;
        private PassOutController passOutController;
        private DialogController dialogController;
        private DialogBlockers dialogBlockers;
        private DialogStoryPhase dialogStoryPhase;
        private LinkEquipmentPhase linkEquipmentPhase;
        private CombatPhase combatPhase;
        private EntityPhase entityPhase;
        private AnimatedTilesPhase animatedTilesPhase;

        public Inputs inventoryController(InventoryController value) {
            this.inventoryController = value;
            return this;
        }

        public Inputs inventoryMenu(InventoryMenu value) {
            this.inventoryMenu = value;
            return this;
        }

        public Inputs link(Link value) {
            this.link = value;
            return this;
        }

        public Inputs passOutController(PassOutController value) {
            this.passOutController = value;
            return this;
        }

        public Inputs dialogController(DialogController value) {
            this.dialogController = value;
            return this;
        }

        public Inputs dialogBlockers(DialogBlockers value) {
            this.dialogBlockers = value;
            return this;
        }

        public Inputs dialogStoryPhase(DialogStoryPhase value) {
            this.dialogStoryPhase = value;
            return this;
        }

        public Inputs linkEquipmentPhase(LinkEquipmentPhase value) {
            this.linkEquipmentPhase = value;
            return this;
        }

        public Inputs combatPhase(CombatPhase value) {
            this.combatPhase = value;
            return this;
        }

        public Inputs entityPhase(EntityPhase value) {
            this.entityPhase = value;
            return this;
        }

        public Inputs animatedTilesPhase(AnimatedTilesPhase value) {
            this.animatedTilesPhase = value;
            return this;
        }
    }
}
