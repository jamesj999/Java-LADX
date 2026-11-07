package linksawakening.world;

/** Installs family callbacks at phase 5 dispatch sites. */
final class EntityPhase5Handlers {
  static void install(EntityHandlerRegistry registry) {
    RoosterHandler.installPhase5(registry);
    ColorShellTypeHandler.installPhase5(registry);
  }
}
