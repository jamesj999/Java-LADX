package linksawakening.world.room;

public record UiInputs(
        boolean dialogActive,
        boolean inventoryAppearing,
        int dialogCooldown,
        int windowY,
        int selectionIndex,
        boolean musicActive) {}
