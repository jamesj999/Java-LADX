package linksawakening.world.entity;

/** Presentation decisions retained across the ordered slot phases. */
public final class EntityPresentationFlags {
    private enum Kind {
        GHINI,
        MAD_BOMBER,
        BOMBITE,
        IRON_MASK,
        SNAKE,
        STAR,
        BLOOPER,
        WINGED_OCTOROK,
        PINCER,
        BUSH_CRAWLER,
        MIMIC,
        MASKED_MIMIC,
        MINI_MOLDORM,
        MOLDORM,
        CUCCO,
        GOPONGA_FLOWER,
        GIANT_GOPONGA,
        GOPONGA_PROJECTILE,
        POKEY,
        PIRANHA,
        ZORA,
        ZOMBIE,
        BUZZ_BLOB,
        SAND_CRAB,
        DOG,
        URCHIN,
        WITCH_RAT,
        WITCH,
        ROOSTER,
        WIZROBE,
        WIZROBE_PROJECTILE,
        POLS_VOICE,
        SPIKED_BEETLE,
        ARMOS_KNIGHT,
        HINOX,
        ROLLING_BONES,
        THREE_OF_A_KIND,
        MOBLIN_KING,
        SECRET_SEASHELL,
        DROPPABLE,
        TARIN
    }

    private final java.util.EnumSet<Kind> preserved = java.util.EnumSet.noneOf(Kind.class);

    public boolean preserveGhiniPresentation() {
        return preserved.contains(Kind.GHINI);
    }

    public boolean preserveGhiniPresentation(boolean value) {
        return set(Kind.GHINI, value);
    }

    public boolean preserveMadBomberPresentation() {
        return preserved.contains(Kind.MAD_BOMBER);
    }

    public boolean preserveMadBomberPresentation(boolean value) {
        return set(Kind.MAD_BOMBER, value);
    }

    public boolean preserveBombitePresentation() {
        return preserved.contains(Kind.BOMBITE);
    }

    public boolean preserveBombitePresentation(boolean value) {
        return set(Kind.BOMBITE, value);
    }

    public boolean preserveIronMaskPresentation() {
        return preserved.contains(Kind.IRON_MASK);
    }

    public boolean preserveIronMaskPresentation(boolean value) {
        return set(Kind.IRON_MASK, value);
    }

    public boolean preserveSnakePresentation() {
        return preserved.contains(Kind.SNAKE);
    }

    public boolean preserveSnakePresentation(boolean value) {
        return set(Kind.SNAKE, value);
    }

    public boolean preserveStarPresentation() {
        return preserved.contains(Kind.STAR);
    }

    public boolean preserveStarPresentation(boolean value) {
        return set(Kind.STAR, value);
    }

    public boolean preserveBlooperPresentation() {
        return preserved.contains(Kind.BLOOPER);
    }

    public boolean preserveBlooperPresentation(boolean value) {
        return set(Kind.BLOOPER, value);
    }

    public boolean preserveWingedOctorokPresentation() {
        return preserved.contains(Kind.WINGED_OCTOROK);
    }

    public boolean preserveWingedOctorokPresentation(boolean value) {
        return set(Kind.WINGED_OCTOROK, value);
    }

    public boolean preservePincerPresentation() {
        return preserved.contains(Kind.PINCER);
    }

    public boolean preservePincerPresentation(boolean value) {
        return set(Kind.PINCER, value);
    }

    public boolean preserveBushCrawlerPresentation() {
        return preserved.contains(Kind.BUSH_CRAWLER);
    }

    public boolean preserveBushCrawlerPresentation(boolean value) {
        return set(Kind.BUSH_CRAWLER, value);
    }

    public boolean preserveMimicPresentation() {
        return preserved.contains(Kind.MIMIC);
    }

    public boolean preserveMimicPresentation(boolean value) {
        return set(Kind.MIMIC, value);
    }

    public boolean preserveMaskedMimicPresentation() {
        return preserved.contains(Kind.MASKED_MIMIC);
    }

    public boolean preserveMaskedMimicPresentation(boolean value) {
        return set(Kind.MASKED_MIMIC, value);
    }

    public boolean preserveMiniMoldormPresentation() {
        return preserved.contains(Kind.MINI_MOLDORM);
    }

    public boolean preserveMiniMoldormPresentation(boolean value) {
        return set(Kind.MINI_MOLDORM, value);
    }

    public boolean preserveMoldormPresentation() {
        return preserved.contains(Kind.MOLDORM);
    }

    public boolean preserveMoldormPresentation(boolean value) {
        return set(Kind.MOLDORM, value);
    }

    public boolean preserveCuccoPresentation() {
        return preserved.contains(Kind.CUCCO);
    }

    public boolean preserveCuccoPresentation(boolean value) {
        return set(Kind.CUCCO, value);
    }

    public boolean preserveGopongaFlowerPresentation() {
        return preserved.contains(Kind.GOPONGA_FLOWER);
    }

    public boolean preserveGopongaFlowerPresentation(boolean value) {
        return set(Kind.GOPONGA_FLOWER, value);
    }

    public boolean preserveGiantGopongaPresentation() {
        return preserved.contains(Kind.GIANT_GOPONGA);
    }

    public boolean preserveGiantGopongaPresentation(boolean value) {
        return set(Kind.GIANT_GOPONGA, value);
    }

    public boolean preserveGopongaProjectilePresentation() {
        return preserved.contains(Kind.GOPONGA_PROJECTILE);
    }

    public boolean preserveGopongaProjectilePresentation(boolean value) {
        return set(Kind.GOPONGA_PROJECTILE, value);
    }

