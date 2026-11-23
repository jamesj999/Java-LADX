package linksawakening.audio.music;

import linksawakening.gameplay.PcmSoundOutput;

/** Output that consumes frame samples synchronously and never retains caller storage. */
public interface ReusableFramePcmOutput extends PcmSoundOutput {}
