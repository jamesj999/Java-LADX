package linksawakening.world.entity;

/** ExecuteEntities timer and interaction decisions shared across the ordered stages of one invocation. */
public final class EntityTimerGates {
    private boolean handlerLinkCollisionEnabled;
    private boolean countdownBlocksInteraction;
    private boolean threeOfAKindInteractive;
    private boolean handlerInteractive;
    private boolean bowWowHandlerInteractive;
    private boolean projectileTimersInteractive;
    private boolean sideViewPotInteractive;
    private boolean sideViewPotLinkCollisionInteractive;
    private boolean freezeTarinTimers;
    private boolean freezeThreeOfAKindTimers;
    private boolean freezeHinoxTimers;
    private boolean freezeGenieTimers;
    private boolean freezeWitchTimers;
    private boolean freezePowderTimers;
    private boolean freezeBooBuddyTimers;
    private boolean freezeBowWowTimers;
    private boolean freezeMoblinKingTimers;
    private boolean ignoreHitsDecrementedBeforeHandler;

    public boolean handlerLinkCollisionEnabled() {
        return handlerLinkCollisionEnabled;
    }

    public boolean handlerLinkCollisionEnabled(boolean value) {
        handlerLinkCollisionEnabled = value;
        return handlerLinkCollisionEnabled;
    }

    public boolean countdownBlocksInteraction() {
        return countdownBlocksInteraction;
    }

    public boolean countdownBlocksInteraction(boolean value) {
        countdownBlocksInteraction = value;
        return countdownBlocksInteraction;
    }

    public boolean threeOfAKindInteractive() {
        return threeOfAKindInteractive;
    }

    public boolean threeOfAKindInteractive(boolean value) {
        threeOfAKindInteractive = value;
        return threeOfAKindInteractive;
    }

    public boolean handlerInteractive() {
        return handlerInteractive;
    }

    public boolean handlerInteractive(boolean value) {
        handlerInteractive = value;
        return handlerInteractive;
    }

    public boolean bowWowHandlerInteractive() {
        return bowWowHandlerInteractive;
    }

    public boolean bowWowHandlerInteractive(boolean value) {
        bowWowHandlerInteractive = value;
        return bowWowHandlerInteractive;
    }

    public boolean projectileTimersInteractive() {
        return projectileTimersInteractive;
    }

    public boolean projectileTimersInteractive(boolean value) {
        projectileTimersInteractive = value;
        return projectileTimersInteractive;
    }

    public boolean sideViewPotInteractive() {
        return sideViewPotInteractive;
    }

    public boolean sideViewPotInteractive(boolean value) {
        sideViewPotInteractive = value;
        return sideViewPotInteractive;
    }

    public boolean sideViewPotLinkCollisionInteractive() {
        return sideViewPotLinkCollisionInteractive;
    }

    public boolean sideViewPotLinkCollisionInteractive(boolean value) {
        sideViewPotLinkCollisionInteractive = value;
        return sideViewPotLinkCollisionInteractive;
    }

    public boolean freezeTarinTimers() {
        return freezeTarinTimers;
    }

    public boolean freezeTarinTimers(boolean value) {
        freezeTarinTimers = value;
        return freezeTarinTimers;
    }

    public boolean freezeThreeOfAKindTimers() {
        return freezeThreeOfAKindTimers;
    }

    public boolean freezeThreeOfAKindTimers(boolean value) {
        freezeThreeOfAKindTimers = value;
        return freezeThreeOfAKindTimers;
    }

    public boolean freezeHinoxTimers() {
        return freezeHinoxTimers;
    }

    public boolean freezeHinoxTimers(boolean value) {
        freezeHinoxTimers = value;
        return freezeHinoxTimers;
    }

    public boolean freezeGenieTimers() {
        return freezeGenieTimers;
    }

    public boolean freezeGenieTimers(boolean value) {
        freezeGenieTimers = value;
        return freezeGenieTimers;
    }

    public boolean freezeWitchTimers() {
        return freezeWitchTimers;
    }

    public boolean freezeWitchTimers(boolean value) {
        freezeWitchTimers = value;
        return freezeWitchTimers;
    }

    public boolean freezePowderTimers() {
        return freezePowderTimers;
    }

    public boolean freezePowderTimers(boolean value) {
        freezePowderTimers = value;
        return freezePowderTimers;
    }

    public boolean freezeBooBuddyTimers() {
        return freezeBooBuddyTimers;
    }

    public boolean freezeBooBuddyTimers(boolean value) {
        freezeBooBuddyTimers = value;
        return freezeBooBuddyTimers;
    }

    public boolean freezeBowWowTimers() {
        return freezeBowWowTimers;
    }

    public boolean freezeBowWowTimers(boolean value) {
        freezeBowWowTimers = value;
        return freezeBowWowTimers;
    }

    public boolean freezeMoblinKingTimers() {
        return freezeMoblinKingTimers;
    }

    public boolean freezeMoblinKingTimers(boolean value) {
        freezeMoblinKingTimers = value;
        return freezeMoblinKingTimers;
    }

    public boolean ignoreHitsDecrementedBeforeHandler() {
        return ignoreHitsDecrementedBeforeHandler;
    }

    public boolean ignoreHitsDecrementedBeforeHandler(boolean value) {
        ignoreHitsDecrementedBeforeHandler = value;
        return ignoreHitsDecrementedBeforeHandler;
    }

    void reset() {
        handlerLinkCollisionEnabled = false;
        countdownBlocksInteraction = false;
        threeOfAKindInteractive = false;
        handlerInteractive = false;
        bowWowHandlerInteractive = false;
        projectileTimersInteractive = false;
        sideViewPotInteractive = false;
        sideViewPotLinkCollisionInteractive = false;
        freezeTarinTimers = false;
        freezeThreeOfAKindTimers = false;
        freezeHinoxTimers = false;
        freezeGenieTimers = false;
        freezeWitchTimers = false;
        freezePowderTimers = false;
        freezeBooBuddyTimers = false;
        freezeBowWowTimers = false;
        freezeMoblinKingTimers = false;
        ignoreHitsDecrementedBeforeHandler = false;
    }
}
