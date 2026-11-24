package linksawakening.world.room;

public record EntityTickInputs(
        int frameCounter,
        LinkPosition position,
        LinkMotion motion,
        int shieldLevel,
        SwordBox sword,
        boolean incomingScrollOnly)
        implements EntityTickValues {
    public EntityTickInputs(
            int frameCounter, LinkPosition position, LinkMotion motion, int shieldLevel, SwordBox sword) {
        this(frameCounter, position, motion, shieldLevel, sword, false);
    }
}
