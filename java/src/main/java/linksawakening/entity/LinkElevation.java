package linksawakening.entity;

/** Shared subpixel height and airborne flag used by ROM ground and pit motion. */
final class LinkElevation {

    private boolean airborne;

    boolean airborne() {
        return airborne;
    }

    boolean airborne(boolean value) {
        airborne = value;
        return airborne;
    }

    private int zSubPixels;

    int zSubPixels() {
        return zSubPixels;
    }

    int zSubPixels(int value) {
        zSubPixels = value;
        return zSubPixels;
    }

    private int zVelocity;

    int zVelocity() {
        return zVelocity;
    }

    int zVelocity(int value) {
        zVelocity = value;
        return zVelocity;
    }
}
