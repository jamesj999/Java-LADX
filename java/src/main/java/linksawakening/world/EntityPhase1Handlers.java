package linksawakening.world;

/** Installs family callbacks at phase 1 dispatch sites. */
final class EntityPhase1Handlers {
  static void install(EntityHandlerRegistry registry) {
    KanaletBombableWallHandler.installPhase1(registry);
    TarinHandler.installPhase1(registry);
    MarinIndoorHandler.installPhase1(registry);
    BooBuddyHandler.installPhase1(registry);
    BowWowHandler.installPhase1(registry);
    MoldormHandler.installPhase1(registry);
    SideViewPotHandler.installPhase1(registry);
    SideViewPotHandler.installPhase1Part2(registry);
    DogHandler.installPhase1(registry);
    HookshotBridgeHandler.installPhase1(registry);
    HookshotChainHandler.installPhase1(registry);
    BoomerangHandler.installPhase1(registry);
    SwordBeamHandler.installPhase1(registry);
    MagicRodFireballHandler.installPhase1(registry);
    MagicPowderSprinkleHandler.installPhase1(registry);
    MusicalNoteHandler.installPhase1(registry);
    BombHandler.installPhase1(registry);
    ArrowHandler.installPhase1(registry);
    LiftableRockHandler.installPhase1(registry);
    HeartContainerHandler.installPhase1(registry);
    MadamMeowmeowHandler.installPhase1(registry);
    RichardHandler.installPhase1(registry);
    BowWowHandler.installPhase1Part2(registry);
    SwordShieldPickupHandler.installPhase1(registry);
    SleepyToadstoolHandler.installPhase1(registry);
    OwlEventHandler.installPhase1(registry);
    Type0X35Handler.installPhase1(registry);
    CrowHandler.installPhase1(registry);
    GenieHandler.installPhase1(registry);
    GenieHandler.installPhase1Part2(registry);
    MoldormHandler.installPhase1Part2(registry);
    WarpHandler.installPhase1(registry);
    Type0X42Handler.installPhase1(registry);
    KidHandler.installPhase1(registry);
    Type0X35Handler.installPhase1Part2(registry);
    Type0X39Handler.installPhase1(registry);
    InstrumentOfTheSirensHandler.installPhase1(registry);
    InstrumentOfTheSirensHandler.installPhase1Part2(registry);
  }
}
