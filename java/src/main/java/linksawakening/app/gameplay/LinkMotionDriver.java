package linksawakening.app.gameplay;

import linksawakening.entity.Link;

/** Orders final-position capture and Link motion. A retained port also supports recording test drivers. */
interface LinkMotionDriver {
    LinkMotionDriver DIRECT = new LinkMotionDriver() {
        public void capture(Link link) {
            link.position().captureRomFinalPosition();
        }

        public void update(Link link) {
            link.motion().update();
        }
    };

    void capture(Link link);

    void update(Link link);
}
