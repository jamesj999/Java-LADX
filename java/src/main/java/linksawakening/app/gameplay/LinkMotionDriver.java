package linksawakening.app.gameplay;

import linksawakening.entity.Link;

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