    public boolean preservePokeyPresentation() {
        return preserved.contains(Kind.POKEY);
    }

    public boolean preservePokeyPresentation(boolean value) {
        return set(Kind.POKEY, value);
    }

    public boolean preservePiranhaPresentation() {
        return preserved.contains(Kind.PIRANHA);
    }

    public boolean preservePiranhaPresentation(boolean value) {
        return set(Kind.PIRANHA, value);
    }

    public boolean preserveZoraPresentation() {
        return preserved.contains(Kind.ZORA);
    }

    public boolean preserveZoraPresentation(boolean value) {
        return set(Kind.ZORA, value);
    }

    public boolean preserveZombiePresentation() {
        return preserved.contains(Kind.ZOMBIE);
    }

    public boolean preserveZombiePresentation(boolean value) {
        return set(Kind.ZOMBIE, value);
    }

    public boolean preserveBuzzBlobPresentation() {
        return preserved.contains(Kind.BUZZ_BLOB);
    }

    public boolean preserveBuzzBlobPresentation(boolean value) {
        return set(Kind.BUZZ_BLOB, value);
    }

    public boolean preserveSandCrabPresentation() {
        return preserved.contains(Kind.SAND_CRAB);
    }

    public boolean preserveSandCrabPresentation(boolean value) {
        return set(Kind.SAND_CRAB, value);
    }

    public boolean preserveDogPresentation() {
        return preserved.contains(Kind.DOG);
    }

    public boolean preserveDogPresentation(boolean value) {
        return set(Kind.DOG, value);
    }

    public boolean preserveUrchinPresentation() {
        return preserved.contains(Kind.URCHIN);
    }

    public boolean preserveUrchinPresentation(boolean value) {
        return set(Kind.URCHIN, value);
    }

    public boolean preserveWitchRatPresentation() {
        return preserved.contains(Kind.WITCH_RAT);
    }

    public boolean preserveWitchRatPresentation(boolean value) {
        return set(Kind.WITCH_RAT, value);
    }

    public boolean preserveWitchPresentation() {
        return preserved.contains(Kind.WITCH);
    }

    public boolean preserveWitchPresentation(boolean value) {
        return set(Kind.WITCH, value);
    }

    public boolean preserveRoosterPresentation() {
        return preserved.contains(Kind.ROOSTER);
    }

    public boolean preserveRoosterPresentation(boolean value) {
        return set(Kind.ROOSTER, value);
    }

    public boolean preserveWizrobePresentation() {
        return preserved.contains(Kind.WIZROBE);
    }

    public boolean preserveWizrobePresentation(boolean value) {
        return set(Kind.WIZROBE, value);
    }

    public boolean preserveWizrobeProjectilePresentation() {
        return preserved.contains(Kind.WIZROBE_PROJECTILE);
    }

    public boolean preserveWizrobeProjectilePresentation(boolean value) {
        return set(Kind.WIZROBE_PROJECTILE, value);
    }

    public boolean preservePolsVoicePresentation() {
        return preserved.contains(Kind.POLS_VOICE);
    }

    public boolean preservePolsVoicePresentation(boolean value) {
        return set(Kind.POLS_VOICE, value);
    }

    public boolean preserveSpikedBeetlePresentation() {
        return preserved.contains(Kind.SPIKED_BEETLE);
    }

    public boolean preserveSpikedBeetlePresentation(boolean value) {
        return set(Kind.SPIKED_BEETLE, value);
    }

    public boolean preserveArmosKnightPresentation() {
        return preserved.contains(Kind.ARMOS_KNIGHT);
    }

    public boolean preserveArmosKnightPresentation(boolean value) {
        return set(Kind.ARMOS_KNIGHT, value);
    }

    public boolean preserveHinoxPresentation() {
        return preserved.contains(Kind.HINOX);
    }

    public boolean preserveHinoxPresentation(boolean value) {
        return set(Kind.HINOX, value);
    }

    public boolean preserveRollingBonesPresentation() {
        return preserved.contains(Kind.ROLLING_BONES);
    }

    public boolean preserveRollingBonesPresentation(boolean value) {
        return set(Kind.ROLLING_BONES, value);
    }

    public boolean preserveThreeOfAKindPresentation() {
        return preserved.contains(Kind.THREE_OF_A_KIND);
    }

    public boolean preserveThreeOfAKindPresentation(boolean value) {
        return set(Kind.THREE_OF_A_KIND, value);
    }

    public boolean preserveMoblinKingPresentation() {
        return preserved.contains(Kind.MOBLIN_KING);
    }

    public boolean preserveMoblinKingPresentation(boolean value) {
        return set(Kind.MOBLIN_KING, value);
    }

    public boolean preserveSecretSeashellPresentation() {
        return preserved.contains(Kind.SECRET_SEASHELL);
    }

    public boolean preserveSecretSeashellPresentation(boolean value) {
        return set(Kind.SECRET_SEASHELL, value);
    }

    public boolean preserveDroppablePresentation() {
        return preserved.contains(Kind.DROPPABLE);
    }

    public boolean preserveDroppablePresentation(boolean value) {
        return set(Kind.DROPPABLE, value);
    }

    public boolean preserveTarinPresentation() {
        return preserved.contains(Kind.TARIN);
    }

    public boolean preserveTarinPresentation(boolean value) {
        return set(Kind.TARIN, value);
    }

    boolean preservesHandlerPresentation() {
        return !preserved.isEmpty();
    }

    private boolean set(Kind kind, boolean value) {
        if (value) preserved.add(kind);
        else preserved.remove(kind);
        return value;
    }

    void reset() {
        preserved.clear();
    }
}
