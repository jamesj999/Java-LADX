package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

/** The intro handler clock shared by actor and water motion; independent of presentation frame count. */
final class IntroClock {

    // hFrameCounter is reset during the beach sequence and stalls during LCD-off loads.
    // frameCounter is the monotonic presentation clock used for captures.
    private int clock;

    int clock() {
        return clock;
    }

    int clock(int value) {
        clock = value;
        return value;
    }
}
