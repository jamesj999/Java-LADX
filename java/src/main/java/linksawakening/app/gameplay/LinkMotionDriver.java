package linksawakening.app.gameplay;

import linksawakening.entity.Link;

interface LinkMotionDriver {
    LinkMotionDriver DIRECT = new LinkMotionDriver() {
        public void capture(Link link) {
            link.captureRomFinalPosition();
        }

        public void update(Link link) {
            link.update();
        }
    };

    void capture(Link link);

    void update(Link link);
}
